package com.woliveiras.palabrita.core.model

data class ChatMessage(
    val id: Long = 0,
    val puzzleId: Long,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
)

enum class MessageRole {
    USER,
    MODEL,
}
