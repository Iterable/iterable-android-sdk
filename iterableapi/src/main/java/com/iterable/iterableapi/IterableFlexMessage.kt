package com.iterable.iterableapi
data class IterableFlexMessage (
    val metadata: FlexMessageMetadata,
    val elements: FlexMessageElements? = null,
    val payload: HashMap<Any, Any>? = null
)
data class FlexMessageMetadata (
    val id: String,
    val placementID: String,
    val campaignID: String? = null,
    val isProof: Boolean? = null
)
data class FlexMessageElements (
    val title: String? = null,
    val body: String? = null,
    val mediaURL: String? = null,
    val defaultAction: FlexMessageElementsDefaultAction? = null,
    val buttons: List<FlexMessageElementsButton>? = null,
    val text: List<FlexMessageElementsText>? = null
)
data class FlexMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
)
data class FlexMessageElementsDefaultAction (
    val type: String,
    val data: String
)
data class FlexMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
)