package com.iterable.iterableapi

import java.util.concurrent.Executor

internal class IterableDeeplinkRedirectTask(
    private val url: String,
    private val callback: IterableHelper.IterableActionHandler?,
    private val redirectResolver: IterableDeeplinkRedirectResolver,
    private val callbackExecutor: Executor
) : Runnable {

    override fun run() {
        val result = redirectResolver.resolve(url)
        callbackExecutor.execute { deliver(result) }
    }

    private fun deliver(result: IterableDeeplinkRedirectResult) {
        callback?.execute(result.url)

        if (result.campaignId != 0) {
            val attributionInfo = IterableAttributionInfo(
                result.campaignId,
                result.templateId,
                result.messageId
            )
            IterableApi.sharedInstance.setAttributionInfo(attributionInfo)
        }
    }
}
