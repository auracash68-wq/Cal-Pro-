package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sin

object SoundManager {
    private const val TAG = "SoundManager"
    private var soundPool: SoundPool? = null
    
    private var soundTapId = -1
    private var soundSuccessId = -1
    private var soundSaveId = -1
    private var soundRefreshId = -1
    private var soundSearchId = -1
    private var soundErrorId = -1
    
    var soundEnabled = true

    fun init(context: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build()

        try {
            val cacheDir = context.cacheDir
            
            // 1. Soft Tap (short 1000Hz sine decay, 0.05s)
            val fileTap = File(cacheDir, "tap.wav")
            generateWav(fileTap, 1000.0, 0.05, 0.1)
            
            // 2. Success (chime: dual note, 880Hz then 1320Hz, 0.25s)
            val fileSuccess = File(cacheDir, "success.wav")
            generateSuccessWav(fileSuccess)
            
            // 3. Save (upward slide: 600Hz to 1200Hz, 0.2s)
            val fileSave = File(cacheDir, "save.wav")
            generateSlideWav(fileSave, 600.0, 1200.0, 0.2)
            
            // 4. Refresh (two quick blips: 800Hz, 0.15s)
            val fileRefresh = File(cacheDir, "refresh.wav")
            generateDoubleBlipWav(fileRefresh)
            
            // 5. Search (quick elegant sweep: 900Hz to 1500Hz, 0.12s)
            val fileSearch = File(cacheDir, "search.wav")
            generateSlideWav(fileSearch, 900.0, 1500.0, 0.12)
            
            // 6. Error (low buzzer sound: 150Hz square wave, 0.3s)
            val fileError = File(cacheDir, "error.wav")
            generateBuzzerWav(fileError)
            
            soundPool?.let { pool ->
                soundTapId = pool.load(fileTap.absolutePath, 1)
                soundSuccessId = pool.load(fileSuccess.absolutePath, 1)
                soundSaveId = pool.load(fileSave.absolutePath, 1)
                soundRefreshId = pool.load(fileRefresh.absolutePath, 1)
                soundSearchId = pool.load(fileSearch.absolutePath, 1)
                soundErrorId = pool.load(fileError.absolutePath, 1)
            }
            
            val prefs = context.getSharedPreferences("calpro_settings", Context.MODE_PRIVATE)
            soundEnabled = prefs.getBoolean("sound_effects_enabled", true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize custom sound assets", e)
        }
    }

    fun playTap() {
        playSound(soundTapId)
    }

    fun playSuccess() {
        playSound(soundSuccessId)
    }

    fun playSave() {
        playSound(soundSaveId)
    }

    fun playRefresh() {
        playSound(soundRefreshId)
    }

    fun playSearch() {
        playSound(soundSearchId)
    }

    fun playError() {
        playSound(soundErrorId)
    }

    private fun playSound(soundID: Int) {
        if (!soundEnabled || soundID == -1) return
        // Volume 15% - 20%
        soundPool?.play(soundID, 0.18f, 0.18f, 1, 0, 1.0f)
    }

    fun toggleSound(context: Context, enabled: Boolean) {
        soundEnabled = enabled
        val prefs = context.getSharedPreferences("calpro_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("sound_effects_enabled", enabled).apply()
    }

    private fun generateWav(file: File, freq: Double, duration: Double, maxAmpMult: Double) {
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val dataSize = numSamples * 2
        val header = createWavHeader(dataSize)
        
        val pcm = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, pcm, 0, 44)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = Math.exp(-t * 25.0) * maxAmpMult
            val s = sin(2 * Math.PI * freq * t) * envelope
            val value = (s * 32767).toInt().toShort()
            
            val offset = 44 + i * 2
            pcm[offset] = (value.toInt() and 0x00FF).toByte()
            pcm[offset + 1] = ((value.toInt() and 0xFF00) ushr 8).toByte()
        }
        
        try {
            FileOutputStream(file).use { it.write(pcm) }
        } catch (e: IOException) {
            Log.e(TAG, "Wav write failed", e)
        }
    }

    private fun generateSuccessWav(file: File) {
        val sampleRate = 44100
        val duration = 0.25
        val numSamples = (duration * sampleRate).toInt()
        val dataSize = numSamples * 2
        val header = createWavHeader(dataSize)
        val pcm = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, pcm, 0, 44)
        
        val split = numSamples / 2
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val f = if (i < split) 880.0 else 1320.0
            val env = if (i < split) {
                Math.exp(-t * 15.0)
            } else {
                Math.exp(-(t - split.toDouble() / sampleRate) * 10.0)
            }
            val s = sin(2 * Math.PI * f * t) * env * 0.8
            val value = (s * 32767).toInt().toShort()
            
