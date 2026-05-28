package com.example.eldercareapp.voice

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class VoiceRecorder {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun start(context: Context): File {
        cancel()

        val voiceDir = File(context.cacheDir, "voice")
        voiceDir.mkdirs()
        val file = File.createTempFile("voice_", ".m4a", voiceDir)

        val mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        val file = outputFile
        val currentRecorder = recorder
        recorder = null
        outputFile = null

        return try {
            currentRecorder?.stop()
            currentRecorder?.release()
            file?.takeIf { it.exists() && it.length() > 0L }
        } catch (_: RuntimeException) {
            currentRecorder?.release()
            file?.delete()
            null
        }
    }

    fun cancel() {
        val currentRecorder = recorder
        val file = outputFile
        recorder = null
        outputFile = null

        try {
            currentRecorder?.stop()
        } catch (_: RuntimeException) {
            // Ignore invalid recorder state when canceling a very short recording.
        } finally {
            currentRecorder?.release()
            file?.delete()
        }
    }
}
