package com.iterable.iterableapi.ui.flex

data class IterableFlexMessage(var metadata: IterableFlexMessageMetaData, var elements: IterableFlexMessageElements, var custom: IterableCustomPayload)

data class IterableFlexMessageMetaData(var id: String = "", var placementId: String = "", var campaignId: String = "", var isProof: Boolean)

data class IterableFlexMessageElements(var type: String = "", var buttons: FlexMessageButtons, var images: FlexMessageImage, var text: FlexMessageText)

data class FlexMessageButtons(var buttonId: String = "", var button: FlexMessageButtonData)

data class FlexMessageButtonData(var title: String = "", var action: String = "")

data class FlexMessageImage(var imageId: String = "", var imageUrl: String = "")

data class FlexMessageText(var title: String = "", var body: String = "")

data class IterableCustomPayload(var key: String = "", var value: String = "")