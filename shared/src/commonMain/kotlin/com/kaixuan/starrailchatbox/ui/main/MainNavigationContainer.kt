package com.kaixuan.starrailchatbox.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.kaixuan.starrailchatbox.platform.compressImageIfPossible
import com.kaixuan.starrailchatbox.platform.restartApp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.kaixuan.starrailchatbox.platform.rememberCameraLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.attach_not_ready
import starrailchatbox.shared.generated.resources.emoji_not_ready
import starrailchatbox.shared.generated.resources.microphone_not_ready
import starrailchatbox.shared.generated.resources.nav_characters
import starrailchatbox.shared.generated.resources.nav_chat
import starrailchatbox.shared.generated.resources.nav_settings
import starrailchatbox.shared.generated.resources.profile_saved
import starrailchatbox.shared.generated.resources.profile_not_ready
import starrailchatbox.shared.generated.resources.settings_api_not_ready
import starrailchatbox.shared.generated.resources.settings_update_check
import starrailchatbox.shared.generated.resources.settings_update_checking
import starrailchatbox.shared.generated.resources.settings_update_failed
import starrailchatbox.shared.generated.resources.settings_update_dialog_title
import starrailchatbox.shared.generated.resources.settings_update_dialog_version
import starrailchatbox.shared.generated.resources.settings_update_dialog_confirm
import starrailchatbox.shared.generated.resources.settings_update_dialog_cancel
import starrailchatbox.shared.generated.resources.settings_notice_not_ready
import starrailchatbox.shared.generated.resources.settings_about_desc_toast
import starrailchatbox.shared.generated.resources.settings_privacy_not_ready
import starrailchatbox.shared.generated.resources.settings_api_saved
import starrailchatbox.shared.generated.resources.settings_api_fetching
import starrailchatbox.shared.generated.resources.settings_api_fetch_success
import starrailchatbox.shared.generated.resources.settings_api_invalid
import starrailchatbox.shared.generated.resources.settings_api_auth_failed
import starrailchatbox.shared.generated.resources.settings_api_fetch_failed
import starrailchatbox.shared.generated.resources.settings_api_no_models
import starrailchatbox.shared.generated.resources.settings_api_save_failed
import starrailchatbox.shared.generated.resources.theme_changed
import starrailchatbox.shared.generated.resources.voice_not_ready
import starrailchatbox.shared.generated.resources.chat_model_config_required
import starrailchatbox.shared.generated.resources.chat_request_failed
import starrailchatbox.shared.generated.resources.chat_empty_response
import starrailchatbox.shared.generated.resources.character_saved
import starrailchatbox.shared.generated.resources.character_deleted
import starrailchatbox.shared.generated.resources.character_delete_builtin_restricted
import starrailchatbox.shared.generated.resources.character_name_empty
import starrailchatbox.shared.generated.resources.character_save_failed
import starrailchatbox.shared.generated.resources.character_edit_prompt_gen_failed
import starrailchatbox.shared.generated.resources.character_name_required
import starrailchatbox.shared.generated.resources.character_edit_export_success
import starrailchatbox.shared.generated.resources.character_edit_export_failed
import starrailchatbox.shared.generated.resources.character_edit_import_success
import starrailchatbox.shared.generated.resources.character_edit_import_failed
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.chat.ChatAction
import com.kaixuan.starrailchatbox.ui.chat.ChatEffect
import com.kaixuan.starrailchatbox.ui.chat.ChatSessionBottomBar
import com.kaixuan.starrailchatbox.ui.chat.ChatSessionScreen
import com.kaixuan.starrailchatbox.ui.chat.CharacterChatScreen
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.CharacterEffect
import com.kaixuan.starrailchatbox.ui.character.CharacterEffectMessage
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.character.ChatCharactersUiState
import com.kaixuan.starrailchatbox.ui.character.CharactersScreen
import com.kaixuan.starrailchatbox.ui.character.CharacterEditScreen
import com.kaixuan.starrailchatbox.ui.character.CharacterEditViewModel
import com.kaixuan.starrailchatbox.ui.character.CharacterEditArgs
import com.kaixuan.starrailchatbox.ui.character.CharactersViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatUiState
import com.kaixuan.starrailchatbox.ui.chat.CharacterChatState
import com.kaixuan.starrailchatbox.ui.chat.ChatMessageUiModel
import com.kaixuan.starrailchatbox.ui.chat.MessageContent
import com.kaixuan.starrailchatbox.ui.chat.ConversationManagementScreen
import com.kaixuan.starrailchatbox.ui.chat.EffectMessage
import com.kaixuan.starrailchatbox.ui.chat.RecordingOverlay
import com.kaixuan.starrailchatbox.ui.components.NavigationPlaceholderScreen
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.navigation.Route
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import com.kaixuan.starrailchatbox.ui.settings.ApiSettingsScreen
import com.kaixuan.starrailchatbox.ui.settings.SettingsScreen
import com.kaixuan.starrailchatbox.ui.settings.AboutScreen
import com.kaixuan.starrailchatbox.ui.settings.PrivacyPolicyScreen
import com.kaixuan.starrailchatbox.ui.settings.SettingsAction
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffect
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffectMessage
import com.kaixuan.starrailchatbox.ui.settings.SettingsOverviewUiState
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import com.kaixuan.starrailchatbox.ui.settings.SettingsOverviewViewModel
import com.kaixuan.starrailchatbox.ui.profile.ProfileScreen
import com.kaixuan.starrailchatbox.ui.profile.ProfileAction
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffect
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffectMessage
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import com.kaixuan.starrailchatbox.ui.components.StarRailDialog
import com.kaixuan.starrailchatbox.platform.openUri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import starrailchatbox.shared.generated.resources.image_save_failed
import starrailchatbox.shared.generated.resources.image_save_success
import starrailchatbox.shared.generated.resources.profile_export_success
import starrailchatbox.shared.generated.resources.profile_import_success
import starrailchatbox.shared.generated.resources.settings_copied_success
import com.kaixuan.starrailchatbox.getPlatform
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import org.koin.core.Koin
import org.koin.dsl.koinApplication
import org.koin.core.parameter.parametersOf

