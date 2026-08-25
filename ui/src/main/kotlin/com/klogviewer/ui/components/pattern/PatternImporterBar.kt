package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PatternImporterBar(
    selectedPresetName: String,
    onPresetSelected: (String) -> Unit,
    onImportPattern: (String) -> Unit,
    isDirectoryPersistenceEnabled: Boolean,
    onDirectoryPersistenceToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var pasteText by remember { mutableStateOf("") }
    var isPresetDropdownExpanded by remember { mutableStateOf(false) }

    val presets = listOf(
        "Logback / Log4J Standard",
        "Serilog Text Layout",
        "ISO8601 Simple",
        "Custom Draft"
    )

    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preset Dropdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Preset: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { isPresetDropdownExpanded = true }) {
                    Text(selectedPresetName.ifBlank { "Select Preset..." })
                }
                DropdownMenu(
                    expanded = isPresetDropdownExpanded,
                    onDismissRequest = { isPresetDropdownExpanded = false }
                ) {
                    presets.forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset) },
                            onClick = {
                                onPresetSelected(preset)
                                isPresetDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Persistence Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End
            ) {
                Checkbox(
                    checked = isDirectoryPersistenceEnabled,
                    onCheckedChange = onDirectoryPersistenceToggled
                )
                Text(
                    text = "Save mapping for directory",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Paste Field & Import Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = pasteText,
                onValueChange = { pasteText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Paste pattern string (e.g. %d{yyyy-MM-dd} [%t] %-5level - %msg)") },
                singleLine = true
            )
            Button(
                onClick = {
                    if (pasteText.isNotBlank()) {
                        onImportPattern(pasteText)
                    }
                },
                enabled = pasteText.isNotBlank()
            ) {
                Text("Import")
            }
        }
    }
}

@Preview
@Composable
fun PatternImporterBarPreview() {
    MaterialTheme {
        Surface {
            PatternImporterBar(
                selectedPresetName = "Logback / Log4J Standard",
                onPresetSelected = {},
                onImportPattern = {},
                isDirectoryPersistenceEnabled = true,
                onDirectoryPersistenceToggled = {}
            )
        }
    }
}
