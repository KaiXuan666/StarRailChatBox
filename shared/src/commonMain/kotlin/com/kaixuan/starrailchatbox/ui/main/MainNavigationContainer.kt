package com.kaixuan.starrailchatbox.ui.main

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
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
import com.kaixuan.starrailchatbox.ui.character.CharactersScreen
import com.kaixuan.starrailchatbox.ui.character.CharacterEditScreen
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
import com.kaixuan.starrailchatbox.ui.navigation.NavDisplay
import com.kaixuan.starrailchatbox.ui.navigation.Route
import com.kaixuan.starrailchatbox.ui.navigation.entryProvider
import com.kaixuan.starrailchatbox.ui.settings.ApiSettingsScreen
import com.kaixuan.starrailchatbox.ui.settings.SettingsScreen
import com.kaixuan.starrailchatbox.ui.settings.AboutScreen
import com.kaixuan.starrailchatbox.ui.settings.SettingsAction
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffect
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffectMessage
import com.kaixuan.starrailchatbox.ui.settings.SettingsUiState
import com.kaixuan.starrailchatbox.ui.profile.ProfileScreen
import com.kaixuan.starrailchatbox.ui.profile.ProfileAction
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffect
import com.kaixuan.starrailchatbox.ui.profile.ProfileUiState
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffectMessage
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import starrailchatbox.shared.generated.resources.image_save_failed
import starrailchatbox.shared.generated.resources.image_save_success
import starrailchatbox.shared.generated.resources.profile_export_success
import starrailchatbox.shared.generated.resources.profile_import_success
import starrailchatbox.shared.generated.resources.settings_copied_success

@Composable
fun MainRoute(
    main: MainRouteBinding,
    characters: CharactersRouteBinding,
    chat: ChatRouteBinding,
    settings: SettingsRouteBinding,
    profile: ProfileRouteBinding,
) {
    val coroutineScope = rememberCoroutineScope()
    val imagePicker = rememberFilePickerLauncher(type = FileKitType.Image) { picked ->
        picked?.let { 
            coroutineScope.launch {
                val compressedUri = compressImageIfPossible(it.path ?: "")
                chat.onAction(ChatAction.ImageSelected(compressedUri, it.name))
            }
        }
    }
    val filePicker = rememberFilePickerLauncher(type = FileKitType.File()) { picked ->
        picked?.let { chat.onAction(ChatAction.FileSelected(it.path ?: "", it.name)) }
    }
    val cameraLauncher = rememberCameraLauncher { captured ->
        captured?.let { 
            coroutineScope.launch {
                val compressedUri = compressImageIfPossible(captured.uri)
                chat.onAction(ChatAction.ImageSelected(compressedUri, captured.name))
            }
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
    )
    val characterSavedMessage = stringResource(Res.string.character_saved)
    val characterDeletedMessage = stringResource(Res.string.character_deleted)
    val settingsEffectMessages = mapOf(
        SettingsEffectMessage.SETTINGS_API_NOT_READY to stringResource(Res.string.settings_api_not_ready),
        SettingsEffectMessage.SETTINGS_UPDATE_CHECK to stringResource(Res.string.settings_update_check),
        SettingsEffectMessage.SETTINGS_NOTICE_NOT_READY to stringResource(Res.string.settings_notice_not_ready),
        SettingsEffectMessage.SETTINGS_ABOUT_INFO to stringResource(Res.string.settings_about_desc_toast),
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
                    main.onAction(MainAction.PopBackStack)
                }
                CharacterEffect.CharacterDeleted -> {
                    scope.launch { snackbarHostState.showSnackbar(characterDeletedMessage) }
                    main.onAction(MainAction.PopBackStack)
                }
            }
        }
    }

    LaunchedEffect(settings.effects, settingsEffectMessages) {
        val scope = this
        settings.effects.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        settingsEffectMessages.getValue(effect.message),
                    )
                }
                SettingsEffect.ApiSettingsSaved -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            settingsEffectMessages.getValue(SettingsEffectMessage.SETTINGS_API_SAVED),
                        )
                    }
                    main.onAction(MainAction.PopBackStack)
                }
                SettingsEffect.NavigateBack -> {
                    main.onAction(MainAction.PopBackStack)
                }
            }
        }
    }

    LaunchedEffect(profile.effects, profileEffectMessages) {
        val scope = this
        profile.effects.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        profileEffectMessages.getValue(effect.message),
                    )
                }
                ProfileEffect.ProfileSaved -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            profileEffectMessages.getValue(ProfileEffectMessage.PROFILE_SAVED),
                        )
                    }
                    main.onAction(MainAction.PopBackStack)
                }
                ProfileEffect.NavigateBack -> {
                    main.onAction(MainAction.PopBackStack)
                }
                ProfileEffect.RestartApp -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            profileEffectMessages.getValue(ProfileEffectMessage.IMPORT_SUCCESS),
                        )
                        // Give some time for the snackbar to be seen if possible, 
                        // but restartApp might be immediate.
                        // Actually, importing is usually followed by a restart.
                        kotlinx.coroutines.delay(1500)
                        restartApp()
                    }
                }
            }
        }
    }

    MainNavigationContainer(
        mainState = main.state,
        charactersState = characters.state,
        chatState = chat.state,
        settingsState = settings.state,
        profileState = profile.state,
        snackbarHostState = snackbarHostState,
        onMainAction = main.onAction,
        onCharacterAction = characters.onAction,
        onChatAction = wrappedOnChatAction,
        onSettingsAction = settings.onAction,
        onProfileAction = profile.onAction,
    )
}

