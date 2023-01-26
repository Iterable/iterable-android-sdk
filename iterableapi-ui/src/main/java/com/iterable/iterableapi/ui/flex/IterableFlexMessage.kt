package com.iterable.iterableapi.ui.flex

data class IterableFlexMessage(var metadata: IterableFlexMessageMetaData, var elements: IterableFlexMessageElements, var custom: IterableCustomPayload)

data class IterableFlexMessageMetaData(var id: String, var placementId: String, var campaignId: String, var isProof: Boolean)

data class IterableFlexMessageElements(var type: String, var buttons: List<FlexMessageButton>, var images: List<FlexMessageImage>, var text: List<FlexMessageText>)

data class FlexMessageButton(var id: String, var title: String, var action: String)

data class FlexMessageImage(var id: String, var url: String)

data class FlexMessageText(var title: String, var text: String)

data class IterableCustomPayload(var key: Any)