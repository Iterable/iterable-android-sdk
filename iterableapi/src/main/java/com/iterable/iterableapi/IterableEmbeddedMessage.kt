package com.iterable.iterableapi

import org.json.JSONObject

class IterableEmbeddedMessage (
    val metadata: EmbeddedMessageMetadata,
    val elements: EmbeddedMessageElements? = null,
    val payload: JSONObject? = null
)

class EmbeddedMessageMetadata(
    var id: String,
    val placementId: String,
    val campaignId: String? = null,
    val isProof: Boolean? = null
)

class EmbeddedMessageElements (
    val title: String? = null,
    val body: String? = null,
    val mediaURL: String? = null,
    val defaultAction: EmbeddedMessageElementsDefaultAction? = null,
    val buttons: List<EmbeddedMessageElementsButton>? = null,
    val text: List<FlexMessageElementsText>? = null
)

class EmbeddedMessageElementsButton (
    val id: String,
    val title: String? = null,
    val action: String? = null
)

class EmbeddedMessageElementsDefaultAction (
    val type: String,
    val data: String
)

class FlexMessageElementsText (
    val id: String,
    val text: String? = null,
    val label: String? = null
)