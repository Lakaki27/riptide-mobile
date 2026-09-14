package com.evertide.riptide.util

import com.evertide.riptide.data.model.ErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException

fun Throwable.toUserMessage(json: Json, fallback: String = "Something went wrong"): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        val parsed = body?.let {
            runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull()
        }
        return parsed ?: "Request failed with status ${code()}"
    }
    return message ?: fallback
}
