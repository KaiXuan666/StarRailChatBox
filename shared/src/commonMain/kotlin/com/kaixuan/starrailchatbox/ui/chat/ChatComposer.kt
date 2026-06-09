package com.kaixuan.starrailchatbox.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.platform.rememberAudioRecorder
import com.kaixuan.starrailchatbox.platform.rememberCameraLauncher
import com.kaixuan.starrailchatbox.platform.rememberFilePicker
import com.kaixuan.starrailchatbox.platform.rememberImagePicker
import com.kaixuan.starrailchatbox.ui.components.StarRailIcon
import com.kaixuan.starrailchatbox.ui.components.StarRailIconKind
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.add_attachment
import starrailchatbox.shared.generated.resources.hold_to_speak
import starrailchatbox.shared.generated.resources.message_placeholder
import starrailchatbox.shared.generated.resources.nav_chat
import starrailchatbox.shared.generated.resources.record_voice
import starrailchatbox.shared.generated.resources.send_message
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

@Composable
fun ChatSessionBottomBar(
    state: ChatUiState,
    compact: Boolean,
    onAction: (ChatAction) -> Unit,
    onRecordingStateChanged: (isRecording: Boolean, isCancelTargeted: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val characterId = state.selectedCharacterId
    val isVoiceMode = characterId?.let { state.characterStates[it]?.isVoiceMode } ?: false
    val coroutineScope = rememberCoroutineScope()

    val imagePicker = rememberImagePicker { picked ->
        picked?.let { 
            coroutineScope.launch {
                val compressedUri = com.kaixuan.starrailchatbox.platform.compressImageIfPossible(it.uri)
                onAction(ChatAction.ImageSelected(compressedUri, it.name))
            }
        }
    }
    val filePicker = rememberFilePicker { picked ->
        picked?.let { onAction(ChatAction.FileSelected(it.uri, it.name)) }
    }
    val cameraLauncher = rememberCameraLauncher { picked ->
        picked?.let { 
            coroutineScope.launch {
                val compressedUri = com.kaixuan.starrailchatbox.platform.compressImageIfPossible(it.uri)
                onAction(ChatAction.ImageSelected(compressedUri, it.name))
            }
        }
    }

    val interceptedOnAction: (ChatAction) -> Unit = { action ->
        if (action is ChatAction.ComposerActionClicked) {
            when (action.action) {
                ComposerAction.PICK_IMAGE -> imagePicker()
                ComposerAction.PICK_FILE -> filePicker()
                ComposerAction.TAKE_PHOTO -> cameraLauncher()
                else -> onAction(action)
            }
        } else {
            onAction(action)
        }
    }

    Column(modifier = modifier) {
        if (state.selectedAttachments.isNotEmpty()) {
            SelectedAttachmentsArea(
                attachments = state.selectedAttachments,
                compact = compact,
                onAddClicked = { interceptedOnAction(ChatAction.ComposerActionClicked(ComposerAction.ATTACH)) },
                onRemoveClicked = { interceptedOnAction(ChatAction.RemoveAttachment(it)) },
            )
        } else {
            QuickReplies(
                suggestions = state.suggestions,
                compact = compact,
                onReplyClicked = {
                    interceptedOnAction(ChatAction.QuickReplyClicked(it))
                },
            )
        }
        MessageComposer(
            value = state.messageDraft,
            isSending = state.isSending,
            isVoiceMode = isVoiceMode,
            attachments = state.selectedAttachments,
            compact = compact,
            onValueChange = {
                interceptedOnAction(ChatAction.MessageChanged(it))
            },
            onSend = { interceptedOnAction(ChatAction.SendClicked) },
            onComposerAction = {
                interceptedOnAction(ChatAction.ComposerActionClicked(it))
            },
            onRecordingStateChanged = onRecordingStateChanged,
            onVoiceFinished = { uri, duration ->
                interceptedOnAction(ChatAction.VoiceRecordingFinished(uri, duration))
            }
        )
        if (state.isAttachmentPanelVisible) {
            AttachmentPanel(
                compact = compact,
                onAction = { interceptedOnAction(ChatAction.ComposerActionClicked(it)) }
            )
        }
    }
}

@Composable
fun MessageComposer(
    value: String,
    isSending: Boolean,
    isVoiceMode: Boolean,
    attachments: List<SelectedAttachment>,
    compact: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onComposerAction: (ComposerAction) -> Unit,
    onRecordingStateChanged: (Boolean, Boolean) -> Unit,
    onVoiceFinished: (String, Long) -> Unit,
) {
    var dragOffsetY by remember { mutableStateOf(0f) }
    val audioRecorder = rememberAudioRecorder()
    var isRecordingActive by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                end = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                top = if (compact) StarRailSpacing.xs else StarRailSpacing.md,
                bottom = StarRailSpacing.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(StarRailSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComposerIconButton(
            icon = StarRailIconKind.ADD,
            contentDescription = stringResource(Res.string.add_attachment),
            compact = compact,
            onClick = { onComposerAction(ComposerAction.ATTACH) },
        )

        if (isVoiceMode) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 38.dp else 52.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (audioRecorder.hasPermission()) {
                                    dragOffsetY = 0f
                                    isRecordingActive = true
                                    audioRecorder.startRecording()
                                    onRecordingStateChanged(true, false)
                                } else {
                                    isRecordingActive = false
                                    audioRecorder.requestPermission { granted ->
                                        // 权限申请成功，下一次长按便能开始录音
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isRecordingActive) {
                                    isRecordingActive = false
                                    if (dragOffsetY < -160f) {
                                        audioRecorder.stopRecording(cancel = true)
                                        onRecordingStateChanged(false, false)
                                    } else {
                                        onRecordingStateChanged(false, false)
                                        val result = audioRecorder.stopRecording(cancel = false)
                                        if (result != null) {
                                            onVoiceFinished(result.uri, result.durationMs)
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                if (isRecordingActive) {
                                    isRecordingActive = false
                                    audioRecorder.stopRecording(cancel = true)
                                    onRecordingStateChanged(false, false)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (isRecordingActive) {
                                    dragOffsetY += dragAmount.y
                                    onRecordingStateChanged(true, dragOffsetY < -160f)
                                }
                            }
                        )
                    },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.hold_to_speak),
                        style = if (compact) {
                            MaterialTheme.typography.labelLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 0.dp, minHeight = if (compact) 38.dp else 52.dp)
                    .animateContentSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                minLines = 1,
                maxLines = 4,
                decorationBox = { innerTextField ->
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.message_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }

        if (!isVoiceMode && (value.isNotBlank() || attachments.isNotEmpty() || isSending)) {
            ComposerIconButton(
                icon = StarRailIconKind.SEND,
                contentDescription = stringResource(Res.string.send_message),
                compact = compact,
                enabled = !isSending,
                primary = true,
                onClick = onSend,
                loading = isSending,
            )
        }

        ComposerIconButton(
            icon = if (isVoiceMode) StarRailIconKind.KEYBOARD else StarRailIconKind.MICROPHONE,
            contentDescription = stringResource(if (isVoiceMode) Res.string.nav_chat else Res.string.record_voice),
            compact = compact,
            onClick = { onComposerAction(ComposerAction.VOICE) },
        )
    }
}

@Composable
fun QuickReplies(
    suggestions: List<String>,
    compact: Boolean,
    onReplyClicked: (String) -> Unit,
) {
    if (suggestions.isEmpty()) return

    // 限制最多展示 4 个 suggestions，并将其分为最多 2 行（每行最多 2 个）
    val chunkedSuggestions = suggestions.take(4).chunked(2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) StarRailSpacing.sm else StarRailSpacing.md,
                vertical = if (compact) StarRailSpacing.xxs else StarRailSpacing.xs,
            ),
        verticalArrangement = Arrangement.spacedBy(
            if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
        ),
    ) {
        chunkedSuggestions.forEach { rowSuggestions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 32.dp else 40.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    if (compact) StarRailSpacing.xxs else StarRailSpacing.xs
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowSuggestions.forEach { suggestion ->
                    Surface(
                        onClick = { onReplyClicked(suggestion) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                        ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (compact) StarRailSpacing.xs else StarRailSpacing.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = suggestion,
                                style = if (compact) {
                                    MaterialTheme.typography.labelMedium
                                } else {
                                    MaterialTheme.typography.labelLarge
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // 如果行元素少于 2 个，则在右侧填充一个 weight(1f) 的 Spacer，保证第一个元素只占一半宽度
                if (rowSuggestions.size < 2) {
                    Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

@Composable
fun ComposerIconButton(
    icon: StarRailIconKind,
    contentDescription: String,
    compact: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    loading: Boolean = false,
) {
    val containerColor = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    val contentColor = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(if (compact) 38.dp else 52.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        border = if (primary) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        shadowElevation = if (primary && enabled) 5.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 18.dp else 22.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                StarRailIcon(
                    kind = icon,
                    contentDescription = contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(if (compact) 20.dp else 24.dp),
                )
            }
        }
    }
}
