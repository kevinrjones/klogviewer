package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import com.klogviewer.domain.model.PatternTokenRole

@Composable
fun SampleLineSelectionPopup(
    selectedText: String,
    selectedRange: IntRange,
    onExtractAsToken: (IntRange, PatternTokenRole, String?) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf(PatternTokenRole.CUSTOM_PROPERTY) }
    var customName by remember { mutableStateOf("extractedField") }
    var isRoleDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            modifier = modifier.width(320.dp).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Extract Selected Text",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "\"$selectedText\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Role:", style = MaterialTheme.typography.bodyMedium)
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

                if (selectedRole == PatternTokenRole.CUSTOM_PROPERTY) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Property Name") },
                        singleLine = true
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onExtractAsToken(
                                selectedRange,
                                selectedRole,
                                if (selectedRole == PatternTokenRole.CUSTOM_PROPERTY) customName else null
                            )
                            onDismissRequest()
                        }
                    ) {
                        Text("Extract")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun SampleLineSelectionPopupPreview() {
    MaterialTheme {
        Surface {
            SampleLineSelectionPopup(
                selectedText = "tx-9921",
                selectedRange = 10..17,
                onExtractAsToken = { _, _, _ -> },
                onDismissRequest = {}
            )
        }
    }
}
