package com.iiros.scanner.core.url

/** Brand keyword -> the registrable domain(s) that are actually theirs. */
object KnownBrandDomains {
    val OFFICIAL_DOMAINS: Map<String, Set<String>> = mapOf(
        "paypal" to setOf("paypal.com"),
        "google" to setOf("google.com", "accounts.google.com"),
        "microsoft" to setOf("microsoft.com", "live.com", "office.com"),
        "apple" to setOf("apple.com", "icloud.com"),
        "amazon" to setOf("amazon.com"),
        "netflix" to setOf("netflix.com"),
        "facebook" to setOf("facebook.com", "fb.com"),
        "instagram" to setOf("instagram.com"),
        "whatsapp" to setOf("whatsapp.com"),
        "binance" to setOf("binance.com"),
        "coinbase" to setOf("coinbase.com"),
        "chase" to setOf("chase.com"),
        "bankofamerica" to setOf("bankofamerica.com"),
        "wellsfargo" to setOf("wellsfargo.com"),
        "steam" to setOf("steampowered.com", "steamcommunity.com"),
        "discord" to setOf("discord.com", "discordapp.com"),
    )

    val URL_SHORTENERS: Set<String> = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly",
        "rebrand.ly", "cutt.ly", "shorte.st", "adf.ly", "bl.ink",
    )

    /**
     * TLDs that are disproportionately used for throwaway/malicious domains
     * because they are free or extremely cheap to register. This is a
     * heuristic signal only — plenty of legitimate sites use them too, so it
     * is weighted low and never used as a sole verdict.
     */
    val ABUSED_TLDS: Set<String> = setOf(
        "zip", "mov", "tk", "ml", "ga", "cf", "gq", "top", "xyz", "work",
        "click", "link", "country", "stream", "gdn", "review", "kim", "men",
        "loan", "win", "bid", "party", "science", "racing", "accountant",
    )

    val PHISHING_PATH_KEYWORDS: Set<String> = setOf(
        "verify-account", "confirm-account", "update-billing", "secure-login",
        "signin-verify", "account-locked", "wp-login", "reset-password-now",
    )
}
