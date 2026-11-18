package com.iterable.integration.tests.utils

import android.content.Context
import android.util.Log
import com.iterable.integration.tests.BcitHandlers
import com.iterable.integration.tests.BuildConfig
import com.iterable.iterableapi.IterableConfig
import com.iterable.iterableapi.IterableDataRegion
import com.iterable.iterableapi.IterableProject

/**
 * One Iterable project the BCIT app can switch to. [serverApiKey] is empty when the harness has no
 * server-side key for the project, as for C.
 */
data class BcitProject(
    val label: String,
    val apiKey: String,
    val serverApiKey: String,
    val dataRegion: IterableDataRegion
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    /** Server-side API base, ending in `/api/`, in the project's region. */
    val serverBaseUrl: String get() = dataRegion.endpoint
}

/**
 * The projects used by the switchProject bug bash (SDK-760). A is the existing BCIT project, B a
 * second project, C an extra Mobile key whose project is not assumed, and D a second Mobile key on B.
 * Labels are resolved from the API key, so they match the iOS harness.
 */
object BcitProjects {

    const val UNKNOWN_LABEL = "?"

    private val regionB: IterableDataRegion =
        BuildConfig.ITERABLE_DATA_REGION_B.takeIf { it.isNotBlank() }?.let { IterableDataRegion.from(it) }
            ?: IterableDataRegion.US

    val A = BcitProject("A", BuildConfig.ITERABLE_API_KEY, BuildConfig.ITERABLE_SERVER_API_KEY, IterableDataRegion.US)
    val B = BcitProject("B", BuildConfig.ITERABLE_API_KEY_B, BuildConfig.ITERABLE_SERVER_API_KEY_B, regionB)
    val C = BcitProject("C", BuildConfig.ITERABLE_API_KEY_C, "", IterableDataRegion.US)
    val D = BcitProject("D", BuildConfig.ITERABLE_API_KEY_D, BuildConfig.ITERABLE_SERVER_API_KEY_B, regionB)

    val all: List<BcitProject> = listOf(A, B, C, D)

    val projectIdB: String get() = BuildConfig.ITERABLE_PROJECT_ID_B

    val isBConfigured: Boolean get() = B.isConfigured && B.serverApiKey.isNotBlank()

    fun labelFor(apiKey: String?, projects: List<BcitProject> = all): String =
        projects.firstOrNull { it.isConfigured && it.apiKey == apiKey }?.label ?: UNKNOWN_LABEL

    /** B and D share project B's user; A and C use project A's, matching the iOS harness. */
    fun testUserEmailFor(project: BcitProject, emailA: String): String =
        if (project == B || project == D) TestUserEmailGenerator.forProjectB(emailA) else emailA

    /** MainActivity's project A config, in [project]'s region. */
    fun defaultConfig(context: Context, project: BcitProject): IterableConfig = IterableConfig.Builder()
        .setAutoPushRegistration(true)
        .setEnableEmbeddedMessaging(true)
        .setInAppDisplayInterval(BcitHandlers.IN_APP_DISPLAY_INTERVAL_SECONDS)
        .setAllowedProtocols(BcitHandlers.ALLOWED_PROTOCOLS)
        .setUrlHandler(BcitHandlers.urlHandler(context))
        .setCustomActionHandler(BcitHandlers.customActionHandler)
        .setLogLevel(Log.VERBOSE)
        .setDataRegion(project.dataRegion)
        .build()

    fun iterableProject(
        context: Context,
        project: BcitProject,
        config: IterableConfig = defaultConfig(context, project)
    ): IterableProject = IterableProject(project.apiKey, config)
}
