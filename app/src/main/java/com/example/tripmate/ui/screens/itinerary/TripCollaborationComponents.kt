package com.example.tripmate.ui.screens.itinerary

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.tripmate.model.ProfileRow
import com.example.tripmate.model.TripPlan
import com.example.tripmate.ui.shared.TripPlanViewModel
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.PrimaryOrangeVariant
import kotlinx.coroutines.launch

/**
 * Prominent collaboration bar placed directly on the trip itinerary screen.
 * Shows co-travelers, group voting status, and direct buttons to invite friends or manage expenses.
 */
@Composable
fun TripCollaborationCard(
    plan: TripPlan,
    members: List<ProfileRow>,
    onInviteClick: () -> Unit,
    onManageExpensesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memberCount = (members.size).coerceAtLeast(if (plan.isShared) plan.membersCount else 1)
    val isMultiUser = memberCount > 1 || plan.isShared

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMultiUser) PrimaryOrange.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            // Header Row: Avatars & Invite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    // Overlapping Avatar Stack
                    MemberAvatarStack(members = members)

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isMultiUser) "Group Trip (${memberCount} Travelers)" else "Solo Trip",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (isMultiUser) "Voting & Split Expenses Active" else "Invite friends to collaborate & vote",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Invite Companions Button (Protected width and single line)
                Button(
                    onClick = onInviteClick,
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "Invite",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Invite",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            // Action Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                // Group Expenses Pill
                OutlinedButton(
                    onClick = onManageExpensesClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryOrange
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Group Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Group Voting Pill / Indicator
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HowToVote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Vote on Activities",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Avatar cluster showing members of a collaborative trip.
 */
@Composable
fun MemberAvatarStack(
    members: List<ProfileRow>,
    modifier: Modifier = Modifier
) {
    if (members.isEmpty()) {
        Box(
            modifier = modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy((-8).dp)
        ) {
            members.take(3).forEachIndexed { index, profile ->
                val initials = profile.fullName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("")
                    ?: profile.email?.firstOrNull()?.uppercase() ?: "U"

                val bgColors = listOf(
                    PrimaryOrange,
                    Color(0xFF26A69A),
                    Color(0xFF5C6BC0)
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(bgColors[index % bgColors.size]),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = profile.fullName,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (members.size > 3) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${members.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Production-level Invite Modal Bottom Sheet.
 * Generates instant shareable links, invite codes, native Android share sheet, and direct email invitations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripInviteSheet(
    tripPlanViewModel: TripPlanViewModel,
    destination: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val isLoggedIn = remember { tripPlanViewModel.isLoggedIn() }
    var effectiveTripId by remember { mutableStateOf<String?>(null) }
    var cloudSyncPending by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(true) }
    var emailInput by remember { mutableStateOf("") }
    var isSendingEmail by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val members by tripPlanViewModel.tripMembers.collectAsState()

    // Ensure trip is synced with Supabase to obtain a shareable cloudTripId
    LaunchedEffect(Unit) {
        isSyncing = true
        val syncedId = tripPlanViewModel.ensureTripSyncedToCloud()
        val currentPlan = tripPlanViewModel.tripPlan.value
        val fallbackId = currentPlan?.supabaseTripId ?: currentPlan?.id
        effectiveTripId = syncedId ?: fallbackId
        cloudSyncPending = (syncedId == null && fallbackId != null)
        isSyncing = false
    }

    val inviteLink = effectiveTripId?.let { "https://tripmate.app/join/$it" } ?: ""
    val shareText = "Join my $destination trip on TripMate! 🌴✈️\n\n" +
            "Click the link to view the itinerary, vote on activities, and split group expenses with us:\n" +
            "$inviteLink\n\n" +
            "(Or enter trip code: $effectiveTripId in TripMate)"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Invite Friends to $destination",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Co-plan, vote on activities & split expenses together",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (isSyncing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.lg),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = PrimaryOrange,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Preparing invite link…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!isLoggedIn) {
                Text(
                    text = "Please log in to TripMate to generate shareable invite links and invite friends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = Dimens.md)
                )
            } else if (effectiveTripId == null) {
                Text(
                    text = "No active trip found to share.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = Dimens.md)
                )
            } else {
                if (cloudSyncPending) {
                    Surface(
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Dimens.sm, vertical = Dimens.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(Dimens.xs))
                            Text(
                                text = "Notice: Live cloud sync pending database setup. Using trip code for sharing.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
                // Section 1: Shareable Link Box
                Text(
                    text = "Shareable Trip Link",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.md, vertical = Dimens.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inviteLink,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(inviteLink))
                                Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Link",
                                tint = PrimaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Share Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Invite friends to $destination trip")
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.radiusFull),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Link", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(effectiveTripId ?: ""))
                            Toast.makeText(context, "Trip code copied!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(Dimens.radiusFull)
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Code")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.xs), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Section 2: Direct Email Invite
                Text(
                    text = "Or Invite Directly via Email",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; statusMessage = null },
                        placeholder = { Text("friend@email.com", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        leadingIcon = {
                            Icon(Icons.Filled.Mail, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    )

                    Button(
                        onClick = {
                            val email = emailInput.trim()
                            if (email.isBlank() || !email.contains("@")) {
                                statusMessage = "Please enter a valid email address."
                                isError = true
                                return@Button
                            }
                            isSendingEmail = true
                            tripPlanViewModel.inviteMember(email) { success, error ->
                                isSendingEmail = false
                                if (success) {
                                    emailInput = ""
                                    statusMessage = "Companion added to trip!"
                                    isError = false
                                } else {
                                    statusMessage = error ?: "Failed to invite companion"
                                    isError = true
                                }
                            }
                        },
                        enabled = !isSendingEmail && emailInput.isNotBlank(),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        if (isSendingEmail) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Invite")
                        }
                    }
                }

                statusMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Section 3: Current Members List
                if (members.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.xs), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(
                        text = "Current Members (${members.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.xs)) {
                        members.forEachIndexed { idx, member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.sm)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryOrangeVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (member.fullName?.firstOrNull() ?: member.email?.firstOrNull() ?: 'U').uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = member.fullName ?: member.email ?: "Traveler",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!member.fullName.isNullOrBlank() && !member.email.isNullOrBlank()) {
                                            Text(
                                                text = member.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(Dimens.radiusFull),
                                    color = if (idx == 0) PrimaryOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Text(
                                        text = if (idx == 0) "Owner" else "Companion",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (idx == 0) PrimaryOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dimens.md))
        }
    }
}

/**
 * Clean dialog for friends to enter an invite code or paste a trip link to join immediately.
 */
@Composable
fun JoinTripDialog(
    tripPlanViewModel: TripPlanViewModel,
    onDismiss: () -> Unit,
    onSuccess: (destination: String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.GroupAdd,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(Dimens.sm))
                Text("Join a Trip", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                Text(
                    text = "Enter the trip code or paste the invite link shared by your friend to collaborate and vote together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; errorMessage = null },
                    placeholder = { Text("Paste link or enter code", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.radiusMd)
                )

                errorMessage?.let {
                    Surface(
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(Dimens.sm)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val code = input.trim()
                    if (code.isBlank()) {
                        errorMessage = "Please enter an invite code or link."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    tripPlanViewModel.joinTripByCodeOrLink(code) { success, msg ->
                        isLoading = false
                        if (success) {
                            val dest = tripPlanViewModel.tripPlan.value?.destination ?: "Trip"
                            onSuccess(dest)
                            onDismiss()
                        } else {
                            val cleanMsg = when {
                                msg?.contains("permission denied", ignoreCase = true) == true || msg?.contains("42501") == true ->
                                    "Cloud database permission required. Please run the schema SQL script in your Supabase dashboard to grant permissions on the trips table."
                                msg?.contains("No trip found", ignoreCase = true) == true || msg?.contains("PGRST116") == true ->
                                    "No trip found with this code. Please verify the link or code."
                                else -> msg ?: "Could not join trip. Please verify the code or link."
                            }
                            errorMessage = cleanMsg
                        }
                    }
                },
                enabled = !isLoading && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Join Trip", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
