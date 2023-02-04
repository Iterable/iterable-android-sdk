package com.iterable.iterableapi

class IterableFlexMessage(
    var metadata: FlexMessageMetaData,
    var elements: FlexMessageElements? = null,
    var custom: Any? = null
) {
    class FlexMessageMetaData(
        var id: String,
        var placementId: String,
        var campaignId: String? = null,
        var isProof: Boolean? = false
    )

    class FlexMessageElements (
        var type: String? = null,
        var buttons: List<FlexMessageButton>? = null,
        var images: List<FlexMessageImage>? = null,
        var text: List<FlexMessageText>? = null
    ) {
        class FlexMessageButton(
            var id: String,
            var title: String? = null,
            var action: String? = null
        )

        class FlexMessageImage(
            var id: String,
            var url: String? = null
        )

        class FlexMessageText(
            var id: String,
            var text: String? = null
        )
    }
}