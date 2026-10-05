package com.example.tripmate.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.ui.shared.TripPlanViewModel
import com.example.tripmate.ui.screens.assistant.AiAssistantScreen
import com.example.tripmate.ui.screens.createtrip.basics.CreateTripBasicsScreen
import com.example.tripmate.ui.screens.createtrip.constraints.CreateTripConstraintsScreen
import com.example.tripmate.ui.screens.createtrip.preferences.CreateTripPreferencesScreen
import com.example.tripmate.ui.screens.expenses.ExpenseTrackerScreen
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
        val startDestinationId = graph.findStartDestination().id
        popUpTo(startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        // If navigating back to the start destination (Explore),
        // we don't want to restore a state that might put us back on another tab.
        restoreState = (route != graph.findStartDestination().route)
    }
}

@Composable
fun TripPilotNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val tripPlanViewModel: TripPlanViewModel = viewModel()

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
                tripPlanViewModel = tripPlanViewModel,
                onPlanNewTripClick = {
                    navController.navigate(Screen.CreateTripBasics.buildRoute())
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
                },
                onSearchDestination = { destination ->
                    navController.navigate(Screen.CreateTripBasics.buildRoute(destination))
                },
                onQuickTripLength = { startMillis, endMillis ->
                    tripPlanViewModel.presetDates(startMillis, endMillis)
                    navController.navigate(Screen.CreateTripBasics.buildRoute())
                },
                onTravelStatsClick = {
                    navController.navigateToTab(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.CreateTripBasics.route,
            arguments = listOf(
                androidx.navigation.navArgument(Screen.CreateTripBasics.ARG_DESTINATION) {
                    type = androidx.navigation.NavType.StringType
                    defaultValue = ""
                }
            ),
            enterTransition = {
                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = {
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
            }
        ) { backStackEntry ->
            val prefillDestination = backStackEntry.arguments?.getString(Screen.CreateTripBasics.ARG_DESTINATION).orEmpty()
            CreateTripBasicsScreen(
                initialDestination = prefillDestination,
                onBackClick = { navController.popBackStack() },
                onCloseClick = { navController.popBackStack(Screen.Home.route, inclusive = false) },
                onNextClick = { basics ->
                    tripPlanViewModel.updateDestinationAndDates(
                        destination = basics.destination,
                        startMillis = basics.startDateMillis,
                        endMillis = basics.endDateMillis,
                        travelers = basics.travelers
                    )
                    navController.navigate(Screen.CreateTripPreferences.route)
                }
            )
        }

        composable(
            route = Screen.CreateTripPreferences.route,
            enterTransition = {
                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = {
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
            }
        ) {
            CreateTripPreferencesScreen(
                onBackClick = { navController.popBackStack() },
                onDoneClick = { prefs ->
                    tripPlanViewModel.updatePreferences(prefs)
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
                onSkipClick = { 
                    tripPlanViewModel.generateTrip()
                    navController.navigate(Screen.TripItinerary.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onGenerateTripClick = { constraints ->
                    tripPlanViewModel.updateConstraints(constraints)
                    tripPlanViewModel.generateTrip()
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
                tripPlanViewModel = tripPlanViewModel,
                onExploreClick = {
                    navController.navigateToTab(Screen.Home.route)
                },
                onPlanNewTripClick = {
                    navController.navigate(Screen.CreateTripBasics.route)
                },
                onAssistantClick = {
                    navController.navigateToTab(Screen.AiAssistant.route)
                },
                onProfileClick = {
                    navController.navigateToTab(Screen.Profile.route)
                },
                onOpenExpenses = { tripId ->
                    navController.navigate(Screen.ExpenseTracker.buildRoute(tripId))
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
                onAssistantClick = { navController.navigateToTab(Screen.AiAssistant.route) },
                onSettingsClick = { navController.navigate(Screen.AccountSettings.route) },
                onHelpClick = { navController.navigate(Screen.HelpSupport.route) },
                onLoggedOut = {
                    // Session flow in MainActivity automatically swaps to AuthNavGraph.
                    // Clear local trip state so it's fresh for the next user.
                    tripPlanViewModel.clearTrip()
                }
            )
        }

        composable(
            route = Screen.AccountSettings.route,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            com.example.tripmate.ui.screens.profile.AccountSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.HelpSupport.route,
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(200)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) {
            com.example.tripmate.ui.screens.profile.HelpSupportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ExpenseTracker.route,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
            ExpenseTrackerScreen(
                tripId = tripId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
