package com.example.tripmate.ui.screens.itinerary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.tripmate.model.DocumentCategory
import com.example.tripmate.model.DocumentFileType
import com.example.tripmate.model.TripDocument
import com.example.tripmate.model.TripPlan
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.util.TripDocumentFileManager
import java.io.File

/**
 * Full Documents & Reservations Tab (TripIt Standard)
 */
@Composable
fun DocumentsReservationsSectionView(
    plan: TripPlan,
    onAddDocument: (TripDocument) -> Unit,
    onUpdateDocument: (TripDocument) -> Unit,
    onDeleteDocument: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDoc by remember { mutableStateOf<TripDocument?>(null) }
    var previewImageDoc by remember { mutableStateOf<TripDocument?>(null) }
    var docToDelete by remember { mutableStateOf<TripDocument?>(null) }

    var selectedCategoryFilter by remember { mutableStateOf<DocumentCategory?>(null) }
    var selectedDayFilter by remember { mutableStateOf<Int?>(null) } // null = All, -1 = Trip Level, 0..N-1 = Day
    var searchQuery by remember { mutableStateOf("") }

    val documents = plan.documents

    val filteredDocs = remember(documents, selectedCategoryFilter, selectedDayFilter, searchQuery) {
        documents.filter { doc ->
            val matchCategory = selectedCategoryFilter == null || doc.category == selectedCategoryFilter
            val matchDay = when (selectedDayFilter) {
                null -> true
                -1 -> doc.dayIndex == null
                else -> doc.dayIndex == selectedDayFilter
            }
            val q = searchQuery.trim().lowercase()
            val matchQuery = q.isEmpty() ||
                doc.title.lowercase().contains(q) ||
                (doc.confirmationNumber?.lowercase()?.contains(q) == true) ||
                (doc.provider?.lowercase()?.contains(q) == true) ||
                (doc.linkedItemTitle?.lowercase()?.contains(q) == true) ||
                (doc.notes?.lowercase()?.contains(q) == true)

            matchCategory && matchDay && matchQuery
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.md)
    ) {
        // Section Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.radiusLg),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.md),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Documents & Reservations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(Dimens.xs))
                            Surface(
                                shape = RoundedCornerShape(Dimens.radiusFull),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "${documents.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Flight passes, hotel bookings, tickets & confirmations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(Dimens.radiusFull),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Reservation",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search confirmation #, flight, hotel, or pass…",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(Dimens.radiusMd),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All (${documents.size})", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(Dimens.radiusFull)
                        )
                    }
                    items(DocumentCategory.entries) { cat ->
                        val count = documents.count { it.category == cat }
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = {
                                selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                            },
                            label = {
                                Text(
                                    "${cat.label.substringBefore(" /")} ($count)",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = categoryIcon(cat),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(Dimens.radiusFull)
                        )
                    }
                }

                // Day Filter Chips
                if (plan.days.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs),
                        contentPadding = PaddingValues(bottom = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDayFilter == null,
                                onClick = { selectedDayFilter = null },
                                label = { Text("All Days", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedDayFilter == -1,
                                onClick = { selectedDayFilter = if (selectedDayFilter == -1) null else -1 },
                                label = { Text("Entire Trip", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            )
                        }
                        items(plan.days.indices.toList()) { dayIdx ->
                            val day = plan.days[dayIdx]
                            FilterChip(
                                selected = selectedDayFilter == dayIdx,
                                onClick = { selectedDayFilter = if (selectedDayFilter == dayIdx) null else dayIdx },
                                label = { Text("Day ${day.dayNumber}", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            )
                        }
                    }
                }
            }
        }

        // Content List
        if (documents.isEmpty()) {
            EmptyDocumentsPlaceholder(onAddClick = { showAddDialog = true })
        } else if (filteredDocs.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusMd),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(Dimens.sm))
                    Text(
                        text = "No reservations match your filter",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Dimens.xs))
                    TextButton(onClick = {
                        selectedCategoryFilter = null
                        selectedDayFilter = null
                        searchQuery = ""
                    }) {
                        Text("Reset Filters", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.sm)
            ) {
                filteredDocs.forEach { doc ->
                    TripDocumentCard(
                        doc = doc,
                        onEdit = { editingDoc = doc },
                        onDelete = { docToDelete = doc },
                        onViewImage = { previewImageDoc = doc },
                        onOpenPdf = {
                            doc.fileUri?.let { path ->
                                val res = TripDocumentFileManager.openPdfFile(context, path)
                                if (res.isFailure) {
                                    Toast.makeText(
                                        context,
                                        "Could not open PDF: ${res.exceptionOrNull()?.localizedMessage ?: "No PDF viewer found"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Document Dialog
    if (showAddDialog) {
        AddEditDocumentDialog(
            plan = plan,
            existingDocument = null,
            onDismiss = { showAddDialog = false },
            onSave = { newDoc ->
                onAddDocument(newDoc)
                showAddDialog = false
            }
        )
    }

    // Edit Document Dialog
    editingDoc?.let { doc ->
        AddEditDocumentDialog(
            plan = plan,
            existingDocument = doc,
            onDismiss = { editingDoc = null },
            onSave = { updated ->
                onUpdateDocument(updated)
                editingDoc = null
            }
        )
    }

    // Delete Confirmation Dialog
    docToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Delete Reservation?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Are you sure you want to delete \"${doc.title}\"? Any attached files will also be removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteDocument(doc.id)
                        docToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Image Preview Dialog
    previewImageDoc?.let { doc ->
        DocumentImagePreviewDialog(
            doc = doc,
            onDismiss = { previewImageDoc = null }
        )
    }
}

/**
 * Individual Reservation Card
 */
@Composable
fun TripDocumentCard(
    doc: TripDocument,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewImage: () -> Unit,
    onOpenPdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categoryColor = categoryColor(doc.category)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMd),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.md),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            // Header Row: Category Badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    color = categoryColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = categoryIcon(doc.category),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = doc.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                }

                // Edit / Delete Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Title & Provider
            Column {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!doc.provider.isNullOrBlank()) {
                    Text(
                        text = doc.provider,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!doc.dateTimeLabel.isNullOrBlank()) {
                    Text(
                        text = "📅 ${doc.dateTimeLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Confirmation Number with One-Tap Copy
            if (!doc.confirmationNumber.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Confirmation Number", doc.confirmationNumber)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            "Copied confirmation code: ${doc.confirmationNumber}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.sm, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CONF # ${doc.confirmationNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Linkage Badge: Linked to Day or Stop
            if (doc.linkedItemTitle != null || doc.dayLabel != null) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusFull),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                append("Linked to: ")
                                if (doc.dayLabel != null) append(doc.dayLabel)
                                if (doc.linkedItemTitle != null) {
                                    if (doc.dayLabel != null) append(" • ")
                                    append(doc.linkedItemTitle)
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Attached File Section (PDF / Image)
            if (doc.fileUri != null && doc.fileType != DocumentFileType.NONE) {
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusSm),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.sm, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (doc.fileType == DocumentFileType.PDF) Icons.Filled.PictureAsPdf else Icons.Filled.Image,
                                contentDescription = null,
                                tint = if (doc.fileType == DocumentFileType.PDF) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(Dimens.xs))
                            Column {
                                Text(
                                    text = doc.fileName ?: if (doc.fileType == DocumentFileType.PDF) "Document.pdf" else "Attachment.jpg",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                doc.fileSizeBytes?.let { bytes ->
                                    Text(
                                        text = TripDocumentFileManager.formatFileSize(bytes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (doc.fileType == DocumentFileType.PDF) onOpenPdf()
                                else onViewImage()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(Dimens.radiusFull),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (doc.fileType == DocumentFileType.PDF) "Open PDF" else "View Image",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Notes
            if (!doc.notes.isNullOrBlank()) {
                Text(
                    text = doc.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Empty state card when no documents are attached yet.
 */
@Composable
private fun EmptyDocumentsPlaceholder(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusLg),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ConfirmationNumber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.height(Dimens.md))

            Text(
                text = "Keep All Bookings In One Place",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(Dimens.xs))

            Text(
                text = "Attach boarding passes, hotel confirmations, train tickets, and museum passes. Link them directly to specific days or stops in your itinerary.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = Dimens.md)
            )

            Spacer(Modifier.height(Dimens.lg))

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(Dimens.radiusFull),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Add Reservation or Pass",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Add / Edit Document Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDocumentDialog(
    plan: TripPlan,
    existingDocument: TripDocument?,
    onDismiss: () -> Unit,
    onSave: (TripDocument) -> Unit
) {
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf(existingDocument?.category ?: DocumentCategory.FLIGHT) }
    var title by remember { mutableStateOf(existingDocument?.title ?: "") }
    var confirmationNumber by remember { mutableStateOf(existingDocument?.confirmationNumber ?: "") }
    var provider by remember { mutableStateOf(existingDocument?.provider ?: "") }
    var dateTimeLabel by remember { mutableStateOf(existingDocument?.dateTimeLabel ?: "") }
    var notes by remember { mutableStateOf(existingDocument?.notes ?: "") }

    var selectedDayIndex by remember { mutableStateOf<Int?>(existingDocument?.dayIndex) }
    var selectedItemId by remember { mutableStateOf<String?>(existingDocument?.linkedItemId) }

    var attachedFilePath by remember { mutableStateOf<String?>(existingDocument?.fileUri) }
    var attachedFileName by remember { mutableStateOf<String?>(existingDocument?.fileName) }
    var attachedFileType by remember { mutableStateOf(existingDocument?.fileType ?: DocumentFileType.NONE) }
    var attachedFileSize by remember { mutableStateOf<Long?>(existingDocument?.fileSizeBytes) }

    var titleError by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val docId = existingDocument?.id ?: java.util.UUID.randomUUID().toString()
            val result = TripDocumentFileManager.copyPickedFile(
                context = context,
                sourceUri = uri,
                tripId = plan.id,
                docId = docId
            )
            if (result != null) {
                attachedFilePath = result.filePath
                attachedFileName = result.fileName
                attachedFileType = result.fileType
                attachedFileSize = result.fileSizeBytes
            } else {
                Toast.makeText(context, "Could not attach file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 680.dp)
                .padding(vertical = Dimens.md),
            shape = RoundedCornerShape(Dimens.radiusLg),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.lg)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingDocument == null) "Add Reservation / Pass" else "Edit Reservation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(Modifier.height(Dimens.sm))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.sm)
                ) {
                    // Category Selection
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        items(DocumentCategory.entries) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text("Title *") },
                        placeholder = {
                            Text(
                                when (selectedCategory) {
                                    DocumentCategory.FLIGHT -> "e.g. Flight UA 881 SFO to NRT"
                                    DocumentCategory.HOTEL -> "e.g. Grand Hyatt Tokyo Booking"
                                    DocumentCategory.TRAIN -> "e.g. Shinkansen Tokyo to Kyoto"
                                    DocumentCategory.ACTIVITY_PASS -> "e.g. TeamLab Planets Pass"
                                    DocumentCategory.CAR_RENTAL -> "e.g. Toyota Rent-a-Car"
                                    DocumentCategory.RESTAURANT -> "e.g. Sukiyabashi Jiro Dinner"
                                    DocumentCategory.GENERAL -> "e.g. Travel Insurance Policy"
                                }
                            )
                        },
                        isError = titleError,
                        supportingText = if (titleError) { { Text("Title is required") } } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Confirmation / PNR Code
                    OutlinedTextField(
                        value = confirmationNumber,
                        onValueChange = { confirmationNumber = it },
                        label = { Text("Confirmation / Booking Reference #") },
                        placeholder = { Text("e.g. BK-98214 or PNR: 7XYZ99") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.ConfirmationNumber, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Provider / Carrier
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Provider / Carrier / Agency") },
                        placeholder = { Text("e.g. United Airlines, Marriott, JR East") },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Date / Time Label
                    OutlinedTextField(
                        value = dateTimeLabel,
                        onValueChange = { dateTimeLabel = it },
                        label = { Text("Date & Time") },
                        placeholder = { Text("e.g. Oct 12, 10:30 AM or 3:00 PM Check-in") },
                        singleLine = true,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Day Linker
                    Text(
                        text = "Link to Day or Stop",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = Dimens.xs)
                    )

                    // Day selector chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDayIndex == null,
                                onClick = {
                                    selectedDayIndex = null
                                    selectedItemId = null
                                },
                                label = { Text("Entire Trip", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            )
                        }
                        items(plan.days.indices.toList()) { dayIdx ->
                            val day = plan.days[dayIdx]
                            FilterChip(
                                selected = selectedDayIndex == dayIdx,
                                onClick = {
                                    selectedDayIndex = dayIdx
                                    selectedItemId = null
                                },
                                label = { Text("Day ${day.dayNumber}", style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(Dimens.radiusFull)
                            )
                        }
                    }

                    // Activity/Stop linker (if day selected)
                    if (selectedDayIndex != null && selectedDayIndex in plan.days.indices) {
                        val currentDay = plan.days[selectedDayIndex!!]
                        if (currentDay.items.isNotEmpty()) {
                            Text(
                                text = "Specific Activity / Stop (Optional)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.xs)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedItemId == null,
                                        onClick = { selectedItemId = null },
                                        label = { Text("Day-level Only", style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(Dimens.radiusFull)
                                    )
                                }
                                items(currentDay.items) { item ->
                                    FilterChip(
                                        selected = selectedItemId == item.id,
                                        onClick = { selectedItemId = item.id },
                                        label = {
                                            Text(
                                                item.title,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        shape = RoundedCornerShape(Dimens.radiusFull)
                                    )
                                }
                            }
                        }
                    }

                    // File Attachment Picker
                    Text(
                        text = "Attached Document (PDF or Image)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = Dimens.xs)
                    )

                    if (attachedFilePath != null && attachedFileType != DocumentFileType.NONE) {
                        Surface(
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Dimens.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (attachedFileType == DocumentFileType.PDF) Icons.Filled.PictureAsPdf else Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = if (attachedFileType == DocumentFileType.PDF) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(Dimens.xs))
                                    Column {
                                        Text(
                                            text = attachedFileName ?: "Attached File",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        attachedFileSize?.let {
                                            Text(
                                                text = TripDocumentFileManager.formatFileSize(it),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = {
                                    attachedFilePath = null
                                    attachedFileName = null
                                    attachedFileType = DocumentFileType.NONE
                                    attachedFileSize = null
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Remove file",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                            },
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Filled.AttachFile, contentDescription = null)
                            Spacer(Modifier.width(Dimens.xs))
                            Text("Attach Boarding Pass or Ticket (PDF / Image)")
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes & Instructions") },
                        placeholder = { Text("e.g. Terminal 2, Gate 42, breakfast included, bring passport") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(Dimens.md))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(Dimens.sm))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                titleError = true
                                return@Button
                            }

                            val dayLabel = selectedDayIndex?.let { idx ->
                                plan.days.getOrNull(idx)?.let { "Day ${it.dayNumber} (${it.dateLabel})" }
                            }
                            val linkedItemTitle = if (selectedDayIndex != null && selectedItemId != null) {
                                plan.days.getOrNull(selectedDayIndex!!)?.items?.firstOrNull { it.id == selectedItemId }?.title
                            } else null

                            val doc = TripDocument(
                                id = existingDocument?.id ?: java.util.UUID.randomUUID().toString(),
                                tripId = plan.id,
                                title = title.trim(),
                                category = selectedCategory,
                                confirmationNumber = confirmationNumber.trim().takeIf { it.isNotBlank() },
                                provider = provider.trim().takeIf { it.isNotBlank() },
                                dayIndex = selectedDayIndex,
                                dayLabel = dayLabel,
                                linkedItemId = selectedItemId,
                                linkedItemTitle = linkedItemTitle,
                                fileUri = attachedFilePath,
                                fileType = attachedFileType,
                                fileName = attachedFileName,
                                fileSizeBytes = attachedFileSize,
                                dateTimeLabel = dateTimeLabel.trim().takeIf { it.isNotBlank() },
                                notes = notes.trim().takeIf { it.isNotBlank() },
                                createdAt = existingDocument?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(doc)
                        },
                        shape = RoundedCornerShape(Dimens.radiusFull)
                    ) {
                        Text(if (existingDocument == null) "Save Reservation" else "Update")
                    }
                }
            }
        }
    }
}

/**
 * Image viewer dialog for boarding passes and photo tickets.
 */
@Composable
fun DocumentImagePreviewDialog(
    doc: TripDocument,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.md)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!doc.confirmationNumber.isNullOrBlank()) {
                            Text(
                                text = "CONF # ${doc.confirmationNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(Dimens.md))

                // Image Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val file = doc.fileUri?.let { File(it) }
                    if (file != null && file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = doc.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(Dimens.radiusMd)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "Image file not found on device.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Copy Confirmation Code Button if available
                if (!doc.confirmationNumber.isNullOrBlank()) {
                    Spacer(Modifier.height(Dimens.md))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Confirmation Number", doc.confirmationNumber)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied ${doc.confirmationNumber}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(Dimens.xs))
                        Text("Copy Confirmation # (${doc.confirmationNumber})")
                    }
                }
            }
        }
    }
}

/**
 * Returns a category-specific icon.
 */
fun categoryIcon(category: DocumentCategory): ImageVector = when (category) {
    DocumentCategory.FLIGHT -> Icons.Filled.Flight
    DocumentCategory.HOTEL -> Icons.Filled.Hotel
    DocumentCategory.TRAIN -> Icons.Filled.Train
    DocumentCategory.ACTIVITY_PASS -> Icons.Filled.ConfirmationNumber
    DocumentCategory.CAR_RENTAL -> Icons.Filled.DirectionsCar
    DocumentCategory.RESTAURANT -> Icons.Filled.Restaurant
    DocumentCategory.GENERAL -> Icons.Filled.Description
}

/**
 * Returns a vibrant, category-specific color.
 */
@Composable
fun categoryColor(category: DocumentCategory): Color = when (category) {
    DocumentCategory.FLIGHT -> Color(0xFF0284C7)       // Sky blue
    DocumentCategory.HOTEL -> Color(0xFF8B5CF6)        // Purple
    DocumentCategory.TRAIN -> Color(0xFFF59E0B)        // Amber
    DocumentCategory.ACTIVITY_PASS -> Color(0xFF10B981) // Emerald
    DocumentCategory.CAR_RENTAL -> Color(0xFFEC4899)    // Pink
    DocumentCategory.RESTAURANT -> Color(0xFFF97316)    // Orange
    DocumentCategory.GENERAL -> MaterialTheme.colorScheme.primary
}
