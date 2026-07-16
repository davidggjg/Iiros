package com.iiros.scanner.core.url

import com.iiros.scanner.core.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UrlThreatScannerTest {

    @Test
    fun `clean https url on a real domain is safe`() {
        val result = UrlThreatScanner.scan("https://www.wikipedia.org/wiki/Android")
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun `raw ip host is flagged`() {
        val result = UrlThreatScanner.scan("http://192.168.1.50/login")
        assertTrue(result.findings.any { it.id == "url.raw_ip_host" })
    }

    @Test
    fun `punycode homograph domain is flagged`() {
        val result = UrlThreatScanner.scan("https://xn--pypal-4ve.com/login")
        assertTrue(result.findings.any { it.id == "url.punycode_host" })
    }

    @Test
    fun `userinfo at-sign phishing trick is flagged high severity`() {
        val result = UrlThreatScanner.scan("http://paypal.com@evil-domain.tk/reset")
        val finding = result.findings.first { it.id == "url.userinfo_trick" }
        assertEquals(RiskLevel.HIGH, finding.severity)
    }

    @Test
    fun `brand mentioned on wrong domain is flagged as impersonation`() {
        val result = UrlThreatScanner.scan("http://paypal-secure-login.tk/verify-account")
        assertTrue(result.findings.any { it.id == "url.brand_impersonation" })
        assertTrue(result.findings.any { it.id == "url.phishing_keyword" })
        assertTrue(result.findings.any { it.id == "url.abused_tld" })
        assertTrue(result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.CRITICAL)
    }

    @Test
    fun `url shortener is flagged low severity`() {
        val result = UrlThreatScanner.scan("https://bit.ly/3xyzabc")
        assertTrue(result.findings.any { it.id == "url.shortener" })
    }

    @Test
    fun `official brand domain is not flagged as impersonation`() {
        val result = UrlThreatScanner.scan("https://www.paypal.com/signin")
        assertTrue(result.findings.none { it.id == "url.brand_impersonation" })
    }

    @Test
    fun `deep subdomain chain is flagged`() {
        val result = UrlThreatScanner.scan("https://secure.login.account.verify.example-free-host.tk")
        assertTrue(result.findings.any { it.id == "url.deep_subdomains" })
    }

    @Test
    fun `blank input yields a safe empty result without crashing`() {
        val result = UrlThreatScanner.scan("   ")
        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.findings.isEmpty())
    }

    @Test
    fun `co-il second level domain is treated as one registrable domain`() {
        val result = UrlThreatScanner.scan("https://www.example.co.il/page")
        assertTrue(result.findings.none { it.id == "url.deep_subdomains" })
    }
}
