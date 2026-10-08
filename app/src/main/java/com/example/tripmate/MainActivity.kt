package com.example.tripmate

import android.content.Intent
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
import com.example.tripmate.data.SupabaseClientProvider
import com.example.tripmate.navigation.AuthNavGraph
import com.example.tripmate.navigation.TripPilotNavGraph
import com.example.tripmate.ui.screens.auth.AuthViewModel
import com.example.tripmate.ui.theme.TripPilotTheme
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseClientProvider.client.handleDeeplinks(intent = intent)
        com.example.tripmate.data.WikipediaImageService.init(applicationContext)
        // Wikimedia rejects the default OkHttp user-agent (HTTP 403); identify the app, keep Coil's memory + disk caches.
        coil.Coil.setImageLoader(
            coil.ImageLoader.Builder(applicationContext)
                .okHttpClient {
                    okhttp3.OkHttpClient.Builder()
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request().newBuilder()
                                    .header("User-Agent", "TripMateApp/1.0 (Android; Contact: support@tripmate.app)")
                                    .build()
                            )
                        }
                        .build()
                }
                .crossfade(true)
                .build()
        )
        enableEdgeToEdge()
        setContent {
            TripPilotApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent = intent)
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