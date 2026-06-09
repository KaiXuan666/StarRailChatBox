package com.kaixuan.starrailchatbox.platform

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

class AndroidAudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTime: Long = 0L

    fun start() {
        try {
            releaseRecorder()
            
            // 直接将音频文件保存至 app 私有的 chat_attachments 目录下，免去二次拷贝
            val outputDir = File(context.filesDir, "chat_attachments")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val file = File(outputDir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = file
            startTime = System.currentTimeMillis()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            currentFile?.delete()
            currentFile = null
        }
    }

    fun stop(cancel: Boolean): RecordResult? {
        val duration = System.currentTimeMillis() - startTime
        val file = currentFile
        
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // 如果录音时间过短或出错，stop 会抛出异常
            e.printStackTrace()
            releaseRecorder()
            file?.delete()
            currentFile = null
            return null
        } finally {
            releaseRecorder()
        }

        currentFile = null

        if (cancel || file == null || !file.exists()) {
            file?.delete()
            return null
        }

        // 如果录音时间过短（小于800毫秒），视为无效录音，静默删除并返回 null
        if (duration < 800) {
            file.delete()
            return null
        }

        // 修复 ftyp major brand。Android MediaRecorder 默认输出 mp42 容器标识，
        // 导致小米等服务端的音频格式校验器判定为无效音频。在此将 mp42 修改为标准的 M4A 标识。
        try {
            java.io.RandomAccessFile(file, "rw").use { raf ->
                if (raf.length() >= 12) {
                    raf.seek(8)
                    val brand = ByteArray(4)
                    raf.readFully(brand)
                    if (brand[0] == 'm'.toByte() && 
                        brand[1] == 'p'.toByte() && 
                        brand[2] == '4'.toByte() && 
                        brand[3] == '2'.toByte()) {
                        raf.seek(8)
                        raf.write(byteArrayOf('M'.toByte(), '4'.toByte(), 'A'.toByte(), ' '.toByte()))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return RecordResult(
            uri = file.absolutePath,
            durationMs = duration
        )
    }

    private fun releaseRecorder() {
        mediaRecorder?.apply {
            try {
                reset()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
    }
}


@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    val recorder = remember(context) { AndroidAudioRecorder(context) }

    return remember(recorder) {
        object : AudioRecorder {
            override fun startRecording() {
                recorder.start()
            }

            override fun stopRecording(cancel: Boolean): RecordResult? {
                return recorder.stop(cancel)
            }
        }
    }
}
