package com.iterable.iterableapi

import org.json.JSONObject
import java.io.File

public class IterableEmbeddedManager {
    fun getMessages(): List<IterableEmbeddedMessage> {
        val file = File("data.json")
        val bufferedReader = file.bufferedReader()
        val jsonString = bufferedReader.use { it.readText() }
        val messageJson = JSONObject(jsonString)

        val embeddedMessages = listOf(
            IterableEmbeddedMessage.fromJSONObject(messageJson),
            IterableEmbeddedMessage.fromJSONObject(messageJson),
            IterableEmbeddedMessage.fromJSONObject(messageJson)
        )

        return embeddedMessages
    }
}