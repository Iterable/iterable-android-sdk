package com.iterable.iterableapi

data class IterableFlexMessage(var metadata: IterableFlexMessageMetaData, var elements: IterableFlexMessageElements, var custom: Any)

data class IterableFlexMessageMetaData(var id: String, var placementId: String, var campaignId: String, var isProof: Boolean)

data class IterableFlexMessageElements(var type: String, var buttons: List<IterableFlexMessageButton>, var images: List<IterableFlexMessageImage>, var text: List<IterableFlexMessageText>)

data class IterableFlexMessageButton(var id: String, var title: String, var action: String)

data class IterableFlexMessageImage(var id: String, var url: String)

data class IterableFlexMessageText(var title: String, var text: String)