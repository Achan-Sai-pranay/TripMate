package com.example.tripmate.ui.screens.assistant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripmate.model.AssistantSuggestion
import com.example.tripmate.model.ChatMessage
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.TripPilotBottomNav
import com.example.tripmate.ui.theme.Dimens
import kotlinx.coroutines.launch

private const val ASSISTANT_USER_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuBqy6luG88wyBEaehc3k7YMnIHoQ2HZcdnrH5s-xz5cUz_ntiGlPNThuRFjWDw7DcngWwryr2rvj1IiKzWUtGN3bVs4y9t_4FgNe6RhvyfmivKLLzocIfEmzZ1orvfZigGhI29LC1_g-EhEiCMvkC5FGIaVl_UtNMvRIEBZh3SilOIRioEIIYE9HOfoNFMu7M18urmLeut41XZ6Bn1oZIUEJyNNHJHXuDHCPX2lQnoRDJ_FFQa23W2mHw"

@Composable
fun AiAssistantScreen(
    onExploreClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AssistantViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var attachedImage by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= 28) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, it)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            attachedImage = bitmap
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.sendMessage(spokenText)
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask TripPilot anything...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val suggestions = remember {
        listOf(
            AssistantSuggestion("Check my remaining budget", "Review expenses for Paris trip", Icons.Filled.AccountBalanceWallet),
            AssistantSuggestion("Find a nearby cafe", "Looking for strong espresso and wifi", Icons.Filled.LocalCafe),
            AssistantSuggestion("Replan Day 2 for rain", "Indoor activities in Kyoto", Icons.Filled.Cloud)
        )
    }

    // Surface API/network errors as a snackbar instead of failing silently
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Auto-scroll to the latest message as the conversation grows
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AssistantTopBar(
                avatarUrl = ASSISTANT_USER_AVATAR_URL,
                onNotificationsClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("You're all caught up — no new notifications")
                    }
                }
            )
        },
        bottomBar = {
            TripPilotBottomNav(
                selectedTab = BottomNavTab.ASSISTANT,
                onTabSelected = { tab ->
                    when (tab) {
                        BottomNavTab.EXPLORE -> onExploreClick()
                        BottomNavTab.MY_TRIPS -> onMyTripsClick()
                        BottomNavTab.PROFILE -> onProfileClick()
                        BottomNavTab.ASSISTANT -> { /* already here */ }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = Dimens.marginMobile, vertical = Dimens.md),
                verticalArrangement = Arrangement.spacedBy(Dimens.md)
            ) {
                item {
                    AiOrbSection(
                        onOrbClick = ::startVoiceInput,
                        modifier = Modifier.padding(vertical = Dimens.lg)
                    )
                }

                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = "SUGGESTIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = Dimens.xs)
                        )
                    }
                    itemsIndexed(suggestions) { _, suggestion ->
                        SuggestionCard(
                            suggestion = suggestion,
                            onClick = { viewModel.sendMessage(suggestion.title) },
                            modifier = Modifier.padding(bottom = Dimens.sm)
                        )
                    }
                } else {
                    itemsIndexed(messages) { _, message ->
                        ChatBubble(message = message)
                    }
                    if (isLoading) {
                        item { TypingIndicator() }
                    }
                }
            }

            attachedImage?.let { bitmap ->
                AttachedImagePreview(
                    bitmap = bitmap,
                    onRemove = { attachedImage = null }
                )
            }

            // Bottom Map Route popup when itinerary/destinations are discussed
            val mapRoute by viewModel.activeMapRoute.collectAsState()
            mapRoute?.let { route ->
                com.example.tripmate.ui.components.AssistantBottomMapRoute(
                    route = route,
                    onDismiss = { viewModel.dismissMap() }
                )
            }

            AssistantInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onAttachClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onMicClick = ::startVoiceInput,
                onSendClick = {
                    val image = attachedImage
                    when {
                        image != null -> {
                            viewModel.sendMessageWithImage(inputText, image)
                            inputText = ""
                            attachedImage = null
                        }
                        inputText.isNotBlank() -> {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                },
                sendEnabled = !isLoading
            )
        }
    }
}

@Composable
private fun AssistantTopBar(
    avatarUrl: String,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Text(
                text = "TripPilot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Dimens.sm)
            )
        }
        IconButton(onClick = onNotificationsClick) {
            Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AiOrbSection(onOrbClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    )
                )
                .clickable(onClick = onOrbClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = "Tap to speak", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
        }
        Text(
            text = "Tap to speak, or type below",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Dimens.md)
        )
    }
}

