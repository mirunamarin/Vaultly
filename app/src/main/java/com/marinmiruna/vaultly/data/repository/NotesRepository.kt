package com.marinmiruna.vaultly.data.repository

import com.marinmiruna.vaultly.data.db.dao.NoteDao
import com.marinmiruna.vaultly.data.db.entity.NoteEntity
import com.marinmiruna.vaultly.domain.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotesRepository @Inject constructor(
    private val noteDao: NoteDao
) {

    fun observeNotes(query: String): Flow<List<Note>> {
        return noteDao.observeNotes(query).map { notes ->
            notes.map { entity -> entity.toDomain() }
        }
    }

    fun observeNote(id: Long): Flow<Note?> {
        return noteDao.observeNote(id).map { entity ->
            entity?.toDomain()
        }
    }

    suspend fun saveNote(
        id: Long,
        title: String,
        content: String
    ) {
        val now = System.currentTimeMillis()
        val existingNote = if (id == 0L) null else noteDao.getNoteById(id)

        noteDao.upsert(
            NoteEntity(
                id = id,
                title = title.trim(),
                content = content.trim(),
                createdAt = existingNote?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun deleteNote(note: Note) {
        noteDao.delete(note.toEntity())
    }

    private fun NoteEntity.toDomain(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Note.toEntity(): NoteEntity {
        return NoteEntity(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
