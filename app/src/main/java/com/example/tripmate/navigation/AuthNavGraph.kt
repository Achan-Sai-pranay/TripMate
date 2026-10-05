package com.example.tripmate.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripmate.ui.screens.auth.LoginScreen
import com.example.tripmate.ui.screens.auth.SignUpScreen

@Composable
fun AuthNavGraph(
    onSignedIn: () -> Unit = {}
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onInstantSignedIn = onSignedIn
            )
        }
    }
}
