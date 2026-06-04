package com.marinmiruna.vaultly.viewmodel

import android.app.Application
import com.marinmiruna.vaultly.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marinmiruna.vaultly.data.repository.PasswordRepository
import com.marinmiruna.vaultly.data.security.SecureClipboardManager
import com.marinmiruna.vaultly.domain.model.PasswordEntry
import com.marinmiruna.vaultly.domain.model.PasswordListItem
import com.marinmiruna.vaultly.session.SensitiveSessionManager
import com.marinmiruna.vaultly.domain.security.PasswordSecurityAnalyzer
import com.marinmiruna.vaultly.domain.security.PasswordSecurityReport
import com.marinmiruna.vaultly.domain.security.PasswordSecurityInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class PasswordsViewModel @Inject constructor(
    private val passwordRepository: PasswordRepository,
    private val sensitiveSessionManager: SensitiveSessionManager,
    private val secureClipboardManager: SecureClipboardManager,
    private val application: Application
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedPasswordId = MutableStateFlow(0L)
    private var loadPasswordJob: Job? = null
    private var clearClipboardJob: Job? = null
    private val passwordSecurityReports: StateFlow<Map<Long, PasswordSecurityReport>> =
        passwordRepository
            .observePasswordSecurityReports()
            .catch {
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyMap()
            )

    private val passwordSecurityInputs: StateFlow<List<PasswordSecurityInput>> =
        passwordRepository
            .observePasswordSecurityInputs()
            .catch {
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val authState: StateFlow<PasswordsAuthState> = MutableStateFlow(
        PasswordsAuthState(
            isAuthenticated = sensitiveSessionManager.isPasswordsSessionValid()
        )
    )

    val uiState: StateFlow<PasswordsUiState> = searchQuery
        .flatMapLatest { query ->
            passwordRepository.observePasswords(query)
        }
        .catch { exception ->
            _detailState.value = _detailState.value.copy(
                errorMessage = UserMessageMapper.passwordOperationMessage(
                    context = application,
                    exception = exception
                )
            )
            emit(emptyList())
        }
        .combine(searchQuery) { passwords, query ->
            passwords to query
        }
        .combine(passwordSecurityReports) { (passwords, query), securityReports ->
            PasswordsUiState(
                passwords = passwords,
                searchQuery = query,
                securityReports = securityReports
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PasswordsUiState()
        )

    private val _detailState = MutableStateFlow(PasswordDetailUiState())
    val detailState: StateFlow<PasswordDetailUiState> = _detailState

    fun markAuthenticated() {
        sensitiveSessionManager.markPasswordsAuthenticated()
        (authState as MutableStateFlow).value = PasswordsAuthState(isAuthenticated = true)
    }

    fun refreshAuthState() {
        (authState as MutableStateFlow).value = PasswordsAuthState(
            isAuthenticated = sensitiveSessionManager.isPasswordsSessionValid()
        )
    }

    fun isPasswordSessionValid(): Boolean {
        return sensitiveSessionManager.isPasswordsSessionValid()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun loadPassword(passwordId: Long) {
        if (selectedPasswordId.value == passwordId) {
            return
        }

        selectedPasswordId.value = passwordId
        loadPasswordJob?.cancel()

        if (passwordId == 0L) {
            _detailState.value = PasswordDetailUiState()
            return
        }

        loadPasswordJob = viewModelScope.launch {
            passwordRepository.observePassword(passwordId)
                .catch { exception ->
                _detailState.value = _detailState.value.copy(
                    errorMessage = UserMessageMapper.passwordOperationMessage(
                        context = application,
                        exception = exception
                    )
                )
            }
                .collect { password ->
                    if (password != null) {
                        val report = passwordSecurityReports.value[password.id]

                        _detailState.value = PasswordDetailUiState(
                            id = password.id,
                            service = password.service,
                            username = password.username,
                            password = password.password,
                            url = password.url,
                            note = password.note,
                            canDelete = true,
                            securityReport = report
                        )
                    }
            }
        }
    }

    fun onServiceChange(service: String) {
        _detailState.value = _detailState.value.copy(service = service)
    }

    fun onUsernameChange(username: String) {
        _detailState.value = _detailState.value.copy(username = username)
    }

    fun onPasswordChange(password: String) {
        val state = _detailState.value
        val isReused = isPasswordReused(
            password = password,
            currentPasswordId = state.id
        )

        _detailState.value = state.copy(
            password = password,
            securityReport = PasswordSecurityReport(
                passwordId = state.id,
                issues = PasswordSecurityAnalyzer.analyzePassword(
                    password = password,
                    isReused = isReused
                )
            )
        )
    }

    fun onUrlChange(url: String) {
        _detailState.value = _detailState.value.copy(url = url)
    }

    fun onNoteChange(note: String) {
        _detailState.value = _detailState.value.copy(note = note)
    }

    fun onGeneratorLengthChange(length: Int) {
        _detailState.value = _detailState.value.copy(generatorLength = length.coerceIn(8, 32))
    }

    fun onGeneratorUppercaseChange(include: Boolean) {
        _detailState.value = _detailState.value.copy(generatorUppercase = include)
    }

    fun onGeneratorDigitsChange(include: Boolean) {
        _detailState.value = _detailState.value.copy(generatorDigits = include)
    }

    fun onGeneratorSymbolsChange(include: Boolean) {
        _detailState.value = _detailState.value.copy(generatorSymbols = include)
    }

    fun generatePassword() {
        val state = _detailState.value
        val generated = passwordRepository.generatePassword(
            length = state.generatorLength,
            includeUppercase = state.generatorUppercase,
            includeDigits = state.generatorDigits,
            includeSymbols = state.generatorSymbols
        )

        _detailState.value = state.copy(generatedPassword = generated)
    }

    fun useGeneratedPassword() {
        val state = _detailState.value
        if (state.generatedPassword.isNotBlank()) {
            onPasswordChange(state.generatedPassword)
        }
    }

    fun savePassword(onSaved: () -> Unit) {
        val state = _detailState.value

        if (state.service.isBlank() || state.username.isBlank() || state.password.isBlank()) {
            _detailState.value = state.copy(
                errorMessage = application.getString(R.string.password_validation_required_fields)
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                passwordRepository.savePassword(
                    id = state.id,
                    service = state.service,
                    username = state.username,
                    password = state.password,
                    url = state.url,
                    note = state.note
                )
            }.onSuccess {
                onSaved()
            }.onFailure { exception ->
                _detailState.value = _detailState.value.copy(
                    errorMessage = UserMessageMapper.passwordOperationMessage(
                        context = application,
                        exception = exception
                    )
                )
            }
        }
    }

    fun deleteCurrentPassword(onDeleted: () -> Unit) {
        val state = _detailState.value
        if (state.id == 0L) {
            onDeleted()
            return
        }

        viewModelScope.launch {
            runCatching {
                passwordRepository.deletePassword(
                    PasswordEntry(
                        id = state.id,
                        service = state.service,
                        username = state.username,
                        password = state.password,
                        url = state.url,
                        note = state.note,
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )
            }.onSuccess {
                onDeleted()
            }.onFailure { exception ->
                _detailState.value = _detailState.value.copy(
                    errorMessage = UserMessageMapper.passwordOperationMessage(
                        context = application,
                        exception = exception
                    )
                )
            }
        }
    }

    fun copyPasswordToClipboard(password: String) {
        if (password.isBlank()) {
            return
        }

        secureClipboardManager.copySensitiveText(
            label = application.getString(R.string.password_clipboard_label),
            value = password
        )

        clearClipboardJob?.cancel()
        clearClipboardJob = viewModelScope.launch {
            delay(CLIPBOARD_CLEAR_DELAY_MILLIS)
            secureClipboardManager.clear()
        }
    }

    private fun isPasswordReused(
        password: String,
        currentPasswordId: Long
    ): Boolean {
        if (password.isBlank()) {
            return false
        }

        return passwordSecurityInputs.value.any { input ->
            input.id != currentPasswordId && input.password == password
        }
    }

    override fun onCleared() {
        clearClipboardJob?.cancel()
        secureClipboardManager.clear()
        super.onCleared()
    }

    companion object {
        private const val CLIPBOARD_CLEAR_DELAY_MILLIS = 30_000L
    }
}

data class PasswordsAuthState(
    val isAuthenticated: Boolean = false
)

data class PasswordsUiState(
    val passwords: List<PasswordListItem> = emptyList(),
    val searchQuery: String = "",
    val securityReports: Map<Long, PasswordSecurityReport> = emptyMap()
)

data class PasswordDetailUiState(
    val id: Long = 0,
    val service: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val note: String = "",
    val canDelete: Boolean = false,
    val errorMessage: String? = null,
    val securityReport: PasswordSecurityReport? = null,
    val generatedPassword: String = "",
    val generatorLength: Int = 16,
    val generatorUppercase: Boolean = true,
    val generatorDigits: Boolean = true,
    val generatorSymbols: Boolean = true
)
