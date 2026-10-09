package com.example.tripmate.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.tripmate.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Dimens.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.xl)
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.md)) {
                Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                FaqItem(
                    question = "How does TripMate generate itineraries?",
                    answer = "We use advanced AI models to analyze your destination, budget, and travel vibes to craft an optimized, personalized day-by-day itinerary."
                )
                FaqItem(
                    question = "Can I change my budget?",
                    answer = "Yes! You can adjust your budget during the 'Plan a Trip' flow, or directly fine-tune it in the Expense Tracker tab of your itinerary."
                )
                FaqItem(
                    question = "Where is my trip data stored?",
                    answer = "Your trips, expenses, and itinerary items are securely synchronized to your cloud account via Supabase for multi-device access and live group collaboration, while cached locally on your device for offline travel viewing."
                )
                FaqItem(
                    question = "Is my data private and secure?",
                    answer = "Yes. TripMate uses encrypted Supabase cloud storage with strict row-level security policies. Only you and companions you explicitly invite can view or contribute to your trips."
                )
                FaqItem(
                    question = "How does group collaboration work?",
                    answer = "You can invite friends using unique 6-character trip codes. Once joined, everyone can add expenses, split bills, vote on activities, and view live itinerary changes."
                )
                FaqItem(
                    question = "Can I access my itineraries offline?",
                    answer = "Yes! Your planned itineraries, activity details, and saved stops remain cached on your device so you can access your schedule even without mobile data or Wi-Fi."
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.md), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Need more help?", style = MaterialTheme.typography.titleMedium)
                Button(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:support@tripmate.app")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "TripMate Support Request")
                    }
                    runCatching { context.startActivity(intent) }
                }) {
                    Text("Contact Us")
                }
            }
            
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TripMate App", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = Dimens.sm)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.sm)
            )
        }
    }
}
