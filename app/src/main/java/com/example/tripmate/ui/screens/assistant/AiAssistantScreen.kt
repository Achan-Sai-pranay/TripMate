package com.example.tripmate.ui.screens.assistant

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.tripmate.model.ChatMessage
import com.example.tripmate.ui.components.BottomNavTab
import com.example.tripmate.ui.components.InteractiveAssistantMap
import com.example.tripmate.ui.components.TripMateBottomNav
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.util.Locale

private const val ASSISTANT_USER_AVATAR_URL =
    "https://lh3.googleusercontent.com/aida-public/AB6AXuBqy6luG88wyBEaehc3k7YMnIHoQ2HZcdnrH5s-xz5cUz_ntiGlPNThuRFjWDw7DcngWwryr2rvj1IiKzWUtGN3bVs4y9t_4FgNe6RhvyfmivKLLzocIfEmzZ1orvfZigGhI29LC1_g-EhEiCMvkC5FGIaVl_UtNMvRIEBZh3SilOIRioEIIYE9HOfoNFMu7M18urmLeut41XZ6Bn1oZIUEJyNNHJHXuDHCPX2lQnoRDJ_FFQa23W2mHw"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onExploreClick: () -> Unit,
    onMyTripsClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialDestination: String = "",
    viewModel: AssistantViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeMapRoute by viewModel.activeMapRoute.collectAsState()
    var attachedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showQuickOptionsSheet by remember { mutableStateOf(false) }

    // Map panel starts collapsed so the chat keeps most of the screen
    var mapExpanded by remember { mutableStateOf(false) }
    var focusPinTitle by remember { mutableStateOf<String?>(null) }
    var focusRequestId by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // When the user drags the chat to read, get the map out of the way
    val isChatDragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isChatDragged) {
        if (isChatDragged && mapExpanded) mapExpanded = false
    }

    LaunchedEffect(initialDestination) {
        if (initialDestination.isNotBlank()) {
            viewModel.setInitialDestination(initialDestination)
        }
    }

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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask TripMate any travel question...")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val errorMessage by viewModel.errorMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Auto-scroll to latest message when new message arrives
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    // Determine if user has scrolled up and show the floating down arrow
    val showScrollToBottom by remember {
        derivedStateOf {
            messages.isNotEmpty() && listState.canScrollForward
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AssistantTopBar(
                avatarUrl = ASSISTANT_USER_AVATAR_URL,
                onAvatarClick = onProfileClick,
                onNotificationsClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("You're all caught up — no new notifications")
                    }
                }
            )
        },
        bottomBar = {
            com.example.tripmate.ui.components.TripMateBottomNav(
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
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Upper Area: Chat messages list (taking top space)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimens.marginMobile, vertical = Dimens.sm),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            EmptyStateWelcome(
                                onOrbClick = ::startVoiceInput,
                                onPromptClick = { prompt -> viewModel.sendMessage(prompt) }
                            )
                        }
                    } else {
                        itemsIndexed(messages) { _: Int, message: ChatMessage ->
                            CompetitorChatBubble(
                                message = message,
                                onSpotClick = if (activeMapRoute != null) { spot ->
                                    focusPinTitle = spot
                                    focusRequestId++
                                    mapExpanded = true
                                } else null
                            )
                        }

                        if (isLoading) {
                            item {
                                CompetitorSearchingIndicator()
                            }
                        }
                    }
                }

                // Floating Scroll-to-Bottom Button (inspired by competitor screenshot)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.lastIndex)
                                }
                            }
                        },
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        containerColor = Color.White,
                        contentColor = Color(0xFF1E293B),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Lower Area: collapsible map panel (collapsed = slim bar, expanded = real map)
            activeMapRoute?.let { route ->
                InteractiveAssistantMap(
                    route = route,
                    expanded = mapExpanded,
                    onExpandedChange = { mapExpanded = it },
                    onDismiss = {
                        mapExpanded = false
                        viewModel.dismissMap()
                    },
                    focusPinTitle = focusPinTitle,
                    focusRequestId = focusRequestId,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Attached photo preview if present
            attachedImage?.let { bitmap ->
                AttachedImagePreview(
                    bitmap = bitmap,
                    onRemove = { attachedImage = null }
                )
            }

            // Bottom Area: Status Header, Input Bar, and Disclaimer
            BottomChatInputSection(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onAddClick = { showQuickOptionsSheet = true },
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
                sendEnabled = !isLoading && (inputText.isNotBlank() || attachedImage != null)
            )
        }

        // Quick Options Modal Bottom Sheet
        if (showQuickOptionsSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showQuickOptionsSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.md, vertical = Dimens.sm)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Quick Assistant Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Photo picker option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.radiusMd))
                            .clickable {
                                showQuickOptionsSheet = false
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Attach travel photo",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Ask about a monument, hotel, or landscape",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val quickPrompts = listOf(
                        "3 day itinerary to Goa",
                        "Alternate places for day 3 in Goa",
                        "Best sunset beach shacks in North Goa",
                        "Historic churches & Latin quarter walk in Goa"
                    )

                    quickPrompts.forEach { prompt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Dimens.radiusMd))
                                .clickable {
                                    showQuickOptionsSheet = false
                                    viewModel.sendMessage(prompt)
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top bar header with profile photo, TripMate branding, and status indicator.
 */
@Composable
private fun AssistantTopBar(
    avatarUrl: String,
    onNotificationsClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
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
                    .clickable { onAvatarClick() }
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Spacer(modifier = Modifier.width(Dimens.sm))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TripMate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                }
                Text(
                    text = "AI Travel Assistant",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary
                )
            }
        }

        IconButton(onClick = onNotificationsClick) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Modern welcome state with glowing orb and quick prompt suggestions.
 */
