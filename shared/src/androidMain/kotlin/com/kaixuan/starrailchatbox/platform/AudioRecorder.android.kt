package com.kaixuan.starrailchatbox.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class AndroidAudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var currentFile: File? = null
    private var startTime: Long = 0L
    private var recordThread: Thread? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    @SuppressLint("MissingPermission")
    fun start() {
        try {
            releaseRecorder()
            
            // 直接将音频文件保存至 app 私有的 chat_attachments 目录下，免去二次拷贝
            val outputDir = File(context.filesDir, "chat_attachments")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val file = File(outputDir, "voice_${System.currentTimeMillis()}.wav")
            currentFile = file
            startTime = System.currentTimeMillis()

            val recordSize = if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                sampleRate * 2
            } else {
                bufferSize * 2
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                recordSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                releaseRecorder()
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordThread = thread(start = true) {
                writeAudioDataToFile(file, recordSize)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            currentFile?.delete()
            currentFile = null
        }
    }

    private fun writeAudioDataToFile(file: File, recordSize: Int) {
        val data = ByteArray(recordSize)
        var os: FileOutputStream? = null
        try {
            os = FileOutputStream(file)
            // 先写入 44 字节的空白，以便录音结束后填充 WAV 头部
            val header = ByteArray(44)
            os.write(header)

            while (isRecording) {
                val read = audioRecord?.read(data, 0, recordSize) ?: -1
                if (read > 0) {
                    os.write(data, 0, read)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                os?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop(cancel: Boolean): RecordResult? {
        isRecording = false
        try {
            recordThread?.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recordThread = null

        val duration = System.currentTimeMillis() - startTime
        val file = currentFile
        
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
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

        // 写入正式的 WAV 头部
        val totalAudioLen = file.length() - 44
        if (totalAudioLen > 0) {
            writeWavHeader(file, totalAudioLen)
        } else {
            file.delete()
            return null
        }

        return RecordResult(
            uri = file.absolutePath,
            durationMs = duration
        )
    }

    private fun releaseRecorder() {
        isRecording = false
        audioRecord?.apply {
            try {
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        audioRecord = null
    }

    private fun writeWavHeader(file: File, totalAudioLen: Long) {
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = sampleRate.toLong()
        val channels = 1
        val byteRate = longSampleRate * channels * 2 // 16bit = 2bytes

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xff).toByte()
        header[25] = ((longSampleRate shr 8) and 0xff).toByte()
        header[26] = ((longSampleRate shr 16) and 0xff).toByte()
        header[27] = ((longSampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(file, "rw")
            raf.seek(0)
            raf.write(header)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                raf?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}


@Composable
actual fun rememberAudioRecorder(): AudioRecorder {
    val context = LocalContext.current
    var hasPermissionState by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var onPermissionResultCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermissionState = isGranted
        onPermissionResultCallback?.invoke(isGranted)
    }

    val recorder = remember(context) { AndroidAudioRecorder(context) }

    return remember(context, hasPermissionState) {
        object : AudioRecorder {
            override fun hasPermission(): Boolean {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted != hasPermissionState) {
                    hasPermissionState = granted
                }
                return granted
            }

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                onPermissionResultCallback = onResult
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            override fun startRecording() {
                recorder.start()
            }

            override fun stopRecording(cancel: Boolean): RecordResult? {
                return recorder.stop(cancel)
            }
        }
    }
}