private val NavigationSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.ChatSession::class, Route.ChatSession.serializer())
            subclass(Route.ConversationManagement::class, Route.ConversationManagement.serializer())
            subclass(Route.CharacterEdit::class, Route.CharacterEdit.serializer())
            subclass(Route.Characters::class, Route.Characters.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
            subclass(Route.ApiSettings::class, Route.ApiSettings.serializer())
            subclass(Route.MultimodalApiSettings::class, Route.MultimodalApiSettings.serializer())
            subclass(Route.VoiceApiSettings::class, Route.VoiceApiSettings.serializer())
            subclass(Route.ImageGenerationApiSettings::class, Route.ImageGenerationApiSettings.serializer())
            subclass(Route.Profile::class, Route.Profile.serializer())
            subclass(Route.About::class, Route.About.serializer())
            subclass(Route.PrivacyPolicy::class, Route.PrivacyPolicy.serializer())
            subclass(Route.CharacterChat::class, Route.CharacterChat.serializer())
        }
    }
}

@Composable
fun MainRoute(
    main: MainRouteBinding,
    koin: Koin,
) {
    val backStack = rememberNavBackStack(
        NavigationSavedStateConfiguration,
        Route.ChatSession,
    )
    val rootViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current)
    var charactersVisited by remember { mutableStateOf(false) }
    var settingsVisited by remember { mutableStateOf(false) }
    val currentEntry = backStack.lastOrNull()
    LaunchedEffect(currentEntry) {
        when (currentEntry) {
            Route.Characters -> charactersVisited = true
            Route.Settings -> settingsVisited = true
            else -> Unit
        }
    }

    val chatViewModel = viewModel<ChatViewModel>(
        viewModelStoreOwner = rootViewModelStoreOwner,
        key = "root-chat",
    ) { koin.get() }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val chatCharacterState by chatViewModel.characterUiState.collectAsStateWithLifecycle()
    val chat = ChatRouteBinding(
        state = chatState,
        effects = chatViewModel.effects,
        onAction = chatViewModel::onAction,
    )
    val chatCharacters = ChatCharactersRouteBinding(
        state = chatCharacterState,
        effects = chatViewModel.characterEffects,
        onAction = chatViewModel::onCharacterAction,
    )

    val charactersViewModel = if (charactersVisited) {
        viewModel<CharactersViewModel>(
            viewModelStoreOwner = rootViewModelStoreOwner,
            key = "root-characters",
        ) { koin.get() }
    } else {
        null
    }
    val charactersState = charactersViewModel?.uiState?.collectAsStateWithLifecycle()?.value
        ?: CharactersUiState()
    val characters = CharactersRouteBinding(
        state = charactersState,
        effects = charactersViewModel?.effects ?: emptyFlow(),
        onAction = charactersViewModel?.let { it::onAction } ?: {},
    )

    val settingsViewModel = if (settingsVisited) {
        viewModel<SettingsOverviewViewModel>(
            viewModelStoreOwner = rootViewModelStoreOwner,
            key = "root-settings-overview",
        ) { koin.get() }
    } else {
        null
    }
    val settingsState = settingsViewModel?.uiState?.collectAsStateWithLifecycle()?.value
        ?: SettingsOverviewUiState()
    val settings = SettingsRouteBinding(state = settingsState)
    val onMainAction: (MainAction) -> Unit = { action ->
        when (action) {
            is MainAction.NavigationSelected -> {
                backStack.clear()
                backStack.add(action.route)
            }
            is MainAction.NavigateTo -> {
                if (backStack.lastOrNull() != action.route) {
                    backStack.add(action.route)
                }
            }
            MainAction.PopBackStack -> {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            }
            is MainAction.SettingsItemClicked -> {
                val route = when (action.item) {
                    MainSettingsItem.PROFILE -> Route.Profile
                    MainSettingsItem.API_SETTINGS -> Route.ApiSettings
                    MainSettingsItem.MULTIMODAL_API_SETTINGS -> Route.MultimodalApiSettings
                    MainSettingsItem.IMAGE_GENERATION_API_SETTINGS -> Route.ImageGenerationApiSettings
                    MainSettingsItem.VOICE_API_SETTINGS -> Route.VoiceApiSettings
                    MainSettingsItem.ABOUT_US -> Route.About
                    MainSettingsItem.PRIVACY_SECURITY -> Route.PrivacyPolicy
                    else -> null
                }
                if (route != null) {
                    backStack.add(route)
                } else {
                    main.onAction(action)
                }
            }
            else -> main.onAction(action)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val versionName = remember { getPlatform().versionName }
    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { picked ->
        picked?.let { 
            coroutineScope.launch {
                val compressedUri = compressImageIfPossible(it.path ?: "")
                chat.onAction(ChatAction.ImageSelected(compressedUri, it.name, it.extension))
            }
        }
    }
    val filePicker = rememberFilePickerLauncher(type = FileKitType.File()) { picked ->
        picked?.let { chat.onAction(ChatAction.FileSelected(it.path ?: "", it.name, it.extension)) }
    }
    val characterCardPicker = rememberFilePickerLauncher(
        type = FileKitType.File(listOf("png", "json"))
    ) { picked ->
        picked?.let { 
            coroutineScope.launch {
                val bytes = com.kaixuan.starrailchatbox.platform.readUriAsBytes(it.path ?: "")
                val cacheFileName = "import_raw_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.${it.extension}"
                val cachePath = com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.cacheDir / cacheFileName
                com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.writeBytes(cachePath, bytes)
                
                onMainAction(
                    MainAction.NavigateTo(
                        Route.CharacterEdit(
                            characterId = null,
                            importPath = cachePath.toString(),
                            importName = it.name,
                            importExtension = it.extension,
                        ),
                    ),
                )
            }
        }
    }
    val cameraLauncher = rememberCameraLauncher { captured ->
        captured?.let { 
            coroutineScope.launch {
                val compressedUri = compressImageIfPossible(captured.uri)
                chat.onAction(ChatAction.ImageSelected(compressedUri, captured.name, captured.extension))
            }
        }
    }

    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            characters.onAction(CharacterAction.CharacterExportDirectorySelected(directory))
        }
    }

    val wrappedOnChatAction: (ChatAction) -> Unit = { action ->
        when (action) {
            is ChatAction.ComposerActionClicked -> {
                when (action.action) {
                    com.kaixuan.starrailchatbox.ui.chat.ComposerAction.PICK_IMAGE -> imagePicker.launch()
                    com.kaixuan.starrailchatbox.ui.chat.ComposerAction.PICK_FILE -> filePicker.launch()
                    com.kaixuan.starrailchatbox.ui.chat.ComposerAction.TAKE_PHOTO -> cameraLauncher()
                    else -> chat.onAction(action)
                }
            }
            else -> chat.onAction(action)
        }
    }

    val wrappedOnCharacterAction: (CharacterAction) -> Unit = { action ->
        when (action) {
            CharacterAction.CharacterImportClicked -> characterCardPicker.launch()
            is CharacterAction.CharacterSelected -> {
                characters.onAction(action)
                chatCharacters.onAction(action)
            }
            else -> characters.onAction(action)
        }
    }
    val wrappedOnSettingsAction: (SettingsAction) -> Unit = { action ->
        when (action) {
            is SettingsAction.SettingsItemClicked -> {
                val item = when (action.item) {
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.PROFILE -> MainSettingsItem.PROFILE
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.API_SETTINGS -> MainSettingsItem.API_SETTINGS
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.MULTIMODAL_API_SETTINGS -> MainSettingsItem.MULTIMODAL_API_SETTINGS
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.IMAGE_GENERATION_API_SETTINGS -> MainSettingsItem.IMAGE_GENERATION_API_SETTINGS
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.VOICE_API_SETTINGS -> MainSettingsItem.VOICE_API_SETTINGS
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.CHECK_UPDATE -> MainSettingsItem.CHECK_UPDATE
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.MESSAGE_NOTIFICATION -> MainSettingsItem.MESSAGE_NOTIFICATION
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.THEME_STYLE -> MainSettingsItem.THEME_STYLE
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.ABOUT_US -> MainSettingsItem.ABOUT_US
                    com.kaixuan.starrailchatbox.ui.settings.SettingsItem.PRIVACY_SECURITY -> MainSettingsItem.PRIVACY_SECURITY
                }
                onMainAction(MainAction.SettingsItemClicked(item))
            }
            is SettingsAction.CopyToClipboard -> Unit
            else -> Unit
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val chatEffectMessages = mapOf(
        EffectMessage.VOICE_NOT_READY to stringResource(Res.string.voice_not_ready),
        EffectMessage.PROFILE_NOT_READY to stringResource(Res.string.profile_not_ready),
        EffectMessage.ATTACH_NOT_READY to stringResource(Res.string.attach_not_ready),
        EffectMessage.EMOJI_NOT_READY to stringResource(Res.string.emoji_not_ready),
        EffectMessage.MICROPHONE_NOT_READY to stringResource(Res.string.microphone_not_ready),
        EffectMessage.MODEL_CONFIG_REQUIRED to stringResource(Res.string.chat_model_config_required),
        EffectMessage.CHAT_REQUEST_FAILED to stringResource(Res.string.chat_request_failed),
        EffectMessage.CHAT_EMPTY_RESPONSE to stringResource(Res.string.chat_empty_response),
    )
    val characterEffectMessages = mapOf(
        CharacterEffectMessage.CHARACTER_NAME_EMPTY to stringResource(Res.string.character_name_empty),
        CharacterEffectMessage.CHARACTER_SAVE_FAILED to stringResource(Res.string.character_save_failed),
        CharacterEffectMessage.PROMPT_GEN_FAILED to stringResource(Res.string.character_edit_prompt_gen_failed),
        CharacterEffectMessage.CHARACTER_NAME_REQUIRED to stringResource(Res.string.character_name_required),
        CharacterEffectMessage.MODEL_CONFIG_REQUIRED to stringResource(Res.string.chat_model_config_required),
        CharacterEffectMessage.CHARACTER_DELETE_BUILTIN_RESTRICTED to stringResource(Res.string.character_delete_builtin_restricted),
        CharacterEffectMessage.CHARACTER_EXPORT_SUCCESS to stringResource(Res.string.character_edit_export_success),
        CharacterEffectMessage.CHARACTER_EXPORT_FAILED to stringResource(Res.string.character_edit_export_failed),
        CharacterEffectMessage.CHARACTER_IMPORT_SUCCESS to stringResource(Res.string.character_edit_import_success),
        CharacterEffectMessage.CHARACTER_IMPORT_FAILED to stringResource(Res.string.character_edit_import_failed),
    )
    val characterSavedMessage = stringResource(Res.string.character_saved)
    val characterDeletedMessage = stringResource(Res.string.character_deleted)
    val settingsEffectMessages = mapOf(
        SettingsEffectMessage.SETTINGS_API_NOT_READY to stringResource(Res.string.settings_api_not_ready),
        SettingsEffectMessage.SETTINGS_UPDATE_CHECK to stringResource(Res.string.settings_update_check, versionName),
        SettingsEffectMessage.SETTINGS_NOTICE_NOT_READY to stringResource(Res.string.settings_notice_not_ready),
        SettingsEffectMessage.SETTINGS_ABOUT_INFO to stringResource(Res.string.settings_about_desc_toast, versionName),
        SettingsEffectMessage.SETTINGS_PRIVACY_INFO to stringResource(Res.string.settings_privacy_not_ready),
        SettingsEffectMessage.SETTINGS_API_SAVED to stringResource(Res.string.settings_api_saved),
        SettingsEffectMessage.SETTINGS_API_FETCH_START to stringResource(Res.string.settings_api_fetching),
        SettingsEffectMessage.SETTINGS_API_FETCH_SUCCESS to stringResource(Res.string.settings_api_fetch_success),
        SettingsEffectMessage.SETTINGS_API_INVALID to stringResource(Res.string.settings_api_invalid),
        SettingsEffectMessage.SETTINGS_API_AUTH_FAILED to stringResource(Res.string.settings_api_auth_failed),
        SettingsEffectMessage.SETTINGS_API_FETCH_FAILED to stringResource(Res.string.settings_api_fetch_failed),
        SettingsEffectMessage.SETTINGS_API_NO_MODELS to stringResource(Res.string.settings_api_no_models),
        SettingsEffectMessage.SETTINGS_API_SAVE_FAILED to stringResource(Res.string.settings_api_save_failed),
        SettingsEffectMessage.SETTINGS_COPIED_SUCCESS to stringResource(Res.string.settings_copied_success),
    )
    val profileEffectMessages = mapOf(
        ProfileEffectMessage.PROFILE_SAVED to stringResource(Res.string.profile_saved),
        ProfileEffectMessage.EXPORT_SUCCESS to stringResource(Res.string.profile_export_success),
        ProfileEffectMessage.IMPORT_SUCCESS to stringResource(Res.string.profile_import_success),
    )
    val mainEffectMessages = mapOf(
        MainEffectMessage.THEME_CHANGED to stringResource(Res.string.theme_changed),
        MainEffectMessage.IMAGE_SAVED to stringResource(Res.string.image_save_success),
        MainEffectMessage.IMAGE_SAVE_FAILED to stringResource(Res.string.image_save_failed),
        MainEffectMessage.ALREADY_LATEST_VERSION to stringResource(Res.string.settings_update_check, versionName),
        MainEffectMessage.CHECKING_FOR_UPDATE to stringResource(Res.string.settings_update_checking),
        MainEffectMessage.UPDATE_CHECK_FAILED to stringResource(Res.string.settings_update_failed),
    )

    LaunchedEffect(main.effects, mainEffectMessages) {
        main.effects.collectLatest { effect ->
            when (effect) {
                is MainEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        mainEffectMessages.getValue(effect.message),
                    )
                }
            }
        }
    }

    LaunchedEffect(chat.effects, chatEffectMessages) {
        chat.effects.collectLatest { effect ->
            when (effect) {
                is ChatEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        chatEffectMessages.getValue(effect.message),
                    )
                }
            }
        }
    }

    LaunchedEffect(characters.effects, characterEffectMessages, characterSavedMessage, characterDeletedMessage) {
        val scope = this
        characters.effects.collectLatest { effect ->
            when (effect) {
                is CharacterEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        characterEffectMessages.getValue(effect.message),
                    )
                }
                CharacterEffect.CharacterSaved -> {
                    scope.launch { snackbarHostState.showSnackbar(characterSavedMessage) }
                    onMainAction(MainAction.PopBackStack)
                }
                CharacterEffect.CharacterDeleted -> {
                    scope.launch { snackbarHostState.showSnackbar(characterDeletedMessage) }
                    onMainAction(MainAction.PopBackStack)
                }
                CharacterEffect.RequestDirectoryPicker -> {
                    directoryPicker.launch()
                }
            }
        }
    }

    LaunchedEffect(chatCharacters.effects, characterEffectMessages, characterSavedMessage) {
        val scope = this
        chatCharacters.effects.collectLatest { effect ->
            when (effect) {
                is CharacterEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        characterEffectMessages.getValue(effect.message),
                    )
                }
                CharacterEffect.CharacterSaved -> {
                    scope.launch { snackbarHostState.showSnackbar(characterSavedMessage) }
                    onMainAction(MainAction.PopBackStack)
                }
                CharacterEffect.CharacterDeleted -> onMainAction(MainAction.PopBackStack)
                CharacterEffect.RequestDirectoryPicker -> directoryPicker.launch()
            }
        }
    }

    MainNavigationContainer(
        mainState = main.state,
        backStack = backStack,
        charactersState = characters.state,
        chatCharactersState = chatCharacters.state,
        chatState = chat.state,
        settingsState = settings.state,
        snackbarHostState = snackbarHostState,
        characterEffectMessages = characterEffectMessages,
        settingsEffectMessages = settingsEffectMessages,
        profileEffectMessages = profileEffectMessages,
        koin = koin,
        onMainAction = onMainAction,
        onCharacterAction = wrappedOnCharacterAction,
        onChatCharacterAction = chatCharacters.onAction,
        onChatAction = wrappedOnChatAction,
        onSettingsAction = wrappedOnSettingsAction,
    )

    // Global Update Dialog
    if (main.state.showUpdateDialog && main.state.updateInfo != null) {
        UpdateDialog(
            info = main.state.updateInfo,
            onMainAction = onMainAction
        )
    }
}

