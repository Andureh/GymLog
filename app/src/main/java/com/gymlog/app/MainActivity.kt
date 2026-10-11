package com.gymlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import com.gymlog.app.i18n.I18n
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.ui.AppRoot
import com.gymlog.app.ui.theme.GymTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Volta a resolver a língua (o idioma do sistema pode ter mudado)
        I18n.apply(app.settings.current.language)
        setContent {
            CompositionLocalProvider(LocalStrings provides I18n.current) {
                GymTheme {
                    AppRoot()
                }
            }
        }
    }
}
