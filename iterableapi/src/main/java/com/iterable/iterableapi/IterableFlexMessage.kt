package com.iterable.iterableapi

data class IterableFlexMessage (
    var metadata: IterableFlexMessageMetaData,
    var elements: IterableFlexMessageElements,
    var custom: IterableFlexMessageCustomPayload)