@Composable
private fun AttachedImagePreview(bitmap: Bitmap, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(Dimens.radiusSm))
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp).align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove photo",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    if (message.isFromUser) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.tripmate.ui.theme.PrimaryOrange)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = Dimens.md, vertical = Dimens.sm)
                )
            }
        }
    } else {
        // Wanderlog-Style Assistant Card (Structured Cards, Spots & Badges)
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.96f),
                shape = RoundedCornerShape(Dimens.radiusLg),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, com.example.tripmate.ui.theme.SubtleBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(Dimens.md)) {
                    // Header Brand Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Dimens.xs)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = com.example.tripmate.ui.theme.PrimaryOrange.copy(alpha = 0.12f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = com.example.tripmate.ui.theme.PrimaryOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Dimens.xs))
                        Text(
                            text = "TripMate AI Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = com.example.tripmate.ui.theme.PrimaryOrange
                        )
                    }

                    // Structured Wanderlog Content
                    WanderlogStructuredText(rawText = message.text)
                }
            }
        }
    }
}

@Composable
private fun WanderlogStructuredText(rawText: String) {
    val lines = remember(rawText) { rawText.lines() }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        lines.forEach { line ->
            val trim = line.trim()
            if (trim.isBlank()) return@forEach

            when {
                // Day Section Header (e.g. "Day 1: Lake & Heritage")
                trim.startsWith("Day ", ignoreCase = true) ||
                trim.startsWith("### Day", ignoreCase = true) ||
                trim.startsWith("**Day", ignoreCase = true) -> {
                    val cleanDayTitle = trim
                        .removePrefix("###")
                        .replace("**", "")
                        .trim()

                    Surface(
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.xs, bottom = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                tint = com.example.tripmate.ui.theme.PrimaryOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cleanDayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.example.tripmate.ui.theme.TextPrimary
                            )
                        }
                    }
                }

                // Spot item (e.g. "• Dal Lake Shikara — 2h • ₹800: Floating market view")
                trim.startsWith("•") || trim.startsWith("-") || trim.startsWith("*") -> {
                    val content = trim.trimStart('•', '-', '*', ' ').trim()
                    val spotName = content
                        .substringBefore("—")
                        .substringBefore("-")
                        .substringBefore(":")
                        .replace("**", "")
                        .trim()

                    val detailAndTip = content
                        .substringAfter("—", "")
                        .ifBlank { content.substringAfter("-", "").ifBlank { content.substringAfter(":", "") } }
                        .trim()

                    Card(
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.sm),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(com.example.tripmate.ui.theme.PrimaryOrange.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = com.example.tripmate.ui.theme.PrimaryOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.xs))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spotName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.tripmate.ui.theme.TextPrimary
                                )
                                if (detailAndTip.isNotBlank()) {
                                    Text(
                                        text = detailAndTip.replace("**", ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = com.example.tripmate.ui.theme.TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Pro Tip / Key Insight Highlight
                trim.startsWith("💡") ||
                trim.contains("Pro Tip", ignoreCase = true) ||
                trim.startsWith("Tip:", ignoreCase = true) -> {
                    Surface(
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.sm),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp).padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimens.xs))
                            Text(
                                text = trim.replace("**", ""),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                // Overview or summary text
                else -> {
                    Text(
                        text = trim.replace("**", ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.example.tripmate.ui.theme.TextPrimary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Card(
            shape = RoundedCornerShape(Dimens.radiusMd),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Row(modifier = Modifier.padding(Dimens.md), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "TripPilot is typing…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Dimens.sm)
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(suggestion: AssistantSuggestion, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Row(modifier = Modifier.padding(Dimens.md), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Dimens.radiusMd))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(suggestion.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = Dimens.md)) {
                Text(text = suggestion.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp), color = MaterialTheme.colorScheme.onSurface)
                Text(text = suggestion.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun AssistantInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    sendEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.md)
            .clip(RoundedCornerShape(Dimens.radiusFull))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(Dimens.radiusFull))
            .padding(start = Dimens.md, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask TripMate anything (e.g. 3 day Goa itinerary)...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
        IconButton(onClick = onMicClick) {
            Icon(Icons.Filled.Mic, contentDescription = "Speak", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onAttachClick) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Attach file", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (sendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onSendClick, enabled = sendEnabled) {
                Icon(Icons.Filled.ArrowUpward, contentDescription = "Send", tint = if (sendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