@Composable
private fun UpdateDialog(
    info: UpdateInfo,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier
) {
    StarRailDialog(
        title = stringResource(Res.string.settings_update_dialog_title),
        dismissText = stringResource(Res.string.settings_update_dialog_cancel),
        confirmText = stringResource(Res.string.settings_update_dialog_confirm),
        onDismissRequest = { onMainAction(MainAction.UpdateDialogDismiss) },
        onConfirm = {
            openUri(info.downloadUrl)
            onMainAction(MainAction.UpdateDialogConfirm)
        },
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Version Badge
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Text(
                    text = stringResource(Res.string.settings_update_dialog_version, info.version),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Update Description
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Text(
                    text = info.description,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun MainNavigationContainer(
    mainState: MainUiState,
    backStack: List<NavKey>,
    charactersState: CharactersUiState,
    chatCharactersState: ChatCharactersUiState,
    chatState: ChatUiState,
    settingsState: SettingsOverviewUiState,
    snackbarHostState: SnackbarHostState,
    characterEffectMessages: Map<CharacterEffectMessage, String>,
    settingsEffectMessages: Map<SettingsEffectMessage, String>,
    profileEffectMessages: Map<ProfileEffectMessage, String>,
    koin: Koin,
    onMainAction: (MainAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onChatCharacterAction: (CharacterAction) -> Unit,
    onChatAction: (ChatAction) -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.starRailColors
    val currentRoute = backStack.lastOrNull() as? Route ?: Route.ChatSession

    var isRecording by remember { mutableStateOf(false) }
    var isCancelTargeted by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        colors.backgroundGlow.copy(alpha = if (isSystemInDarkTheme()) 0.12f else 0.38f),
                        MaterialTheme.colorScheme.background,
                    ),
                    start = Offset.Zero,
                    end = Offset(0f, Float.POSITIVE_INFINITY)
                ),
            ),
    ) {
        val expanded = maxWidth >= 840.dp
        val compact = maxWidth < 480.dp
        StarfieldBackground()

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
        ) {
            val showRail = expanded && backStack.size <= 1
            if (showRail) {
                MainNavigationRail(
                    currentRoute = currentRoute,
                    onDestinationSelected = {
                        onMainAction(MainAction.NavigationSelected(it))
                    },
                )
            }

            Scaffold(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 840.dp),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = 60.dp)
                    )
                },
                bottomBar = {
                    MainBottomArea(
                        backStack = backStack,
                        showNavigationBar = !showRail,
                        compact = compact,
                        onMainAction = onMainAction,
                    )
                },
            ) { contentPadding ->
                val entryProvider = entryProvider<NavKey> {
                    entry<Route.ChatSession> {
                        ChatSessionRoute(
                            state = chatState,
                            charactersState = chatCharactersState,
                            navigationBarPadding = contentPadding,
                            applyNavigationBarInsets = showRail,
                            compact = compact,
                            onAction = onChatAction,
                            onCharacterAction = onCharacterAction,
                            onMainAction = onMainAction,
                            isRecording = isRecording,
                            isCancelTargeted = isCancelTargeted,
                            onRecordingStateChanged = { recording, cancelTargeted ->
                                isRecording = recording
                                isCancelTargeted = cancelTargeted
                            },
                        )
                    }
                    entry<Route.ConversationManagement> {
                        ConversationManagementScreen(
                            state = chatState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onAction = onChatAction,
                            onMainAction = onMainAction,
                        )
                    }
                    entry<Route.CharacterEdit> { entry ->
                        CharacterEditRoute(
                            route = entry,
                            koin = koin,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onCharactersChanged = {
                                onCharacterAction(CharacterAction.RefreshCharacters)
                                onChatCharacterAction(CharacterAction.RefreshCharacters)
                            },
                            snackbarHostState = snackbarHostState,
                            effectMessages = characterEffectMessages,
                        )
                    }
                    entry<Route.Characters> {
                        CharactersScreen(
                            state = charactersState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onAction = onCharacterAction,
                        )
                    }
                    entry<Route.Settings> {
                        SettingsScreen(
                            mainState = mainState,
                            settingsState = settingsState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onSettingsAction = onSettingsAction,
                        )
                    }
                    entry<Route.ApiSettings> {
                        ApiSettingsRoute(
                            koin = koin,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            snackbarHostState = snackbarHostState,
                            effectMessages = settingsEffectMessages,
                        )
                    }
                    entry<Route.MultimodalApiSettings> {
                        ApiSettingsRoute(
                            koin = koin,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            snackbarHostState = snackbarHostState,
                            effectMessages = settingsEffectMessages,
                            isMultimodal = true,
                        )
                    }
                    entry<Route.VoiceApiSettings> {
                        ApiSettingsRoute(
                            koin = koin,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            snackbarHostState = snackbarHostState,
                            effectMessages = settingsEffectMessages,
                            isVoice = true,
                        )
                    }
                    entry<Route.ImageGenerationApiSettings> {
                        ApiSettingsRoute(
                            koin = koin,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            snackbarHostState = snackbarHostState,
                            effectMessages = settingsEffectMessages,
                            isImageGeneration = true,
                        )
                    }
                    entry<Route.Profile> {
                        ProfileRoute(
                            koin = koin,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            snackbarHostState = snackbarHostState,
                            effectMessages = profileEffectMessages,
                        )
                    }
                    entry<Route.About> {
                        AboutScreen(
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                        )
                    }
                    entry<Route.PrivacyPolicy> {
                        PrivacyPolicyScreen(
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                        )
                    }
                    entry<Route.CharacterChat> { entry ->
                        CharacterChatScreen(
                            characterId = entry.characterId,
                            state = chatState,
                            charactersState = chatCharactersState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onAction = onChatAction,
                            onCharacterAction = onCharacterAction,
                            onMainAction = onMainAction,
                        )
                    }
                }

                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            onMainAction(MainAction.PopBackStack)
                        }
                    },
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = {
                        EnterTransition.None togetherWith ExitTransition.None
                    },
                    popTransitionSpec = {
                        EnterTransition.None togetherWith ExitTransition.None
                    },
                    predictivePopTransitionSpec = {
                        EnterTransition.None togetherWith ExitTransition.None
                    },
                    entryProvider = entryProvider,
                )
            }
        }
    }
}

