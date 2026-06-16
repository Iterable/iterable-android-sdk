package com.iterable.integration.tests.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Persisted local override of the test user email. When set, takes precedence over
 * the BuildConfig default and the CI dated email — so a developer can validate
 * tests against any user (e.g. their own personal Iterable account) without
 * editing local.properties or rebuilding.
 *
 * Stored in app-private SharedPreferences. Never read in CI by the workflow path,
 * but BaseIntegrationTest will still honour it if a developer left an override
 * set on a local emulator.
 */
object TestUserEmailOverride {

    private const val PREFS_NAME = "iterable_integration_tests"
    private const val KEY_EMAIL = "test_user_email_override"

    fun get(context: Context): String? =
        prefs(context).getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun set(context: Context, email: String) {
        prefs(context).edit().putString(KEY_EMAIL, email).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_EMAIL).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
