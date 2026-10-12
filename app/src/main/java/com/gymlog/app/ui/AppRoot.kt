package com.gymlog.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gymlog.app.app
import com.gymlog.app.data.WorkoutFull
import com.gymlog.app.i18n.LocalStrings
import com.gymlog.app.i18n.Strings
import com.gymlog.app.ui.components.rememberNow
import com.gymlog.app.ui.screens.BodyWeightScreen
import com.gymlog.app.ui.screens.CoachScreen
import com.gymlog.app.ui.screens.ExerciseDetailScreen
import com.gymlog.app.ui.screens.ExercisesScreen
import com.gymlog.app.ui.screens.HistoryScreen
import com.gymlog.app.ui.screens.HomeScreen
import com.gymlog.app.ui.screens.ProfileScreen
import com.gymlog.app.ui.screens.RoutineEditScreen
import com.gymlog.app.ui.screens.RoutineProgressScreen
import com.gymlog.app.ui.screens.WorkoutDetailScreen
import com.gymlog.app.ui.screens.WorkoutScreen
import com.gymlog.app.util.fmtClock

private class Tab(val route: String, val label: (Strings) -> String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", { it.tabWorkout }, Icons.Default.FitnessCenter),
    Tab("history", { it.tabHistory }, Icons.Default.History),
    Tab("exercises", { it.tabExercises }, Icons.AutoMirrored.Filled.List),
    Tab("profile", { it.tabProfile }, Icons.Default.Person),
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val app = LocalContext.current.app
    val active by remember { app.repo.activeWorkout() }.collectAsStateWithLifecycle(null)
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBottom = tabs.any { it.route == route }
    val s = LocalStrings.current

    NotificationPermission()

    Scaffold(
        bottomBar = {
            if (showBottom) {
                Column {
                    active?.let { ActiveWorkoutBar(it) { nav.navigate("workout/${it.workout.id}") } }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = route == t.route,
                                onClick = { nav.navigateTab(t.route) },
                                icon = { Icon(t.icon, null) },
                                label = { Text(t.label(s)) },
                            )
                        }
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(nav) }
            composable("history") { HistoryScreen(nav) }
            composable("exercises") { ExercisesScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
            composable("workout/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                WorkoutScreen(nav, it.arguments!!.getLong("id"))
            }
            composable(
                "workoutDetail/{id}?new={new}",
                listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("new") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                WorkoutDetailScreen(nav, it.arguments!!.getLong("id"), it.arguments!!.getBoolean("new"))
            }
            composable("exercise/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                ExerciseDetailScreen(nav, it.arguments!!.getLong("id"))
            }
            composable("routine/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                RoutineEditScreen(nav, it.arguments!!.getLong("id"))
            }
            composable("bodyweight") { BodyWeightScreen(nav) }
            composable(
                "routineProgress?name={name}",
                listOf(navArgument("name") { type = NavType.StringType; defaultValue = "" }),
            ) {
                RoutineProgressScreen(nav, it.arguments?.getString("name").orEmpty())
            }
            composable(
                "coach?workout={workout}",
                listOf(navArgument("workout") { type = NavType.LongType; defaultValue = 0L }),
            ) {
                CoachScreen(nav, it.arguments!!.getLong("workout"))
            }
        }
    }
}

fun NavController.navigateTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun ActiveWorkoutBar(w: WorkoutFull, onClick: () -> Unit) {
    val now = rememberNow()
    val app = LocalContext.current.app
    val rest by app.restTimer.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(s.workoutInProgress, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "${w.workout.name} · ${fmtClock((now - w.workout.startTime) / 1000)}",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        rest?.let {
            Icon(Icons.Default.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(4.dp))
            Text(fmtClock(((it.endAt - now + 999) / 1000).coerceAtLeast(0)), color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
        }
        Icon(Icons.Default.KeyboardArrowUp, s.open, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun NotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
