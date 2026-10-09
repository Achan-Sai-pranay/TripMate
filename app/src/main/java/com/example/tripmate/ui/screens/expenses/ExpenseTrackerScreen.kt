package com.example.tripmate.ui.screens.expenses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import com.example.tripmate.model.ExpenseCategory
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tripmate.data.MemberBalance
import com.example.tripmate.model.CurrencyInfo
import com.example.tripmate.model.SUPPORTED_CURRENCIES
import com.example.tripmate.model.cleanNotes
import com.example.tripmate.model.currencyMeta
import com.example.tripmate.model.exchangeRate
import com.example.tripmate.model.getCurrencyInfo
import com.example.tripmate.model.originalAmount
import com.example.tripmate.model.originalCurrency
import com.example.tripmate.ui.theme.Dimens
import java.util.Locale

@Composable
fun ExpenseTrackerScreen(
    tripId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseTrackerViewModel = viewModel()
) {
    val trip             by viewModel.trip.collectAsState()
    val isInternational  by viewModel.isInternational.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val exchangeRates    by viewModel.exchangeRates.collectAsState()
    val isFetchingRates  by viewModel.isFetchingRates.collectAsState()
    val expenses         by viewModel.expenses.collectAsState()
    val members          by viewModel.members.collectAsState()
    val balances         by viewModel.balances.collectAsState()
    val isLoading        by viewModel.isLoading.collectAsState()
    val errorMessage     by viewModel.errorMessage.collectAsState()

    var showAddExpense    by remember { mutableStateOf(false) }
    var showInviteDialog  by remember { mutableStateOf(false) }
    var showTripTypeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tripId) { viewModel.load(tripId) }
    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.dismissError() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.xs, vertical = Dimens.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = Dimens.xs)
                ) {
                    Text(
                        "Group Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // International mode pill badge (visible only when international is detected/active)
                    if (isInternational) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { showTripTypeDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "International • Multi-Currency",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else if (trip?.destination != null) {
                        Text(
                            text = "${trip?.destination} (Domestic)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { showTripTypeDialog = true }
                        )
                    }
                }

                IconButton(onClick = { showInviteDialog = true }) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = "Invite member",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddExpense = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add expense",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        if (isLoading && expenses.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    "Loading expenses…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Dimens.md)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(Dimens.marginMobile),
                verticalArrangement = Arrangement.spacedBy(Dimens.lg)
            ) {
                // ── Balances section ──────────────────────────────────────────
                item {
                    Text(
                        "Balances",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (balances.isEmpty()) {
                    item {
                        Text(
                            "No members yet — tap the person icon to invite someone to split costs.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(balances) { balance -> BalanceRow(balance) }
                }

                // ── Expenses section ──────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isInternational) {
                            Text(
                                "Base currency: INR (₹)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (expenses.isEmpty()) {
                    item {
                        Text(
                            "No expenses logged yet. Tap + to add the first one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(expenses.reversed()) { expense ->
                        val origCurr = expense.originalCurrency
                        val origAmt = expense.originalAmount
                        val fxRate = expense.exchangeRate
                        val hasForeignCurrency = origCurr != null && origCurr != "INR" && origAmt != null

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            border = BorderStroke(
                                1.dp, MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.md),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        expense.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (hasForeignCurrency && origCurr != null && origAmt != null) {
                                        val currInfo = getCurrencyInfo(origCurr)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(Dimens.radiusSm),
                                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    text = "${currInfo.flagEmoji} ${currInfo.symbol}${"%.2f".format(Locale.US, origAmt)} ($origCurr)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (fxRate != null) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "@ ₹${"%.2f".format(Locale.US, fxRate)}/${origCurr}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    expense.category?.let { cat ->
                                        val displayLabel = runCatching { ExpenseCategory.valueOf(cat).label }.getOrDefault(cat)
                                        Text(
                                            displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val shareInInr = expense.amount / members.size.coerceAtLeast(1)
                                    val splitText = if (origCurr != null && origAmt != null) {
                                        val shareInForeign = origAmt / members.size.coerceAtLeast(1)
                                        val symbol = getCurrencyInfo(origCurr).symbol
                                        "Split: ₹${"%.0f".format(Locale.US, shareInInr)} ($symbol${"%.2f".format(Locale.US, shareInForeign)}) each"
                                    } else {
                                        "Split equally among ${members.size} member(s)"
                                    }

                                    Text(
                                        splitText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (!expense.cleanNotes.isNullOrBlank()) {
                                        Text(
                                            "Note: ${expense.cleanNotes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = Dimens.sm)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "₹${expense.amount.toLong()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (hasForeignCurrency) {
                                            Text(
                                                "converted",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    if (expense.id != null) {
                                        IconButton(
                                            onClick = { viewModel.deleteExpense(tripId, expense.id) },
                                            modifier = Modifier.size(32.dp).padding(start = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add Expense dialog ────────────────────────────────────────────────────
    if (showAddExpense) {
        if (isInternational) {
            InternationalAddExpenseDialog(
                membersCount = members.size,
                selectedCurrency = selectedCurrency,
                exchangeRates = exchangeRates,
                isFetchingRates = isFetchingRates,
                onSelectCurrency = { viewModel.selectCurrency(it) },
                onRefreshRates = { viewModel.fetchRates() },
                onDismiss = { showAddExpense = false },
                onConfirm = { desc, amount, category, curr, customRate, notes ->
                    viewModel.addExpense(
                        tripId = tripId,
                        description = desc,
                        amount = amount,
                        category = category,
                        currencyCode = curr.code,
                        customRate = customRate,
                        userNotes = notes
                    )
                    showAddExpense = false
                }
            )
        } else {
            DomesticAddExpenseDialog(
                membersCount = members.size,
                onDismiss = { showAddExpense = false },
                onConfirm = { desc, amount, category ->
                    viewModel.addExpense(tripId, desc, amount, category = category)
                    showAddExpense = false
                }
            )
        }
    }

    // ── Trip Type Dialog (Allows manual toggle between Domestic and International) ─
    if (showTripTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTripTypeDialog = false },
            title = { Text("Trip Currency Mode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    Text(
                        "TripMate auto-detects whether your trip destination is domestic or international.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Currently set to: ${if (isInternational) "🌍 International (Multi-Currency Enabled)" else "🇮🇳 Domestic (Single Currency ₹)"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "You can toggle this mode if you want to record foreign currency expenses for this trip.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleInternationalMode()
                    showTripTypeDialog = false
                }) {
                    Text(if (isInternational) "Switch to Domestic" else "Switch to International")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTripTypeDialog = false }) { Text("Close") }
            }
        )
    }

    // ── Invite dialog ─────────────────────────────────────────────────────────
    if (showInviteDialog) {
        var inviteEmail by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("Invite by Email") },
            text = {
                Column {
                    Text(
                        "They must already have a TripMate account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.sm)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inviteEmail.isNotBlank()) {
                        viewModel.inviteMember(tripId, inviteEmail)
                        showInviteDialog = false
                    }
                }) { Text("Invite", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Standard domestic add expense dialog - clean and simple single currency in ₹.
 */
@Composable
private fun DomesticAddExpenseDialog(
    membersCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, category: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(
                    "Split equally among all $membersCount member(s).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amountValue = amount.toDoubleOrNull()
                if (description.isNotBlank() && amountValue != null && amountValue > 0) {
                    onConfirm(description.trim(), amountValue, selectedCategory.name)
                }
            }) { Text("Add", color = MaterialTheme.colorScheme.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Production Splitwise-standard international add expense dialog:
 * - Transaction currency selector
 * - Amount in transaction currency
 * - Live exchange rate display with refresh
 * - Real-time conversion preview to primary currency (INR)
 * - Optional custom rate override
 */
@Composable
private fun InternationalAddExpenseDialog(
    membersCount: Int,
    selectedCurrency: CurrencyInfo,
    exchangeRates: Map<String, Double>,
    isFetchingRates: Boolean,
    onSelectCurrency: (CurrencyInfo) -> Unit,
    onRefreshRates: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        amount: Double,
        category: String,
        currency: CurrencyInfo,
        customRate: Double?,
        notes: String?
    ) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var showCurrencyDropdown by remember { mutableStateOf(false) }
    var showCustomRateField by remember { mutableStateOf(false) }
    var customRateInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val isForeign = selectedCurrency.code != "INR"
    val liveRate = exchangeRates[selectedCurrency.code] ?: 1.0
    val activeRate = customRateInput.toDoubleOrNull()?.takeIf { it > 0.0 } ?: liveRate

    val enteredAmount = amount.toDoubleOrNull() ?: 0.0
    val convertedInr = enteredAmount * activeRate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Add Expense")
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Multi-Currency",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (e.g. Dinner, Taxi, Tickets)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                ) {
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Currency selector
                Text(
                    "Transaction Currency",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.radiusMd))
                            .clickable { showCurrencyDropdown = true }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.md, vertical = Dimens.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    selectedCurrency.flagEmoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        "${selectedCurrency.code} (${selectedCurrency.symbol})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        selectedCurrency.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = "Select currency",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCurrencyDropdown,
                        onDismissRequest = { showCurrencyDropdown = false }
                    ) {
                        SUPPORTED_CURRENCIES.forEach { curr ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(curr.flagEmoji, fontSize = 18.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("${curr.code} - ${curr.name} (${curr.symbol})")
                                    }
                                },
                                onClick = {
                                    onSelectCurrency(curr)
                                    showCurrencyDropdown = false
                                }
                            )
                        }
                    }
                }

                // Amount input in selected currency
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (${selectedCurrency.symbol})") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live rate & conversion preview (only if selected currency is foreign)
                if (isForeign) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(Dimens.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.CurrencyExchange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "1 ${selectedCurrency.code} = ₹${"%.2f".format(Locale.US, activeRate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = onRefreshRates,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        if (isFetchingRates) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "Refresh rate",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = { showCustomRateField = !showCustomRateField },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            if (showCustomRateField) "Default" else "Edit Rate",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            // Expandable custom exchange rate input
                            AnimatedVisibility(visible = showCustomRateField) {
                                OutlinedTextField(
                                    value = customRateInput,
                                    onValueChange = { customRateInput = it.filter { c -> c.isDigit() || c == '.' } },
                                    label = { Text("Custom Rate (₹ per 1 ${selectedCurrency.code})") },
                                    placeholder = { Text("%.2f".format(Locale.US, liveRate)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = Dimens.xs)
                                )
                            }

                            // Dynamic Live Preview in Primary Currency (INR)
                            if (enteredAmount > 0.0) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "≈ ₹${"%.2f".format(Locale.US, convertedInr)} INR",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Primary trip currency",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    "Split equally among all $membersCount member(s).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amountValue = amount.toDoubleOrNull()
                val customRateValue = customRateInput.toDoubleOrNull()
                if (description.isNotBlank() && amountValue != null && amountValue > 0) {
                    onConfirm(
                        description.trim(),
                        amountValue,
                        selectedCategory.name,
                        selectedCurrency,
                        customRateValue,
                        notesInput.takeIf { it.isNotBlank() }
                    )
                }
            }) { Text("Add Expense", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BalanceRow(balance: MemberBalance, modifier: Modifier = Modifier) {
    val net = balance.net
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(
            containerColor = if (net >= 0)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.md, vertical = Dimens.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    balance.profile.fullName ?: balance.profile.email ?: "Member",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Paid ₹${balance.paid.toLong()} · Fair share ₹${balance.fairShare.toLong()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (net >= 0) "gets back ₹${"%.0f".format(Locale.US, net)}"
                else "owes ₹${"%.0f".format(Locale.US, -net)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (net >= 0) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error
            )
        }
    }
}
