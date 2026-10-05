package com.example.tripmate.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tripmate.model.AssistantMapRoute

/**
 * Backward compatibility alias delegating to InteractiveAssistantMap.
 */
@Composable
fun AssistantBottomMapRoute(
    route: AssistantMapRoute,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    InteractiveAssistantMap(
        route = route,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onDismiss = onDismiss,
        modifier = modifier
    )
}
