package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedImage?) -> Unit): () -> Unit {
    return {
        val document = kotlinx.browser.document
        val input = document.createElement("input") as org.w3c.dom.HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.onchange = {
            val files = input.files
            if (files != null && files.length > 0) {
                val file = files.item(0)!!
                val reader = org.w3c.files.FileReader()
                reader.onload = {
                    onImagePicked(PickedImage(reader.result as String))
                }
                reader.readAsDataURL(file)
            } else {
                onImagePicked(null)
            }
        }
        input.click()
    }
}
