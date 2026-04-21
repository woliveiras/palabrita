package com.woliveiras.palabrita.core.data.mapper

import com.woliveiras.palabrita.core.data.db.entity.ChatMessageEntity
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.MessageRole

fun ChatMessageEntity.toDomain(): ChatMessage =
  ChatMessage(
    id = id,
    puzzleId = puzzleId,
    role = MessageRole.valueOf(role.uppercase()),
    content = content,
    timestamp = timestamp,
  )

fun ChatMessage.toEntity(): ChatMessageEntity =
  ChatMessageEntity(
    id = id,
    puzzleId = puzzleId,
    role = role.name.lowercase(),
    content = content,
    timestamp = timestamp,
  )
