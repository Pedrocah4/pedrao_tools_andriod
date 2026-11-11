package com.pedraotools.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pedraotools.app.ui.screens.GoogleReviewScreen
import com.pedraotools.app.ui.screens.HomeScreen
import com.pedraotools.app.ui.screens.PlaceholderScreen
import com.pedraotools.app.ui.theme.PedraoToolsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PedraoToolsTheme {
                PedraoToolsApp()
            }
        }
    }
}

private object AppRoutes {
    const val HOME = "home"
    const val GOOGLE_REVIEW = "google_review"
    const val PLACEHOLDER = "placeholder"
}

@Composable
fun PedraoToolsApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                onOpenGoogleReview = {
                    navController.navigate(AppRoutes.GOOGLE_REVIEW)
                },
                onOpenPlaceholder = {
                    navController.navigate(AppRoutes.PLACEHOLDER)
                }
            )
        }

        composable(AppRoutes.GOOGLE_REVIEW) {
            GoogleReviewScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AppRoutes.PLACEHOLDER) {
            PlaceholderScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

