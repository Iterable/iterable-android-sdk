package com.iterable.iterableapi

enum class IterableDataRegion(private val endpoint: String) {
    US("https://api.iterable.com/api/"),
    EU("https://api.eu.iterable.com/api/");

    fun getEndpoint(): String {
        return this.endpoint
    }
}
