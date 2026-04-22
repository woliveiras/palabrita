package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.ChatMessageDao
import com.woliveiras.palabrita.core.data.db.dao.PuzzleDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl
@Inject
constructor(private val chatDao: ChatMessageDao, private val puzzleDao: PuzzleDao) :
  ChatRepository {

  override suspend fun getMessages(puzzleId: Long): List<ChatMessage> =
    chatDao.getByPuzzleId(puzzleId).map { it.toDomain() }

  override suspend fun saveMessage(message: ChatMessage) = chatDao.insert(message.toEntity())

  override suspend fun countUserMessages(puzzleId: Long): Int = chatDao.countUserMessages(puzzleId)

  override suspend fun getPuzzle(puzzleId: Long): Puzzle? = puzzleDao.getById(puzzleId)?.toDomain()

  override suspend fun deleteAll() = chatDao.deleteAll()
}
