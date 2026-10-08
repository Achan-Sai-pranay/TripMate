package com.example.tripmate.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripmate.ui.screens.auth.AuthViewModel
import com.example.tripmate.ui.screens.auth.LoginScreen
import com.example.tripmate.ui.screens.auth.SignUpScreen
import com.example.tripmate.ui.screens.auth.TravelVibeScreen

@Composable
fun AuthNavGraph(
    authViewModel: AuthViewModel = viewModel(),
    onSignedIn: () -> Unit = {}
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                viewModel = authViewModel
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onInstantSignedIn = {
                    authViewModel.startOnboarding()
                    navController.navigate(Screen.TravelVibe.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel
            )
        }
        composable(Screen.TravelVibe.route) {
            TravelVibeScreen(
                onContinue = { vibes ->
                    authViewModel.completeOnboarding(vibes)
                    onSignedIn()
                },
                onSkip = {
                    authViewModel.completeOnboarding(emptyList<String>())
                    onSignedIn()
                }
            )
        }
    }
}
