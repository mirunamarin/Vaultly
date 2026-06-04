package com.marinmiruna.vaultly.viewmodel

import android.app.Application
import com.marinmiruna.vaultly.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marinmiruna.vaultly.data.repository.NotesRepository
import com.marinmiruna.vaultly.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    private val application: Application
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedNoteId = MutableStateFlow(0L)
    private var loadNoteJob: Job? = null

    val uiState: StateFlow<NotesUiState> = searchQuery
        .flatMapLatest { query ->
            notesRepository.observeNotes(query)
        }
        .catch { exception ->
            _detailState.value = _detailState.value.copy(
                errorMessage = UserMessageMapper.noteOperationMessage(
                    context = application,
                    exception = exception
                )
            )
            emit(emptyList())
        }
        .combine(searchQuery) { notes, query ->
            NotesUiState(
                notes = notes,
                searchQuery = query
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotesUiState()
        )

    private val _detailState = MutableStateFlow(NoteDetailUiState())
    val detailState: StateFlow<NoteDetailUiState> = _detailState

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun loadNote(noteId: Long) {
        if (selectedNoteId.value == noteId) {
            return
        }

        selectedNoteId.value = noteId
        loadNoteJob?.cancel()

        if (noteId == 0L) {
            _detailState.value = NoteDetailUiState()
            return
        }

        loadNoteJob = viewModelScope.launch {
            notesRepository.observeNote(noteId)
                .catch { exception ->
                    _detailState.value = _detailState.value.copy(
                        errorMessage = UserMessageMapper.noteOperationMessage(
                            context = application,
                            exception = exception
                        )
                    )
                }
                .collect { note ->
                if (note != null) {
                    _detailState.value = NoteDetailUiState(
                        id = note.id,
                        title = note.title,
                        content = note.content,
                        canDelete = true
                    )
                }
            }
        }
    }

    fun onTitleChange(title: String) {
        _detailState.value = _detailState.value.copy(title = title)
    }

    fun onContentChange(content: String) {
        _detailState.value = _detailState.value.copy(content = content)
    }

    fun saveNote(onSaved: () -> Unit) {
        val state = _detailState.value
        if (state.title.isBlank() && state.content.isBlank()) {
            _detailState.value = state.copy(
                errorMessage = application.getString(R.string.note_validation_empty)
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                notesRepository.saveNote(
                    id = state.id,
                    title = state.title.ifBlank {
                        application.getString(R.string.note_default_title)
                    },
                    content = state.content
                )
            }.onSuccess {
                onSaved()
            }.onFailure { exception ->
                _detailState.value = _detailState.value.copy(
                    errorMessage = UserMessageMapper.noteOperationMessage(
                        context = application,
                        exception = exception
                    )
                )
            }
        }
    }

    fun deleteCurrentNote(onDeleted: () -> Unit) {
        val state = _detailState.value
        if (state.id == 0L) {
            onDeleted()
            return
        }

        viewModelScope.launch {
            runCatching {
                notesRepository.deleteNote(
                    Note(
                        id = state.id,
                        title = state.title,
                        content = state.content,
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            }.onSuccess {
                onDeleted()
            }.onFailure { exception ->
                _detailState.value = _detailState.value.copy(
                    errorMessage = UserMessageMapper.noteOperationMessage(
                        context = application,
                        exception = exception
                    )
                )
            }
        }
    }
}

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = ""
)

data class NoteDetailUiState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val canDelete: Boolean = false,
    val errorMessage: String? = null
)
