package com.example.tripmate.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.tripmate.ui.screens.assistant.AiAssistantScreen
import com.example.tripmate.ui.screens.createtrip.basics.CreateTripBasicsScreen
import com.example.tripmate.ui.screens.createtrip.constraints.CreateTripConstraintsScreen
import com.example.tripmate.ui.screens.home.HomeScreen
import com.example.tripmate.ui.screens.itinerary.TripItineraryScreen
import com.example.tripmate.ui.screens.profile.ProfileScreen

/**
 * Navigates to a bottom-nav tab root, preserving each tab's back stack/scroll state
 * and avoiding duplicate destinations on repeated taps — the standard Compose
 * Navigation pattern for a persistent bottom bar.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun TripPilotNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(
            route = Screen.Home.route,
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) }
        ) {
            HomeScreen(
                onPlanNewTripClick = {
                    navController.navigate(Screen.CreateTripBasics.route)
                },
                onUpcomingTripClick = {
                    navController.navigateToTab(Screen.TripItinerary.route)
                },
                onMyTripsClick = {
                    navController.navigateToTab(Screen.TripItinerary.route)
                },
                onAssistantClick = {
                    navController.navigateToTab(Screen.AiAssistant.route)
                },
                onProfileClick = {
                    navController.navigateToTab(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.CreateTripBasics.route,
            enterTransition = {
                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = {
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
            }
        ) {
            CreateTripBasicsScreen(
                onBackClick = { navController.popBackStack() },
                onCloseClick = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onNextClick = { _ ->
                    navController.navigate(Screen.CreateTripConstraints.route)
                }
            )
        }

        composable(
            route = Screen.CreateTripConstraints.route,
            enterTransition = {
                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = {
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
            }
        ) {
            CreateTripConstraintsScreen(
                onBackClick = { navController.popBackStack() },
                onSkipClick = { navController.navigateToTab(Screen.TripItinerary.route) },
                onGenerateTripClick = { _ ->
                    navController.navigate(Screen.TripItinerary.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.TripItinerary.route,
            enterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            TripItineraryScreen(
                onExploreClick = {
                    navController.navigateToTab(Screen.Home.route)
                },
                onAssistantClick = {
                    navController.navigateToTab(Screen.AiAssistant.route)
                },
                onProfileClick = {
                    navController.navigateToTab(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.AiAssistant.route,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            AiAssistantScreen(
                onExploreClick = { navController.navigateToTab(Screen.Home.route) },
                onMyTripsClick = { navController.navigateToTab(Screen.TripItinerary.route) },
                onProfileClick = { navController.navigateToTab(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.Profile.route,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            ProfileScreen(
                onExploreClick = { navController.navigateToTab(Screen.Home.route) },
                onMyTripsClick = { navController.navigateToTab(Screen.TripItinerary.route) },
                onAssistantClick = { navController.navigateToTab(Screen.AiAssistant.route) }
            )
        }
    }
}
