package com.kaixuan.starrailchatbox.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.attach_not_ready
import starrailchatbox.shared.generated.resources.emoji_not_ready
import starrailchatbox.shared.generated.resources.microphone_not_ready
import starrailchatbox.shared.generated.resources.nav_characters
import starrailchatbox.shared.generated.resources.nav_chat
import starrailchatbox.shared.generated.resources.nav_settings
import starrailchatbox.shared.generated.resources.profile_nickname_empty
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
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.design.starRailColors
import com.kaixuan.starrailchatbox.ui.chat.ChatAction
import com.kaixuan.starrailchatbox.ui.chat.ChatEffect
import com.kaixuan.starrailchatbox.ui.chat.ChatSessionBottomBar
import com.kaixuan.starrailchatbox.ui.chat.ChatSessionScreen
import com.kaixuan.starrailchatbox.ui.chat.ChatUiState
import com.kaixuan.starrailchatbox.ui.chat.ConversationManagementScreen
import com.kaixuan.starrailchatbox.ui.chat.EffectMessage
import com.kaixuan.starrailchatbox.ui.components.NavigationPlaceholderScreen
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import com.kaixuan.starrailchatbox.ui.navigation.NavDisplay
import com.kaixuan.starrailchatbox.ui.navigation.Route
import com.kaixuan.starrailchatbox.ui.navigation.entryProvider
import com.kaixuan.starrailchatbox.ui.settings.ApiSettingsScreen
import com.kaixuan.starrailchatbox.ui.settings.SettingsScreen
import com.kaixuan.starrailchatbox.ui.settings.SettingsAction
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffect
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffectMessage
import com.kaixuan.starrailchatbox.ui.settings.SettingsUiState
import com.kaixuan.starrailchatbox.ui.profile.ProfileUiState
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffect
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffectMessage
import com.kaixuan.starrailchatbox.ui.profile.ProfileAction
import com.kaixuan.starrailchatbox.ui.profile.ProfileScreen

@Composable
fun MainRoute(
    main: MainRouteBinding,
    chat: ChatRouteBinding,
    settings: SettingsRouteBinding,
    profile: ProfileRouteBinding,
) {
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
    )
    val mainEffectMessages = mapOf(
        MainEffectMessage.THEME_CHANGED to stringResource(Res.string.theme_changed),
    )

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

    LaunchedEffect(settings.effects, settingsEffectMessages) {
        settings.effects.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        settingsEffectMessages.getValue(effect.message),
                    )
                }
                SettingsEffect.ApiSettingsSaved -> {
                    snackbarHostState.showSnackbar(
                        settingsEffectMessages.getValue(SettingsEffectMessage.SETTINGS_API_SAVED),
                    )
                    main.onAction(MainAction.PopBackStack)
                }
            }
        }
    }

    val profileEffectMessages = mapOf(
        ProfileEffectMessage.PROFILE_SAVED to stringResource(Res.string.profile_saved),
        ProfileEffectMessage.NICKNAME_EMPTY to stringResource(Res.string.profile_nickname_empty),
    )

    LaunchedEffect(profile.effects) {
        profile.effects.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        profileEffectMessages.getValue(effect.message),
                    )
                }
                ProfileEffect.ProfileSaved -> {
                    main.onAction(MainAction.PopBackStack)
                }
            }
        }
    }

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

    MainNavigationContainer(
        mainState = main.state,
        chatState = chat.state,
        settingsState = settings.state,
        profileState = profile.state,
        snackbarHostState = snackbarHostState,
        onMainAction = main.onAction,
        onChatAction = chat.onAction,
        onSettingsAction = settings.onAction,
        onProfileAction = profile.onAction,
    )
}

@Composable
fun MainNavigationContainer(
    mainState: MainUiState,
    chatState: ChatUiState,
    settingsState: SettingsUiState,
    profileState: ProfileUiState,
    snackbarHostState: SnackbarHostState,
    onMainAction: (MainAction) -> Unit,
    onChatAction: (ChatAction) -> Unit,
    onSettingsAction: (SettingsAction) -> Unit,
    onProfileAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.starRailColors
    val currentRoute = mainState.backStack.lastOrNull() ?: Route.ChatSession

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        colors.backgroundGlow.copy(alpha = 0.38f),
                        MaterialTheme.colorScheme.background,
                    ),
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
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    MainBottomArea(
                        mainState = mainState,
                        chatState = chatState,
                        showNavigationBar = !showRail,
                        compact = compact,
                        onMainAction = onMainAction,
                        onChatAction = onChatAction,
                    )
                },
            ) { contentPadding ->
                val entryProvider = entryProvider<Route> {
                    entry<Route.ChatSession> {
                        ChatSessionScreen(
                            state = chatState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onAction = onChatAction,
                            onMainAction = onMainAction,
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
                    entry<Route.Characters> {
                        NavigationPlaceholderScreen(
                            title = stringResource(Res.string.nav_characters),
                            contentPadding = contentPadding,
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
                    entry<Route.Profile> {
                        ProfileScreen(
                            state = profileState,
                            contentPadding = contentPadding,
                            compact = compact,
                            onMainAction = onMainAction,
                            onAction = onProfileAction,
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
                Route.Settings -> currentRoute == Route.Settings || currentRoute == Route.ApiSettings
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
                Route.Settings -> currentRoute == Route.Settings || currentRoute == Route.ApiSettings
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
private fun StarfieldBackground() {
    val colors = MaterialTheme.starRailColors
    Canvas(Modifier.fillMaxSize()) {
        val thinStroke = Stroke(width = 1.dp.toPx())
        drawOval(
            color = colors.constellationMuted.copy(alpha = 0.45f),
            topLeft = Offset(size.width * 0.62f, size.height * 0.02f),
            size = Size(size.width * 0.48f, size.height * 0.18f),
            style = thinStroke,
        )
        drawOval(
            color = colors.constellationMuted.copy(alpha = 0.38f),
            topLeft = Offset(size.width * 0.68f, size.height * 0.045f),
            size = Size(size.width * 0.34f, size.height * 0.12f),
            style = thinStroke,
        )
        val stars = listOf(
            0.08f to 0.26f,
            0.93f to 0.31f,
            0.81f to 0.52f,
            0.12f to 0.72f,
            0.9f to 0.84f,
            0.46f to 0.18f,
        )
        stars.forEachIndexed { index, (x, y) ->
            val radius = if (index % 2 == 0) 2.2.dp.toPx() else 1.4.dp.toPx()
            drawCircle(
                color = colors.constellation.copy(alpha = 0.58f),
                radius = radius,
                center = Offset(size.width * x, size.height * y),
            )
        }
        drawDecorativeSparkle(
            center = Offset(size.width * 0.84f, size.height * 0.42f),
            radius = 12.dp.toPx(),
            color = colors.constellationMuted.copy(alpha = 0.64f),
        )
        drawDecorativeSparkle(
            center = Offset(size.width * 0.17f, size.height * 0.62f),
            radius = 8.dp.toPx(),
            color = colors.constellation.copy(alpha = 0.42f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDecorativeSparkle(
    center: Offset,
    radius: Float,
    color: Color,
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.2f, center.y - radius * 0.2f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.2f, center.y + radius * 0.2f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.2f, center.y + radius * 0.2f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.2f, center.y - radius * 0.2f)
        close()
    }
    drawPath(path, color)
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
            chat = ChatRouteBinding(
                state = ChatUiState(),
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
            chat = ChatRouteBinding(
                state = ChatUiState(),
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
