package com.example.tripmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.data.SupabaseClientProvider
import com.example.tripmate.data.UserPreferencesRepository
import com.example.tripmate.navigation.AuthNavGraph
import com.example.tripmate.navigation.TripMateNavGraph
import com.example.tripmate.ui.screens.auth.AuthViewModel
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.PrimaryOrangeVariant
import com.example.tripmate.ui.theme.TripMateTheme
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
        handleTripInviteDeepLink(intent)
        setContent {
            TripMateApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseClientProvider.client.handleDeeplinks(intent = intent)
        handleTripInviteDeepLink(intent)
    }

    private fun handleTripInviteDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val tripId = if (uri.scheme == "tripmate" && uri.host == "join") {
            uri.lastPathSegment ?: uri.getQueryParameter("id") ?: uri.getQueryParameter("code")
        } else if ((uri.scheme == "https" || uri.scheme == "http") && uri.host?.contains("tripmate.app") == true && uri.path?.contains("/join") == true) {
            uri.lastPathSegment ?: uri.getQueryParameter("id") ?: uri.getQueryParameter("code")
        } else null

        if (!tripId.isNullOrBlank()) {
            pendingTripInviteId.value = tripId.trim()
        }
    }

    companion object {
        val pendingTripInviteId = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    }
}

@Composable
fun TripMateApp() {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferencesRepository(context.applicationContext) }
    val darkModePref by userPrefs.darkModeFlow.collectAsState(initial = "LIGHT")
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (darkModePref) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }

    TripMateTheme(darkTheme = isDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authViewModel: AuthViewModel = viewModel()
            val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
            val isOnboarding by authViewModel.isOnboarding.collectAsState()

            when {
                isLoggedIn == null -> TripMateSplashScreen()
                isLoggedIn == true && !isOnboarding -> TripMateNavGraph()
                else -> AuthNavGraph(authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun TripMateSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PrimaryOrange,
                                PrimaryOrangeVariant
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.FlightTakeoff,
                    contentDescription = "TripMate",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "TripMate",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Smart AI Itineraries & Group Travel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(32.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = PrimaryOrange
            )
        }
    }
}

@Composable
fun TripPilotApp() {
    TripMateApp()
}