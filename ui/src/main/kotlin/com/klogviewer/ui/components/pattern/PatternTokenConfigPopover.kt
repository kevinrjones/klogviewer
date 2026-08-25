package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole

@Composable
fun PatternTokenConfigPopover(
    token: PatternToken,
    onTokenUpdated: (PatternToken) -> Unit,
    onTokenDeleted: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember(token) { mutableStateOf(token.role) }
    var customPropertyName by remember(token) { mutableStateOf(token.customPropertyName ?: "") }
    var formatPattern by remember(token) { mutableStateOf(token.formatPattern) }
    var isOptional by remember(token) { mutableStateOf(token.isOptional) }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = modifier.width(360.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Configure Token",
                    style = MaterialTheme.typography.titleMedium
                )

                // Role Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Role:", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { isRoleDropdownExpanded = true }) {
                        Text(selectedRole.displayName)
                    }
                    DropdownMenu(
                        expanded = isRoleDropdownExpanded,
                        onDismissRequest = { isRoleDropdownExpanded = false }
                    ) {
                        PatternTokenRole.entries.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.displayName) },
                                onClick = {
                                    selectedRole = role
                                    isRoleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Custom Property Name (if CUSTOM_PROPERTY)
                if (selectedRole == PatternTokenRole.CUSTOM_PROPERTY) {
                    OutlinedTextField(
                        value = customPropertyName,
                        onValueChange = { customPropertyName = it },
                        label = { Text("Property Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Format Pattern (e.g. date format or custom pattern)
                OutlinedTextField(
                    value = formatPattern,
                    onValueChange = { formatPattern = it },
                    label = { Text(if (selectedRole == PatternTokenRole.TIMESTAMP) "Date/Time Format" else "Pattern / Format") },
                    placeholder = { Text(if (selectedRole == PatternTokenRole.TIMESTAMP) "yyyy-MM-dd HH:mm:ss.SSS" else "e.g. %-5level") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isOptional,
                        onCheckedChange = { isOptional = it }
                    )
                    Text(text = "Optional Field", style = MaterialTheme.typography.bodySmall)
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onTokenDeleted(token.id)
                            onDismissRequest()
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onTokenUpdated(
                                    token.copy(
                                        role = selectedRole,
                                        customPropertyName = if (selectedRole == PatternTokenRole.CUSTOM_PROPERTY) customPropertyName else null,
                                        formatPattern = formatPattern,
                                        isOptional = isOptional
                                    )
                                )
                                onDismissRequest()
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PatternTokenConfigPopoverPreview() {
    MaterialTheme {
        Surface {
            PatternTokenConfigPopover(
                token = PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd HH:mm:ss"),
                onTokenUpdated = {},
                onTokenDeleted = {},
                onDismissRequest = {}
            )
        }
    }
}
