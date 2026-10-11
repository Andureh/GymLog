package com.gymlog.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

fun Context.vibrate(pattern: LongArray) {
    val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
    if (v?.hasVibrator() == true) v.vibrate(VibrationEffect.createWaveform(pattern, -1))
}

fun Context.vibrateCelebrate() = vibrate(longArrayOf(0, 80, 60, 80, 60, 250))