            val offset = 44 + i * 2
            pcm[offset] = (value.toInt() and 0x00FF).toByte()
            pcm[offset + 1] = ((value.toInt() and 0xFF00) ushr 8).toByte()
        }
        
        try {
            FileOutputStream(file).use { it.write(pcm) }
        } catch (e: Exception) {
            Log.e(TAG, "Success Wav write failed", e)
        }
    }

    private fun generateSlideWav(file: File, freqStart: Double, freqEnd: Double, duration: Double) {
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val dataSize = numSamples * 2
        val header = createWavHeader(dataSize)
        val pcm = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, pcm, 0, 44)
        
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = freqStart + (freqEnd - freqStart) * progress
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-t * 8.0)
            val s = sin(2 * Math.PI * freq * t) * env * 0.6
            val value = (s * 32767).toInt().toShort()
            
            val offset = 44 + i * 2
            pcm[offset] = (value.toInt() and 0x00FF).toByte()
            pcm[offset + 1] = ((value.toInt() and 0xFF00) ushr 8).toByte()
        }
        
        try {
            FileOutputStream(file).use { it.write(pcm) }
        } catch (e: Exception) {
            Log.e(TAG, "Slide Wav write failed", e)
        }
    }

    private fun generateDoubleBlipWav(file: File) {
        val sampleRate = 44100
        val duration = 0.20
        val numSamples = (duration * sampleRate).toInt()
        val dataSize = numSamples * 2
        val header = createWavHeader(dataSize)
        val pcm = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, pcm, 0, 44)
        
        val gapStart = (numSamples * 0.4).toInt()
        val gapEnd = (numSamples * 0.6).toInt()
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val active = (i < gapStart) || (i > gapEnd)
            val s = if (active) {
                val subT = if (i > gapEnd) t - gapEnd.toDouble() / sampleRate else t
                sin(2 * Math.PI * 1100.0 * subT) * Math.exp(-subT * 25.0) * 0.6
            } else {
                0.0
            }
            val value = (s * 32767).toInt().toShort()
            
            val offset = 44 + i * 2
            pcm[offset] = (value.toInt() and 0x00FF).toByte()
            pcm[offset + 1] = ((value.toInt() and 0xFF00) ushr 8).toByte()
        }
        
        try {
            FileOutputStream(file).use { it.write(pcm) }
        } catch (e: Exception) {
            Log.e(TAG, "Double Blip Wav write failed", e)
        }
    }

    private fun generateBuzzerWav(file: File) {
        val sampleRate = 44100
        val duration = 0.28
        val numSamples = (duration * sampleRate).toInt()
        val dataSize = numSamples * 2
        val header = createWavHeader(dataSize)
        val pcm = ByteArray(44 + dataSize)
        System.arraycopy(header, 0, pcm, 0, 44)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val periodInSamples = sampleRate / 160.0
            val phase = (i % periodInSamples) / periodInSamples
            val square = if (phase < 0.5) 0.5 else -0.5
            val env = Math.exp(-t * 10.0)
            val s = square * env * 0.5
            val value = (s * 32767).toInt().toShort()
            
            val offset = 44 + i * 2
            pcm[offset] = (value.toInt() and 0x00FF).toByte()
            pcm[offset + 1] = ((value.toInt() and 0xFF00) ushr 8).toByte()
        }
        
        try {
            FileOutputStream(file).use { it.write(pcm) }
        } catch (e: Exception) {
            Log.e(TAG, "Buzzer Wav write failed", e)
        }
    }

    private fun createWavHeader(dataSize: Int): ByteArray {
        val header = ByteArray(44)
        val totalSize = 36 + dataSize
        
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        
        header[4] = (totalSize and 0xff).toByte()
        header[5] = (totalSize ushr 8 and 0xff).toByte()
        header[6] = (totalSize ushr 16 and 0xff).toByte()
        header[7] = (totalSize ushr 24 and 0xff).toByte()
        
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        
        header[20] = 1
        header[21] = 0
        
        header[22] = 1
        header[23] = 0
        
        val sampleRate = 44100
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate ushr 8 and 0xff).toByte()
        header[26] = (sampleRate ushr 16 and 0xff).toByte()
        header[27] = (sampleRate ushr 24 and 0xff).toByte()
        
        val byteRate = 44100 * 2
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate ushr 8 and 0xff).toByte()
        header[30] = (byteRate ushr 16 and 0xff).toByte()
        header[31] = (byteRate ushr 24 and 0xff).toByte()
        
        header[32] = 2
        header[33] = 0
        
        header[34] = 16
        header[35] = 0
        
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        
        header[40] = (dataSize and 0xff).toByte()
        header[41] = (dataSize ushr 8 and 0xff).toByte()
        header[42] = (dataSize ushr 16 and 0xff).toByte()
        header[43] = (dataSize ushr 24 and 0xff).toByte()
        
        return header
    }
}
