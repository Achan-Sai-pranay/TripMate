package com.example.tripmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.navigation.AuthNavGraph
import com.example.tripmate.navigation.TripPilotNavGraph
import com.example.tripmate.ui.screens.auth.AuthViewModel
import com.example.tripmate.ui.theme.TripPilotTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripPilotApp()
        }
    }
}

@Composable
fun TripPilotApp() {
    TripPilotTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authViewModel: AuthViewModel = viewModel()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

            when (isLoggedIn) {
                null  -> { /* Session check in-flight — show nothing briefly */ }
                false -> AuthNavGraph()
                true  -> TripPilotNavGraph()
            }
        }
    }
}