@Composable
private fun EmptyStateWelcome(
    onOrbClick: () -> Unit,
    onPromptClick: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFF8A65), PrimaryOrange)
                    )
                )
                .clickable(onClick = onOrbClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = "Tap to speak",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Text(
            text = "Tap to speak, or type below",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Dimens.md, bottom = 4.dp)
        )

        Text(
            text = "Plan trips, alternate stops, and discover places on real maps",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = Dimens.lg)
        )

        // Suggestion chips matching competitor style
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "3 day itinerary to Goa" to "North Goa beaches, forts & Old Goa churches",
                "Alternate places for day 3 in Goa" to "Jardín Botánico, Local Art District & hidden spots",
                "Best sunset cafe spots in North Goa" to "Thalassa, Curlies & beachside dining"
            )

            suggestions.forEach { (title, subtitle) ->
                Card(
                    onClick = { onPromptClick(title) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = PrimaryOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Competitor-matching chat bubble:
 * User message on right with clean rounded pill (like "3 day itinerary to Goa").
 * Assistant message on left with "Searched the web", bot avatar, and structured cards.
 */
@Composable
private fun CompetitorChatBubble(message: ChatMessage, onSpotClick: ((String) -> Unit)? = null) {
    if (message.isFromUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                color = Color(0xFFF1F3F5),
                modifier = Modifier.widthIn(max = 290.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // "Searched the web" subtle italic label matching competitor screenshot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Searched the web",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontStyle = FontStyle.Italic),
                    color = Color(0xFF64748B)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                // Competitor coral/orange circular bot emblem
                Surface(
                    shape = CircleShape,
                    color = PrimaryOrange,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(top = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Structured travel message
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompetitorStructuredMessage(rawText = message.text, onSpotClick = onSpotClick)
                }
            }
        }
    }
}

/**
 * Renders structured travel assistant text matching competitor layout:
 * Overview paragraph, clean bold Day headers ("Day 1: North Goa beaches and forts"),
 * location stop cards with coordinates, and pro tips.
 */
@Composable
private fun CompetitorStructuredMessage(rawText: String, onSpotClick: ((String) -> Unit)? = null) {
    val lines = remember(rawText) { rawText.lines() }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            val trim = line.trim()
            if (trim.isBlank()) return@forEach

            when {
                // Day Section Header (e.g. "Day 1: North Goa beaches and forts")
                trim.startsWith("Day ", ignoreCase = true) ||
                trim.startsWith("### Day", ignoreCase = true) ||
                trim.startsWith("**Day", ignoreCase = true) -> {
                    val cleanDayTitle = trim
                        .removePrefix("###")
                        .replace("**", "")
                        .trim()

                    Text(
                        text = cleanDayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }

                // Spot item (e.g. "• Fort Aguada — 2h • Free: 17th-century lighthouse view")
                trim.startsWith("•") || trim.startsWith("-") || trim.startsWith("*") -> {
                    val content = trim.trimStart('•', '-', '*', ' ').trim()
                    val spotName = content
                        .substringBefore("—")
                        .substringBefore("-")
                        .substringBefore(":")
                        .replace(Regex("[\\[\\]*]"), "")
                        .trim()

                    val detailAndTip = content
                        .substringAfter("—", "")
                        .ifBlank { content.substringAfter("-", "").ifBlank { content.substringAfter(":", "") } }
                        .trim()

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onSpotClick != null) Modifier.clickable { onSpotClick(spotName) }
                                else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spotName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                if (detailAndTip.isNotBlank()) {
                                    Text(
                                        text = detailAndTip.replace("**", ""),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }

                            val context = androidx.compose.ui.platform.LocalContext.current
                            IconButton(
                                onClick = {
                                    val query = android.net.Uri.encode(spotName)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:0,0?q=$query"))
                                    runCatching { context.startActivity(intent) }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = "View on Map",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Pro Tip Card
                trim.startsWith("💡") ||
                trim.contains("Pro Tip", ignoreCase = true) ||
                trim.startsWith("Tip:", ignoreCase = true) -> {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp).padding(top = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = trim.replace("**", ""),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                // Standard overview / text
                else -> {
                    Text(
                        text = trim.replace("**", ""),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
                        color = Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}

/**
 * Loading indicator matching competitor style.
 */
@Composable
private fun CompetitorSearchingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = PrimaryOrange,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Searching travel spots & compiling map pins…",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontStyle = FontStyle.Italic),
            color = Color(0xFF64748B)
        )
    }
}

/**
 * Compact one-row input (add button, text field, microphone, and send button) with a one-line disclaimer.
 * Kept short on purpose so the chat and map get the vertical space.
 */
@Composable
private fun BottomChatInputSection(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onMicClick: () -> Unit,
    onSendClick: () -> Unit,
    sendEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = Dimens.sm, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusFull))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(Dimens.radiusFull))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAddClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Attach photo or quick prompts",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            androidx.compose.foundation.text.BasicTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, color = Color(0xFF0F172A)),
                maxLines = 4,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(PrimaryOrange),
                decorationBox = { inner: @Composable () -> Unit ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "Ask any travel question",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                }
            )

            IconButton(onClick = onMicClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice input",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = CircleShape,
                color = if (sendEnabled) PrimaryOrange else Color(0xFFCBD5E1),
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(enabled = sendEnabled, onClick = onSendClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Text(
            text = "AI responses may not be fully accurate.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color(0xFF94A3B8),
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 3.dp)
        )
    }
}

@Composable
private fun AttachedImagePreview(bitmap: Bitmap, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.marginMobile, vertical = Dimens.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(54.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Dimens.radiusSm))
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove photo",
                    tint = Color.White,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}