@Composable
private fun ChatSessionRoute(
    state: ChatUiState,
    charactersState: ChatCharactersUiState,
    navigationBarPadding: PaddingValues,
    applyNavigationBarInsets: Boolean,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onMainAction: (MainAction) -> Unit,
    isRecording: Boolean,
    isCancelTargeted: Boolean,
    onRecordingStateChanged: (Boolean, Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = navigationBarPadding.calculateBottomPadding()),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
                shadowElevation = 8.dp,
            ) {
                Box(
                    modifier = if (applyNavigationBarInsets) {
                        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    } else {
                        Modifier
                    },
                ) {
                    ChatSessionBottomBar(
                        state = state,
                        compact = compact,
                        onAction = onAction,
                        onRecordingStateChanged = onRecordingStateChanged,
                    )
                }
            }
        },
    ) { contentPadding ->
        ChatSessionScreen(
            state = state,
            charactersState = charactersState,
            contentPadding = contentPadding,
            compact = compact,
            onAction = onAction,
            onCharacterAction = onCharacterAction,
            onMainAction = onMainAction,
            isRecording = isRecording,
            isCancelTargeted = isCancelTargeted,
        )
    }
}

@Composable
private fun CharacterEditRoute(
    route: Route.CharacterEdit,
    koin: Koin,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onCharactersChanged: () -> Unit,
    snackbarHostState: SnackbarHostState,
    effectMessages: Map<CharacterEffectMessage, String>,
) {
    val viewModel = viewModel {
        koin.get<CharacterEditViewModel>(
            parameters = {
                parametersOf(
                    CharacterEditArgs(
                        characterId = route.characterId,
                        importPath = route.importPath,
                        importName = route.importName,
                        importExtension = route.importExtension,
                    ),
                )
            },
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val savedMessage = stringResource(Res.string.character_saved)
    val deletedMessage = stringResource(Res.string.character_deleted)
    val directoryPicker = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            viewModel.onAction(CharacterAction.CharacterExportDirectorySelected(directory))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is CharacterEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effectMessages.getValue(effect.message))
                }
                CharacterEffect.CharacterSaved -> {
                    onCharactersChanged()
                    snackbarHostState.showSnackbar(savedMessage)
                    onMainAction(MainAction.PopBackStack)
                }
                CharacterEffect.CharacterDeleted -> {
                    onCharactersChanged()
                    snackbarHostState.showSnackbar(deletedMessage)
                    onMainAction(MainAction.PopBackStack)
                }
                CharacterEffect.RequestDirectoryPicker -> directoryPicker.launch()
            }
        }
    }

    CharacterEditScreen(
        state = state,
        contentPadding = contentPadding,
        compact = compact,
        onMainAction = onMainAction,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun ApiSettingsRoute(
    koin: Koin,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    effectMessages: Map<SettingsEffectMessage, String>,
    isMultimodal: Boolean = false,
    isVoice: Boolean = false,
    isImageGeneration: Boolean = false,
) {
    val viewModel = viewModel { koin.get<SettingsViewModel>() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effectMessages.getValue(effect.message))
                }
                SettingsEffect.ApiSettingsSaved -> {
                    snackbarHostState.showSnackbar(
                        effectMessages.getValue(SettingsEffectMessage.SETTINGS_API_SAVED),
                    )
                    onMainAction(MainAction.PopBackStack)
                }
                SettingsEffect.NavigateBack -> onMainAction(MainAction.PopBackStack)
            }
        }
    }

    ApiSettingsScreen(
        state = state,
        contentPadding = contentPadding,
        compact = compact,
        onMainAction = onMainAction,
        onSettingsAction = viewModel::onAction,
        isMultimodal = isMultimodal,
        isVoice = isVoice,
        isImageGeneration = isImageGeneration,
    )
}

