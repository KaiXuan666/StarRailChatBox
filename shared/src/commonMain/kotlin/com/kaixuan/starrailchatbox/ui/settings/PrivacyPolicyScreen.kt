package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaixuan.starrailchatbox.design.StarRailSpacing
import com.kaixuan.starrailchatbox.platform.openUri
import com.kaixuan.starrailchatbox.ui.components.BackHandler
import com.kaixuan.starrailchatbox.ui.components.StarRailPageLayout
import com.kaixuan.starrailchatbox.ui.main.MainAction
import org.jetbrains.compose.resources.stringResource
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.navigation_back
import starrailchatbox.shared.generated.resources.privacy_policy_contact_email
import starrailchatbox.shared.generated.resources.privacy_policy_github_link
import starrailchatbox.shared.generated.resources.privacy_policy_intro
import starrailchatbox.shared.generated.resources.privacy_policy_section_1_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_1_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_2_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_2_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_3_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_3_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_4_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_4_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_5_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_5_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_6_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_6_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_7_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_7_title
import starrailchatbox.shared.generated.resources.privacy_policy_section_umeng_content
import starrailchatbox.shared.generated.resources.privacy_policy_section_umeng_link
import starrailchatbox.shared.generated.resources.privacy_policy_section_umeng_title
import starrailchatbox.shared.generated.resources.privacy_policy_subtitle
import starrailchatbox.shared.generated.resources.settings_privacy_title

@Composable
fun PrivacyPolicyScreen(
    contentPadding: PaddingValues,
    compact: Boolean,
    onMainAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        onMainAction(MainAction.PopBackStack)
    }

    StarRailPageLayout(
        title = stringResource(Res.string.settings_privacy_title),
        contentPadding = contentPadding,
        compact = compact,
        backContentDescription = stringResource(Res.string.navigation_back),
        onBackClick = { onMainAction(MainAction.PopBackStack) },
        contentSpacing = StarRailSpacing.md,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = StarRailSpacing.md, vertical = StarRailSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(StarRailSpacing.md)
            ) {
                // Subtitle
                Text(
                    text = stringResource(Res.string.privacy_policy_subtitle),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Intro
                Text(
                    text = stringResource(Res.string.privacy_policy_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                // Sections 1-2
                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_1_title),
                    content = stringResource(Res.string.privacy_policy_section_1_content)
                )

                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_2_title),
                    content = stringResource(Res.string.privacy_policy_section_2_content)
                )

                // UMeng Section
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.privacy_policy_section_umeng_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    )
                    Text(
                        text = stringResource(Res.string.privacy_policy_section_umeng_content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                    val umengLinkText = stringResource(Res.string.privacy_policy_section_umeng_link)
                    val annotatedUmeng = buildAnnotatedString {
                        pushStringAnnotation(tag = "URL", annotation = "https://www.umeng.com/page/policy")
                        withStyle(style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontSize = 14.sp
                        )) {
                            append(umengLinkText)
                        }
                        pop()
                    }
                    ClickableText(
                        text = annotatedUmeng,
                        onClick = { offset ->
                            annotatedUmeng.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { openUri(it.item) }
                        }
                    )
                }

                // Remaining Sections
                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_3_title),
                    content = stringResource(Res.string.privacy_policy_section_3_content)
                )

                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_4_title),
                    content = stringResource(Res.string.privacy_policy_section_4_content)
                )

                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_5_title),
                    content = stringResource(Res.string.privacy_policy_section_5_content)
                )

                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_6_title),
                    content = stringResource(Res.string.privacy_policy_section_6_content)
                )

                PrivacySection(
                    title = stringResource(Res.string.privacy_policy_section_7_title),
                    content = stringResource(Res.string.privacy_policy_section_7_content)
                )

                // Footer with Links
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val emailStr = stringResource(Res.string.privacy_policy_contact_email)
                    val githubStr = stringResource(Res.string.privacy_policy_github_link)

                    val annotatedEmail = buildAnnotatedString {
                        pushStringAnnotation(tag = "URL", annotation = "mailto:kaixuanapp@163.com")
                        withStyle(style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontSize = 13.sp
                        )) {
                            append(emailStr)
                        }
                        pop()
                    }

                    val annotatedGithub = buildAnnotatedString {
                        pushStringAnnotation(tag = "URL", annotation = "https://github.com/KaiXuan666/StarRailChatBox")
                        withStyle(style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontSize = 13.sp
                        )) {
                            append(githubStr)
                        }
                        pop()
                    }

                    ClickableText(
                        text = annotatedEmail,
                        onClick = { offset ->
                            annotatedEmail.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { openUri(it.item) }
                        }
                    )

                    ClickableText(
                        text = annotatedGithub,
                        onClick = { offset ->
                            annotatedGithub.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { openUri(it.item) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, content: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        )
    }
}
