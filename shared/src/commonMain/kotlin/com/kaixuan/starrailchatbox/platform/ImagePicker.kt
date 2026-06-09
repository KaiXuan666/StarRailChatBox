package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

data class PickedImage(
    val uri: String,
    val name: String? = null,
)

/**
 * 跨平台选取图片的 Composable 工具。
 * 返回一个 launch 函数，调用后会启动系统的图片选择器。
 * 成功选择后，回调 [onImagePicked] 并返回可由平台存储层复制的图片 URI。
 */
@Composable
expect fun rememberImagePicker(onImagePicked: (PickedImage?) -> Unit): () -> Unit
