package com.iterable.integration.tests.utils

/**
 * Masks an email for logging / on-screen display so the BCIT test user isn't exposed
 * in public CI artifacts (logcat, screenshots). Keeps the first character and the
 * domain for debuggability: "franco.zalamena@iterable.com" -> "f***@iterable.com".
 */
fun maskEmail(email: String?): String {
    if (email.isNullOrBlank()) return "<none>"
    val at = email.indexOf('@')
    if (at <= 0) return "***"
    return "${email[0]}***${email.substring(at)}"
}
