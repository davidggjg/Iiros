package com.iiros.scanner.core.url

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel
import java.net.IDN
import java.net.URI

/**
 * Offline heuristic scanner for a single pasted URL. It does **not** make any
 * network call (no reputation lookup, no WHOIS, no Safe Browsing API) — it
 * only inspects the literal string the user pasted. That makes it fast and
 * privacy-preserving, at the cost of being unable to catch a freshly-hosted
 * phishing page on an otherwise clean-looking domain. The app layer is free
 * to add an online reputation check (e.g. Google Safe Browsing) on top of
 * this and merge the findings.
 */
object UrlThreatScanner {

    private val IPV4_REGEX = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$")

    /** Second-level suffixes where the registrable domain is 3 labels, not 2. */
    private val TWO_LABEL_PUBLIC_SUFFIXES = setOf(
        "co.il", "org.il", "net.il", "ac.il", "gov.il", "muni.il",
        "co.uk", "org.uk", "gov.uk", "ac.uk",
        "com.au", "co.jp", "co.kr", "com.br",
    )

    fun scan(rawInput: String): UrlScanResult {
        val input = rawInput.trim()
        val findings = mutableListOf<Finding>()

        if (input.isEmpty()) {
            return UrlScanResult(input, null, null, 0, RiskLevel.SAFE, emptyList())
        }

        val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://").containsMatchIn(input)
        val candidate = if (hasScheme) input else "http://$input"

        val uri = try {
            URI(candidate)
        } catch (e: Exception) {
            findings += Finding(
                id = "url.unparseable",
                title = "Could not be parsed as a valid URL",
                detail = e.message ?: "malformed URL",
                severity = RiskLevel.LOW,
                weight = 10,
            )
            return finish(input, null, null, findings)
        }

        val rawHost = uri.host
        if (rawHost.isNullOrBlank()) {
            findings += Finding(
                id = "url.no_host",
                title = "URL has no resolvable host",
                detail = input,
                severity = RiskLevel.LOW,
                weight = 10,
            )
            return finish(input, uri.toString(), null, findings)
        }

        if (!hasScheme) {
            // Not inherently dangerous, just noted for context in the UI.
        } else if (uri.scheme.equals("http", ignoreCase = true)) {
            findings += Finding(
                id = "url.insecure_scheme",
                title = "Uses unencrypted HTTP instead of HTTPS",
                detail = "scheme=${uri.scheme}",
                severity = RiskLevel.LOW,
                weight = 6,
            )
        }

        if (uri.userInfo != null) {
            findings += Finding(
                id = "url.userinfo_trick",
                title = "Hides the real destination behind an \"@\" (userinfo phishing trick)",
                detail = "userinfo=\"${uri.userInfo}\" host=$rawHost",
                severity = RiskLevel.HIGH,
                weight = 35,
            )
        }

        val hostAscii = try {
            IDN.toASCII(rawHost)
        } catch (e: Exception) {
            rawHost
        }
        val host = hostAscii.lowercase()

        if (IPV4_REGEX.matches(host) || (host.contains(":") && host.count { it == ':' } >= 2)) {
            findings += Finding(
                id = "url.raw_ip_host",
                title = "Uses a raw IP address instead of a domain name",
                detail = host,
                severity = RiskLevel.MEDIUM,
                weight = 20,
            )
        }

        if (host.contains("xn--")) {
            findings += Finding(
                id = "url.punycode_host",
                title = "Domain uses punycode encoding (possible look-alike/homograph domain)",
                detail = host,
                severity = RiskLevel.MEDIUM,
                weight = 22,
            )
        } else if (rawHost.any { it.code > 127 }) {
            val scripts = rawHost.filter { it.code > 127 }.map { Character.UnicodeScript.of(it.code) }.toSet()
            findings += Finding(
                id = "url.non_ascii_host",
                title = "Domain contains non-Latin characters (possible homograph attack)",
                detail = "host=$rawHost scripts=${scripts.joinToString()}",
                severity = RiskLevel.MEDIUM,
                weight = 22,
            )
        }

        if (host in KnownBrandDomains.URL_SHORTENERS) {
            findings += Finding(
                id = "url.shortener",
                title = "Uses a URL shortener (destination is hidden)",
                detail = host,
                severity = RiskLevel.LOW,
                weight = 8,
            )
        }

        val labels = host.removeSuffix(".").split(".")
        val registrable = registrableDomain(labels)
        val subdomainDepth = (labels.size - registrable.split(".").size).coerceAtLeast(0)
        if (subdomainDepth >= 3) {
            findings += Finding(
                id = "url.deep_subdomains",
                title = "Unusually many subdomain levels (often used to bury the real domain)",
                detail = host,
                severity = RiskLevel.LOW,
                weight = 10,
            )
        }

        val tld = labels.lastOrNull().orEmpty()
        if (tld in KnownBrandDomains.ABUSED_TLDS) {
            findings += Finding(
                id = "url.abused_tld",
                title = "Top-level domain \".$tld\" is disproportionately used for throwaway/malicious sites",
                detail = "This is a weak heuristic signal on its own.",
                severity = RiskLevel.LOW,
                weight = 8,
            )
        }

        for ((brand, officialDomains) in KnownBrandDomains.OFFICIAL_DOMAINS) {
            val mentionsBrand = host.contains(brand) || uri.path.orEmpty().lowercase().contains(brand)
            val isOfficial = officialDomains.any { registrable == it }
            if (mentionsBrand && !isOfficial) {
                findings += Finding(
                    id = "url.brand_impersonation",
                    title = "Mentions \"$brand\" but is not hosted on ${officialDomains.first()}",
                    detail = "host=$host",
                    severity = RiskLevel.HIGH,
                    weight = 40,
                )
                break
            }
        }

        val pathAndQuery = (uri.path.orEmpty() + "?" + uri.query.orEmpty()).lowercase()
        val phishingKeywordHit = KnownBrandDomains.PHISHING_PATH_KEYWORDS.firstOrNull { pathAndQuery.contains(it) }
        if (phishingKeywordHit != null) {
            findings += Finding(
                id = "url.phishing_keyword",
                title = "URL path contains a common phishing-kit keyword",
                detail = phishingKeywordHit,
                severity = RiskLevel.MEDIUM,
                weight = 15,
            )
        }

        if (uri.port !in intArrayOf(-1, 80, 443)) {
            findings += Finding(
                id = "url.nonstandard_port",
                title = "Uses a non-standard port",
                detail = "port=${uri.port}",
                severity = RiskLevel.LOW,
                weight = 6,
            )
        }

        if (input.length > 150) {
            findings += Finding(
                id = "url.excessive_length",
                title = "Unusually long URL (possible obfuscation)",
                detail = "${input.length} characters",
                severity = RiskLevel.LOW,
                weight = 6,
            )
        }

        return finish(input, uri.toString(), host, findings)
    }

    private fun registrableDomain(labels: List<String>): String {
        if (labels.size <= 2) return labels.joinToString(".")
        val lastTwo = labels.takeLast(2).joinToString(".")
        return if (lastTwo in TWO_LABEL_PUBLIC_SUFFIXES && labels.size >= 3) {
            labels.takeLast(3).joinToString(".")
        } else {
            lastTwo
        }
    }

    private fun finish(input: String, normalized: String?, host: String?, findings: List<Finding>): UrlScanResult {
        val score = findings.sumOf { it.weight }.coerceAtMost(100)
        return UrlScanResult(
            input = input,
            normalizedUrl = normalized,
            host = host,
            score = score,
            riskLevel = RiskLevel.fromScore(score),
            findings = findings.sortedByDescending { it.weight },
        )
    }
}
