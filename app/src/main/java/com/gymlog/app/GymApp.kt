package com.gymlog.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import com.gymlog.app.ai.Coach
import com.gymlog.app.data.AppDatabase
import com.gymlog.app.data.Repository
import com.gymlog.app.data.Settings
import com.gymlog.app.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GymApp : Application() {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: Settings
        private set
    lateinit var repo: Repository
        private set
    lateinit var restTimer: RestTimer
        private set
    lateinit var coach: Coach
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "gymlog.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        settings = Settings(this)
        I18n.apply(settings.current.language)
        repo = Repository(db, settings)
        restTimer = RestTimer(this, appScope)
        coach = Coach(repo, settings, appScope)
        createChannel()
        appScope.launch {
            val v = repo.syncCatalog(settings.current.catalogVersion)
            settings.update { it.copy(catalogVersion = v) }
        }
    }

    /** (Re)cria o canal de notificações com os textos da língua atual. */
    fun createChannel() {
        val s = I18n.current
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(RestTimer.CHANNEL, s.restChannelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = s.restChannelDesc
                enableVibration(true)
            }
        )
    }

    fun setLanguage(code: String) {
        settings.update { it.copy(language = code) }
        I18n.apply(code)
        createChannel()
    }
}

val Context.app: GymApp get() = applicationContext as GymApp
