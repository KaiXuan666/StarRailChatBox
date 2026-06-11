package com.kaixuan.starrailchatbox.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class CharactersViewModel(
    private val characterRepository: CharacterRepository,
    private val characterCardExporter: CharacterCardExporter,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        refresh()
    }

    fun onAction(action: CharacterAction) {
        when (action) {
            CharacterAction.RefreshCharacters -> refresh()
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
            CharacterAction.CharacterExportClicked -> {
                viewModelScope.launch { _effects.send(CharacterEffect.RequestDirectoryPicker) }
            }
            is CharacterAction.CharacterExportDirectorySelected -> exportSelected(action.directory)
            else -> Unit
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val summaries = runCatching { characterRepository.loadCharacterSummaries() }
                .getOrDefault(emptyList())
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
                refresh()
                _effects.send(CharacterEffect.CharacterDeleted)
            }.onFailure {
                _effects.send(
                    CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SAVE_FAILED),
                )
            }
        }
    }

    private fun exportSelected(directory: io.github.vinceglb.filekit.PlatformFile) {
        val characterId = _uiState.value.selectedCharacterId ?: return
        viewModelScope.launch {
            val character = characterRepository.getCharacter(characterId)
            val result = character?.let { characterCardExporter.exportToPng(it, directory) }
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
}
