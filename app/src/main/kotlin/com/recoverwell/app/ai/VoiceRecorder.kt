package com.recoverwell.app.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Thin MediaRecorder wrapper that captures a short spoken check-in to a small
 * AAC/m4a file in the app cache. The file is transcribed then deleted - audio
 * never persists. One recording at a time.
 */
class VoiceRecorder(private val ctx: Context) {

    private var recorder: MediaRecorder? = null
    var outputFile: File? = null
        private set

    val isRecording: Boolean get() = recorder != null

    /** Begin recording. Returns false if the mic/encoder couldn't be set up. */
    fun start(): Boolean {
        if (recorder != null) return true
        val file = File(ctx.cacheDir, "journal_${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()
        return try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16000)
            r.setAudioEncodingBitRate(64000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            outputFile = file
            true
        } catch (e: Exception) {
            try { r.release() } catch (ignore: Exception) {}
            file.delete()
            false
        }
    }

    /** Stop and return the recorded file, or null if it failed (file cleaned up). */
    fun stop(): File? {
        val r = recorder ?: return null
        recorder = null
        return try {
            r.stop()
            r.release()
            outputFile
        } catch (e: Exception) {
            r.release()
            outputFile?.delete()
            outputFile = null
            null
        }
    }

    /** Abandon a recording in progress and delete its file. */
    fun cancel() {
        val r = recorder ?: return
        recorder = null
        try { r.stop() } catch (ignore: Exception) {}
        r.release()
        outputFile?.delete()
        outputFile = null
    }
}
