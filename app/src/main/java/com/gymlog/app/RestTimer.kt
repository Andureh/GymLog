package com.gymlog.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.gymlog.app.i18n.I18n
import com.gymlog.app.util.vibrate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Temporizador de descanso global: sobrevive à navegação e avisa por vibração + notificação. */
class RestTimer(private val ctx: Context, private val scope: CoroutineScope) {
    data class State(val endAt: Long, val total: Int)

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state
    private var job: Job? = null

    fun start(seconds: Int) {
        if (seconds > 0) schedule(State(System.currentTimeMillis() + seconds * 1000L, seconds))
    }

    fun add(deltaSec: Int) {
        val s = _state.value ?: return
        val end = s.endAt + deltaSec * 1000L
        if (end <= System.currentTimeMillis()) stop() else schedule(State(end, (s.total + deltaSec).coerceAtLeast(1)))
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = null
    }

    private fun schedule(s: State) {
        job?.cancel()
        _state.value = s
        job = scope.launch {
            delay(s.endAt - System.currentTimeMillis())
            _state.value = null
            finished()
        }
    }

    private fun finished() {
        ctx.vibrate(longArrayOf(0, 400, 200, 400))
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(I18n.current.restOverTitle)
            .setContentText(I18n.current.restOverText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(pi)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, n)
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val CHANNEL = "rest_timer"
        private const val NOTIFICATION_ID = 1001
    }
}