@Composable
private fun ProfileRoute(
    koin: Koin,
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    effectMessages: Map<ProfileEffectMessage, String>,
) {
    val viewModel = viewModel { koin.get<ProfileViewModel>() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effectMessages.getValue(effect.message))
                }
                ProfileEffect.ProfileSaved -> {
                    snackbarHostState.showSnackbar(
                        effectMessages.getValue(ProfileEffectMessage.PROFILE_SAVED),
                    )
                    onMainAction(MainAction.PopBackStack)
                }
                ProfileEffect.NavigateBack -> onMainAction(MainAction.PopBackStack)
                ProfileEffect.RestartApp -> {
                    snackbarHostState.showSnackbar(
                        effectMessages.getValue(ProfileEffectMessage.IMPORT_SUCCESS),
                    )
                    kotlinx.coroutines.delay(1500)
                    restartApp()
                }
            }
        }
    }

    ProfileScreen(
        state = state,
        contentPadding = contentPadding,
        compact = compact,
        onMainAction = onMainAction,
        onAction = viewModel::onAction,
    )
}

@Composable
private fun MainBottomArea(
    backStack: List<NavKey>,
    showNavigationBar: Boolean,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
) {
    if (backStack.size > 1 || !showNavigationBar) {
        return
    }
    val currentRoute = backStack.lastOrNull() as? Route ?: Route.ChatSession

    Column {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                MainNavigationBar(
                    currentRoute = currentRoute,
                    compact = compact,
                    onDestinationSelected = {
                        onMainAction(MainAction.NavigationSelected(it))
                    },
                )
            }
        }
    }
}

