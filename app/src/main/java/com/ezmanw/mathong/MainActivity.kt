package com.ezmanw.mathong

import android.os.Bundle
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
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.game.Layouts
import com.ezmanw.mathong.ui.achievements.AchievementsScreen
import com.ezmanw.mathong.ui.game.GameScreen
import com.ezmanw.mathong.ui.help.HowToPlayScreen
import com.ezmanw.mathong.ui.home.HomeScreen
import com.ezmanw.mathong.ui.layouts.LayoutPickerScreen
import com.ezmanw.mathong.ui.settings.SettingsScreen
import com.ezmanw.mathong.ui.stats.StatsScreen
import com.ezmanw.mathong.ui.theme.MathongTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as MathongApp).repository
        setContent {
            val settings by repo.settings.collectAsState(initial = Settings())
            MathongTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                MathongNavHost(rememberNavController())
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val LAYOUTS = "layouts"
    const val GAME = "game?layout={layout}&daily={daily}&resume={resume}&nonce={nonce}"
    const val STATS = "stats"
    const val ACHIEVEMENTS = "achievements"
    const val SETTINGS = "settings"
    const val HELP = "help"

    fun game(layout: String? = null, daily: Boolean = false, resume: Boolean = false) =
        "game?layout=${layout ?: ""}&daily=$daily&resume=$resume&nonce=${System.nanoTime()}"
}

@Composable
private fun MathongNavHost(nav: NavHostController) {
    fun openGame(route: String) = nav.navigate(route) {
        popUpTo(Routes.HOME)
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
                onQuickPlay = { openGame(Routes.game(Layouts.all.random().id)) },
                onDaily = { openGame(Routes.game(daily = true)) },
                onStats = { nav.navigate(Routes.STATS) },
                onAchievements = { nav.navigate(Routes.ACHIEVEMENTS) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onHelp = { nav.navigate(Routes.HELP) },
            )
        }
        composable(Routes.LAYOUTS) {
            LayoutPickerScreen(onBack = { nav.popBackStack() }, onPick = { openGame(Routes.game(it)) })
        }
        composable(
            Routes.GAME,
            arguments = listOf(
                navArgument("layout") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("daily") { type = NavType.BoolType; defaultValue = false },
                navArgument("resume") { type = NavType.BoolType; defaultValue = false },
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
        composable(Routes.STATS) { StatsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ACHIEVEMENTS) { AchievementsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HELP) { HowToPlayScreen(onBack = { nav.popBackStack() }) }
    }
}