@Composable
fun MainNavigationContainer(
    mainState: MainUiState,
    charactersState: CharactersUiState,
    chatState: ChatUiState,
    settingsState: SettingsUiState,
    profileState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onMainAction: (MainAction) -> Unit,
    onCharacterAction: (CharacterAction) -> Unit,
    onChatAction: (ChatAction) -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    onProfileAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.starRailColors
    val currentRoute = mainState.backStack.lastOrNull() ?: Route.ChatSession

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
            val showRail = expanded && mainState.backStack.size <= 1
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
                        mainState = mainState,
                        chatState = chatState,
                        showNavigationBar = !showRail,
                        compact = compact,
                        onMainAction = onMainAction,
                        onChatAction = onChatAction,
                        onRecordingStateChanged = { recording, cancelTargeted ->
                            isRecording = recording
                            isCancelTargeted = cancelTargeted
                        }
                    )
                },
            ) { contentPadding ->
                val entryProvider = entryProvider<Route> {
                    entry<Route.ChatSession> {
                        ChatSessionScreen(
                            state = chatState,
                            charactersState = charactersState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onAction = onChatAction,
                            onCharacterAction = onCharacterAction,
                            onMainAction = onMainAction,
                            isRecording = isRecording,
                            isCancelTargeted = isCancelTargeted,
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
                        CharacterEditScreen(
                            characterId = entry.characterId,
                            state = charactersState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onAction = onCharacterAction,
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
                        ApiSettingsScreen(
                            state = settingsState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onSettingsAction = onSettingsAction,
                        )
                    }
                    entry<Route.MultimodalApiSettings> {
                        ApiSettingsScreen(
                            state = settingsState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onSettingsAction = onSettingsAction,
                            isMultimodal = true,
                        )
                    }
                    entry<Route.VoiceApiSettings> {
                        ApiSettingsScreen(
                            state = settingsState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onSettingsAction = onSettingsAction,
                            isVoice = true,
                        )
                    }
                    entry<Route.ImageGenerationApiSettings> {
                        ApiSettingsScreen(
                            state = settingsState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onSettingsAction = onSettingsAction,
                            isImageGeneration = true,
                        )
                    }
                    entry<Route.Profile> {
                        ProfileScreen(
                            state = profileState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onAction = onProfileAction,
                        )
                    }
                    entry<Route.About> {
                        AboutScreen(
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                        )
                    }
                    entry<Route.CharacterChat> { entry ->
                        CharacterChatScreen(
                            characterId = entry.characterId,
                            state = chatState,
                            charactersState = charactersState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onAction = onChatAction,
                            onCharacterAction = onCharacterAction,
                            onMainAction = onMainAction,
                        )
                    }
                }

                NavDisplay(
                    backstack = mainState.backStack,
                    entryProvider = entryProvider
                )
            }
        }
    }
}

@Composable
private fun MainBottomArea(
    mainState: MainUiState,
    chatState: ChatUiState,
    showNavigationBar: Boolean,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    onChatAction: (ChatAction) -> Unit,
    onRecordingStateChanged: (Boolean, Boolean) -> Unit = { _, _ -> },
) {
    if (mainState.backStack.size > 1) {
        return
    }
    val currentRoute = mainState.backStack.lastOrNull() ?: Route.ChatSession
    val isChat = currentRoute == Route.ChatSession
    if (!isChat && !showNavigationBar) {
        return
    }

    Column {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.97f),
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                if (isChat) {
                    ChatSessionBottomBar(
                        state = chatState,
                        compact = compact,
                        onAction = onChatAction,
                        onRecordingStateChanged = onRecordingStateChanged,
                    )
                }
                if (showNavigationBar && mainState.backStack.size <= 1) {
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
    StarRailTheme(darkThemeOverride = false) {
        MainRoute(
            main = MainRouteBinding(
                state = MainUiState(),
                effects = emptyFlow(),
                onAction = {},
            ),
            characters = CharactersRouteBinding(
                state = previewCharactersState,
                effects = emptyFlow(),
                onAction = {},
            ),
            chat = ChatRouteBinding(
                state = previewChatState,
                effects = emptyFlow(),
                onAction = {},
            ),
            settings = SettingsRouteBinding(
                state = SettingsUiState(),
                effects = emptyFlow(),
                onAction = {},
            ),
            profile = ProfileRouteBinding(
                state = ProfileUiState(),
                effects = emptyFlow(),
                onAction = {},
            ),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun MainContainerDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        MainRoute(
            main = MainRouteBinding(
                state = MainUiState(darkThemeOverride = true),
                effects = emptyFlow(),
                onAction = {},
            ),
            characters = CharactersRouteBinding(
                state = previewCharactersState,
                effects = emptyFlow(),
                onAction = {},
            ),
            chat = ChatRouteBinding(
                state = previewChatState,
                effects = emptyFlow(),
                onAction = {},
            ),
            settings = SettingsRouteBinding(
                state = SettingsUiState(),
                effects = emptyFlow(),
                onAction = {},
            ),
            profile = ProfileRouteBinding(
                state = ProfileUiState(),
                effects = emptyFlow(),
                onAction = {},
            ),
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
    characters = listOf(previewCharacter),
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