private data class NavigationItem(
    val route: Route,
    val label: StringResource,
    val icon: StarRailIconKind,
)

private val navigationItems = listOf(
    NavigationItem(
        Route.Characters,
        Res.string.nav_characters,
        StarRailIconKind.PERSON,
    ),
    NavigationItem(
        Route.ChatSession,
        Res.string.nav_chat,
        StarRailIconKind.CONVERSATION,
    ),
    NavigationItem(
        Route.Settings,
        Res.string.nav_settings,
        StarRailIconKind.SETTINGS,
    ),
)

@Composable
private fun MainNavigationBar(
    currentRoute: Route,
    compact: Boolean,
    onDestinationSelected: (Route) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.height(if (compact) 72.dp else 80.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        navigationItems.forEach { item ->
            val selected = when (item.route) {
                Route.Settings -> currentRoute == Route.Settings || currentRoute == Route.ApiSettings || currentRoute == Route.MultimodalApiSettings || currentRoute == Route.VoiceApiSettings || currentRoute == Route.ImageGenerationApiSettings || currentRoute == Route.About || currentRoute == Route.Profile
                else -> item.route == currentRoute
            }
            val label = stringResource(item.label)
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(item.route) },
                icon = {
                    StarRailIcon(
                        kind = item.icon,
                        contentDescription = label,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(if (compact) 24.dp else 26.dp),
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun MainNavigationRail(
    currentRoute: Route,
    onDestinationSelected: (Route) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
    ) {
        Spacer(Modifier.height(StarRailSpacing.lg))
        navigationItems.forEach { item ->
            val selected = when (item.route) {
                Route.Settings -> currentRoute == Route.Settings || currentRoute == Route.ApiSettings || currentRoute == Route.MultimodalApiSettings || currentRoute == Route.VoiceApiSettings || currentRoute == Route.ImageGenerationApiSettings || currentRoute == Route.About || currentRoute == Route.Profile
                else -> item.route == currentRoute
            }
            val label = stringResource(item.label)
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationSelected(item.route) },
                icon = {
                    StarRailIcon(
                        kind = item.icon,
                        contentDescription = label,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp),
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun StarfieldBackground(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.starRailColors
    val darkTheme = isSystemInDarkTheme()
    val density = LocalDensity.current

    // ==========================================
    // 1. 性能优化：在 Canvas 外部把所有 dp 转换为 px 并缓存
    // ==========================================
    val config = remember(colors, darkTheme, density) {
        with(density) {
            val nebulaAlpha = if (darkTheme) 0.08f else 0.12f
            StarfieldConfig(
                pathStrokeWidth = 1.2.dp.toPx(),
                starSizes = listOf(2.2.dp.toPx(), 1.5.dp.toPx(), 1.0.dp.toPx()),
                sparkleSizes = listOf(14.dp.toPx(), 10.dp.toPx(), 12.dp.toPx()),
                // 提前计算好带 alpha 的颜色，避免在 draw 期间频繁 copy()
                nebulaColor1 = colors.backgroundGlow.copy(alpha = nebulaAlpha),
                nebulaColor2 = colors.constellation.copy(alpha = nebulaAlpha * 0.6f),
                pathColor1 = colors.constellationMuted.copy(alpha = if (darkTheme) 0.35f else 0.25f),
                starBaseColor = colors.constellation,
                sparkleColor1 = colors.constellation.copy(alpha = 0.75f),
                sparkleColor2 = colors.warmSparkle.copy(alpha = 0.65f),
                sparkleColor3 = colors.constellationMuted.copy(alpha = 0.55f)
            )
        }
    }

    // ==========================================
    // 2. 动效增强：引入微弱的呼吸感（完全硬件加速）
    // ==========================================
    val infiniteTransition = rememberInfiniteTransition(label = "StarfieldAnimation")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathe"
    )

    // 静态的星星坐标
    val starCoords = remember {
        listOf(
            0.12f to 0.15f, 0.85f to 0.08f, 0.92f to 0.25f,
            0.05f to 0.45f, 0.75f to 0.55f, 0.20f to 0.70f,
            0.88f to 0.78f, 0.45f to 0.85f, 0.55f to 0.12f,
            0.35f to 0.32f, 0.65f to 0.42f, 0.15f to 0.88f,
            0.52f to 0.52f, 0.28f to 0.05f, 0.95f to 0.65f
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // 1. Nebula / Glow spots
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(config.nebulaColor1, Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.3f),
                radius = size.width * 0.7f
            ),
            center = Offset(size.width * 0.2f, size.height * 0.3f),
            radius = size.width * 0.7f
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(config.nebulaColor2, Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.7f),
                radius = size.width * 0.6f
            ),
            center = Offset(size.width * 0.8f, size.height * 0.7f),
            radius = size.width * 0.6f
        )

        // 2. Orbital Paths
        val stroke = Stroke(width = config.pathStrokeWidth)
        drawOval(
            color = config.pathColor1,
            topLeft = Offset(-size.width * 0.2f, size.height * 0.1f),
            size = Size(size.width * 1.4f, size.height * 0.45f),
            style = stroke,
        )

        drawOval(
            color = config.pathColor1.copy(alpha = config.pathColor1.alpha * 0.6f),
            topLeft = Offset(size.width * 0.3f, -size.height * 0.15f),
            size = Size(size.width * 1.3f, size.height * 0.7f),
            style = stroke,
        )

        // 3. Stars - 叠加上述的 breatheAlpha 动效
        starCoords.forEachIndexed { index, (x, y) ->
            val radius = config.starSizes[index % 3]
            val baseAlpha = if (index % 2 == 0) 0.7f else 0.4f
            // 让奇数和偶数位星星交错呼吸
            val finalAlpha = baseAlpha * (if (index % 2 == 0) breatheAlpha else (1.6f - breatheAlpha))

            drawCircle(
                color = config.starBaseColor.copy(alpha = finalAlpha.coerceIn(0f, 1f)),
                radius = radius,
                center = Offset(size.width * x, size.height * y),
            )
        }

        // 4. Sparkles - 同样绑定动画使其闪烁
        drawDecorativeSparkle(
            center = Offset(size.width * 0.18f, size.height * 0.22f),
            radius = config.sparkleSizes[0] * breatheAlpha,
            color = config.sparkleColor1,
        )

        drawDecorativeSparkle(
            center = Offset(size.width * 0.82f, size.height * 0.35f),
            radius = config.sparkleSizes[1] * (1.6f - breatheAlpha),
            color = config.sparkleColor2,
        )

        drawDecorativeSparkle(
            center = Offset(size.width * 0.25f, size.height * 0.65f),
            radius = config.sparkleSizes[2] * breatheAlpha,
            color = config.sparkleColor3,
        )
    }
}

// 辅助类：用于打包缓存的数据
private class StarfieldConfig(
    val pathStrokeWidth: Float,
    val starSizes: List<Float>,
    val sparkleSizes: List<Float>,
    val nebulaColor1: Color,
    val nebulaColor2: Color,
    val pathColor1: Color,
    val starBaseColor: Color,
    val sparkleColor1: Color,
    val sparkleColor2: Color,
    val sparkleColor3: Color
)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDecorativeSparkle(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + radius * 0.08f, center.y - radius * 0.08f, center.x + radius, center.y)
        quadraticTo(center.x + radius * 0.08f, center.y + radius * 0.08f, center.x, center.y + radius)
        quadraticTo(center.x - radius * 0.08f, center.y + radius * 0.08f, center.x - radius, center.y)
        quadraticTo(center.x - radius * 0.08f, center.y - radius * 0.08f, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)

    // Core glow
    drawCircle(
        color = color.copy(alpha = color.alpha * 0.4f),
        radius = radius * 0.25f,
        center = center
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun MainContainerLightPreview() {
    val previewKoin = remember { koinApplication {} }
    StarRailTheme(darkThemeOverride = false) {
        MainRoute(
            main = MainRouteBinding(
                state = MainUiState(),
                effects = emptyFlow(),
                onAction = {},
            ),
            koin = previewKoin.koin,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun MainContainerDarkPreview() {
    val previewKoin = remember { koinApplication {} }
    StarRailTheme(darkThemeOverride = true) {
        MainRoute(
            main = MainRouteBinding(
                state = MainUiState(darkThemeOverride = true),
                effects = emptyFlow(),
                onAction = {},
            ),
            koin = previewKoin.koin,
        )
    }
}

private val previewCharacter = Character(
    id = "builtin:流萤",
    name = "流萤",
    prompt = "Preview prompt",
    openingMessage = "今天要聊点什么呢？",
    avatarUri = "",
)

private val previewCharactersState = CharactersUiState(
    characters = listOf(
        com.kaixuan.starrailchatbox.data.character.CharacterSummary(
            id = previewCharacter.id,
            name = previewCharacter.name,
            avatarUri = previewCharacter.avatarUri,
        ),
    ),
    selectedCharacterId = "builtin:流萤",
    isLoadingCharacters = false,
)

private val previewChatCharactersState = ChatCharactersUiState(
    characters = listOf(
        com.kaixuan.starrailchatbox.data.character.CharacterSummary(
            id = previewCharacter.id,
            name = previewCharacter.name,
            avatarUri = previewCharacter.avatarUri,
        ),
    ),
    selectedCharacterId = "builtin:流萤",
    isLoadingCharacters = false,
)

private val previewChatState = ChatUiState(
    selectedCharacterId = "builtin:流萤",
    selectedCharacter = previewCharacter,
    characterStates = mapOf(
        "builtin:流萤" to CharacterChatState(
            activeSessionId = "preview-session",
            messages = listOf(
                ChatMessageUiModel.Received(
                    id = "preview-opening",
                    timestamp = "10:21",
                    createdAt = 0L,
                    content = MessageContent.Custom("今天要聊点什么呢？"),
                    senderId = "builtin:流萤",
                )
            ),
            messageDraft = "",
            isLoadingSession = false,
            suggestions = listOf("讲讲星核猎手", "你喜欢橡木蛋糕卷吗", "关于这片星空...", "想听听你的过去"),
        )
    ),
)
