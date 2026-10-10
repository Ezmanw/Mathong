package com.ezmanw.mathong

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import com.ezmanw.mathong.notify.Reminders
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ezmanw.mathong.data.GameMode
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.LayoutRegistry
import com.ezmanw.mathong.ui.editor.EditorScreen
import com.ezmanw.mathong.ui.achievements.AchievementsScreen
import com.ezmanw.mathong.ui.game.GameScreen
import com.ezmanw.mathong.ui.help.HowToPlayScreen
import com.ezmanw.mathong.ui.help.TutorialScreen
import com.ezmanw.mathong.ui.home.HomeScreen
import com.ezmanw.mathong.ui.layouts.LayoutPickerScreen
import com.ezmanw.mathong.ui.settings.SettingsScreen
import com.ezmanw.mathong.ui.stats.StatsScreen
import com.ezmanw.mathong.ui.theme.MathongTheme

class MainActivity : ComponentActivity() {
    private val pendingOpen = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpen.value = intent.getStringExtra(Reminders.EXTRA_OPEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingOpen.value = intent?.getStringExtra(Reminders.EXTRA_OPEN)
        enableEdgeToEdge()
        val repo = (application as MathongApp).repository
        setContent {
            val settings by repo.settings.collectAsState(initial = Settings())
            MathongTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                MathongNavHost(rememberNavController(), pendingOpen.value) { pendingOpen.value = null }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val LAYOUTS = "layouts"
    const val GAME = "game?layout={layout}&daily={daily}&resume={resume}&mode={mode}&nonce={nonce}"
    const val STATS = "stats"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val EDITOR = "editor?id={id}"
    const val TUTORIAL = "tutorial"

    fun editor(id: String? = null) = "editor?id=${id ?: ""}"

    fun game(layout: String? = null, daily: Boolean = false, resume: Boolean = false, mode: GameMode = GameMode.CLASSIC) =
        "game?layout=${layout ?: ""}&daily=$daily&resume=$resume&mode=${mode.name}&nonce=${System.nanoTime()}"
}

@Composable
private fun MathongNavHost(nav: NavHostController, open: String?, onOpened: () -> Unit) {
    fun openGame(route: String) = nav.navigate(route) {
        popUpTo(Routes.HOME)
    }
    LaunchedEffect(open) {
        if (open == Reminders.OPEN_DAILY) {
            openGame(Routes.game(daily = true))
            onOpened()
        }
    }
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onContinue = { openGame(Routes.game(resume = true)) },
                onNewGame = { nav.navigate(Routes.LAYOUTS) },
                onQuickPlay = { openGame(Routes.game(LayoutRegistry.all().random().id)) },
                onDaily = { openGame(Routes.game(daily = true)) },
                onStats = { nav.navigate(Routes.STATS) },
                onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onHelp = { nav.navigate(Routes.HELP) },
                onTutorial = { nav.navigate(Routes.TUTORIAL) },
            )
        }
        composable(Routes.LAYOUTS) {
            LayoutPickerScreen(
                onBack = { nav.popBackStack() },
                onPick = { id, mode -> openGame(Routes.game(id, mode = mode)) },
                onCreate = { nav.navigate(Routes.editor()) },
                onEdit = { nav.navigate(Routes.editor(it)) },
            )
        }
        composable(
            Routes.GAME,
            arguments = listOf(
                navArgument("layout") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("daily") { type = NavType.BoolType; defaultValue = false },
                navArgument("resume") { type = NavType.BoolType; defaultValue = false },
                navArgument("mode") { type = NavType.StringType; defaultValue = GameMode.CLASSIC.name },
                navArgument("nonce") { type = NavType.LongType; defaultValue = 0L },
            ),
        ) {
            GameScreen(
                onBack = { nav.popBackStack(Routes.HOME, inclusive = false) },
                onChooseLayout = { nav.navigate(Routes.LAYOUTS) },
                onHelp = { nav.navigate(Routes.HELP) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) {
            EditorScreen(
                onBack = { nav.popBackStack() },
                onPlay = { openGame(Routes.game(it)) },
            )
        }
        composable(Routes.TUTORIAL) {
            TutorialScreen(onBack = { nav.popBackStack() }, onDone = { nav.popBackStack(Routes.HOME, inclusive = false) })
        }
        composable(Routes.STATS) { StatsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ACHIEVEMENTS) { AchievementsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HELP) { HowToPlayScreen(onBack = { nav.popBackStack() }, onTutorial = { nav.navigate(Routes.TUTORIAL) }) }
    }
}
