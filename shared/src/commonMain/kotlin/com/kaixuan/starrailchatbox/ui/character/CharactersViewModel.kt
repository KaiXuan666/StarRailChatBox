package com.kaixuan.starrailchatbox.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.sharing.DefaultPublicCharacterRepository
import com.kaixuan.starrailchatbox.data.character.sharing.PublicCharacterRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class CharactersViewModel(
    private val characterRepository: CharacterRepository,
    private val characterCardExporter: CharacterCardExporter,
    private val publicCharacterRepository: PublicCharacterRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeCharacters()
    }

    fun onAction(action: CharacterAction) {
        when (action) {
            is CharacterAction.CharacterSelected -> {
                _uiState.update { it.copy(selectedCharacterId = action.characterId) }
            }
            is CharacterAction.CharactersReordered -> reorder(action.orderedCharacters.map { it.id })
            is CharacterAction.CharacterDeleteClicked -> delete(action.characterId)
            CharacterAction.CharacterDeleteBuiltinClicked -> {
                _effects.trySend(
                    CharacterEffect.ShowMessage(
                        CharacterEffectMessage.CHARACTER_DELETE_BUILTIN_RESTRICTED,
                    ),
                )
            }
            is CharacterAction.CharacterExportClicked -> {
                _uiState.update { it.copy(exportDialogCharacterId = action.characterId) }
            }
            CharacterAction.CharacterExportDialogDismissed -> {
                _uiState.update { it.copy(exportDialogCharacterId = null) }
            }
            CharacterAction.CharacterExportLocalClicked -> requestLocalExport()
            CharacterAction.CharacterSharePublicClicked -> sharePublic()
            is CharacterAction.CharacterExportDirectorySelected -> exportSelected(action.directory)
            else -> Unit
        }
    }

    private fun observeCharacters() {
        viewModelScope.launch {
            characterRepository.observeCharacterSummaries()
                .catch { emit(emptyList()) }
                .collect(::updateCharacters)
        }
    }

    private fun updateCharacters(summaries: List<CharacterSummary>) {
        _uiState.update { state ->
            val selectedId = state.selectedCharacterId
                ?.takeIf { id -> summaries.any { it.id == id } }
                ?: summaries.firstOrNull()?.id
            state.copy(
                characters = summaries,
                selectedCharacterId = selectedId,
                isLoadingCharacters = false,
            )
        }
    }

    private fun reorder(ids: List<String>) {
        val byId = _uiState.value.characters.associateBy { it.id }
        val reordered = ids.mapNotNull(byId::get)
        if (reordered.size != byId.size) return
        _uiState.update { it.copy(characters = reordered) }
        viewModelScope.launch {
            reordered.forEachIndexed { index, summary ->
                characterRepository.updateSortOrder(summary.id, index)
            }
        }
    }

    private fun delete(characterId: String) {
        viewModelScope.launch {
            runCatching {
                characterRepository.deleteCharacter(
                    characterId,
                    Clock.System.now().toEpochMilliseconds(),
                )
            }.onSuccess {
                _effects.send(CharacterEffect.CharacterDeleted)
            }.onFailure {
                _effects.send(
                    CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SAVE_FAILED),
                )
            }
        }
    }

    private fun exportSelected(directory: io.github.vinceglb.filekit.PlatformFile) {
        val characterId = _uiState.value.pendingLocalExportCharacterId ?: return
        viewModelScope.launch {
            val character = characterRepository.getCharacter(characterId)
            val result = character?.let { characterCardExporter.exportToPng(it, directory) }
            _uiState.update { it.copy(pendingLocalExportCharacterId = null) }
            _effects.send(
                CharacterEffect.ShowMessage(
                    if (result is ApiResult.Success) {
                        CharacterEffectMessage.CHARACTER_EXPORT_SUCCESS
                    } else {
                        CharacterEffectMessage.CHARACTER_EXPORT_FAILED
                    },
                ),
            )
        }
    }

    private fun requestLocalExport() {
        if (_uiState.value.sharingCharacterId != null) return
        val characterId = _uiState.value.exportDialogCharacterId ?: return
        _uiState.update {
            it.copy(
                exportDialogCharacterId = null,
                pendingLocalExportCharacterId = characterId,
            )
        }
        viewModelScope.launch {
            _effects.send(CharacterEffect.RequestDirectoryPicker)
        }
    }

    private fun sharePublic() {
        val characterId = _uiState.value.exportDialogCharacterId ?: return
        if (_uiState.value.sharingCharacterId != null) return
        _uiState.update { it.copy(sharingCharacterId = characterId) }
        viewModelScope.launch {
            val character = characterRepository.getCharacter(characterId)
            if (character == null) {
                _uiState.update { it.copy(sharingCharacterId = null) }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_FAILED)
                return@launch
            }
            if (character.author.isBlank()) {
                _uiState.update { it.copy(sharingCharacterId = null) }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_AUTHOR_REQUIRED)
                return@launch
            }
            if (!publicCharacterRepository.isSupported) {
                _uiState.update { it.copy(sharingCharacterId = null) }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_PLATFORM_UNSUPPORTED)
                return@launch
            }
            val result = publicCharacterRepository.share(character)
            _uiState.update { state ->
                state.copy(
                    sharingCharacterId = null,
                    exportDialogCharacterId = if (result is ApiResult.Success) {
                        null
                    } else {
                        state.exportDialogCharacterId
                    },
                )
            }
            val message = when (result) {
                is ApiResult.Success -> CharacterEffectMessage.CHARACTER_SHARE_SUCCESS
                is ApiResult.UnexpectedError -> when {
                    result.message == DefaultPublicCharacterRepository.ERROR_MEDIA_READ ->
                        CharacterEffectMessage.CHARACTER_SHARE_MEDIA_READ_FAILED
                    result.message?.contains("审核中") == true ->
                        CharacterEffectMessage.CHARACTER_SHARE_REVIEWING
                    result.message == DefaultPublicCharacterRepository.ERROR_PLATFORM_UNSUPPORTED ->
                        CharacterEffectMessage.CHARACTER_SHARE_PLATFORM_UNSUPPORTED
                    else -> CharacterEffectMessage.CHARACTER_SHARE_FAILED
                }
                else -> CharacterEffectMessage.CHARACTER_SHARE_FAILED
            }
            showMessage(message)
        }
    }

    private suspend fun showMessage(message: CharacterEffectMessage) {
        _effects.send(CharacterEffect.ShowMessage(message))
    }
}
