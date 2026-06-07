package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

/**
 * 跨平台选取图片的 Composable 工具。
 * 返回一个 launch 函数，调用后会启动系统的图片选择器。
 * 成功选择后，回调 [onImagePicked] 并返回所选图片的 ByteArray 数据。
 */
@Composable
expect fun rememberImagePicker(onImagePicked: (ByteArray?) -> Unit): () -> Unit
