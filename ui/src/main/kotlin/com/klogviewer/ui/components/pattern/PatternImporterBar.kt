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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextButton

@Composable
fun PatternImporterBar(
    selectedPresetName: String,
    onPresetSelected: (String) -> Unit,
    onImportPattern: (String) -> Unit,
    isDirectoryPersistenceEnabled: Boolean,
    onDirectoryPersistenceToggled: (Boolean) -> Unit,
    onManageMappings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var pasteText by remember { mutableStateOf("") }
    var isPresetDropdownExpanded by remember { mutableStateOf(false) }

    val presetMap = mapOf(
        "Logback / Log4J Standard" to "%d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %-5level %logger - %msg",
        "Serilog Text Layout" to
            "{Timestamp:yyyy-MM-dd HH:mm:ss.SSS} [{Level}] [{ThreadId}] {SourceContext} - {Message}",
        "ISO8601 Simple" to "%d{yyyy-MM-ddTHH:mm:ss} %level %logger - %msg",
        "Custom Draft" to "%d{yyyy-MM-dd HH:mm:ss} %level [%t] %logger - %msg"
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
                    text = "Preset Pattern: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { isPresetDropdownExpanded = true }) {
                    Text(selectedPresetName.ifBlank { "Select Preset..." })
                }
                DropdownMenu(
                    expanded = isPresetDropdownExpanded,
                    onDismissRequest = { isPresetDropdownExpanded = false }
                ) {
                    presetMap.forEach { (presetName, formatStr) ->
                        DropdownMenuItem(
                            text = { Text(presetName) },
                            onClick = {
                                pasteText = formatStr
                                onPresetSelected(presetName)
                                isPresetDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Persistence Toggle & Manage Link
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
                if (onManageMappings != null) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = onManageMappings) {
                        Text("Manage...", style = MaterialTheme.typography.labelSmall)
                    }
                }
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
                label = { Text("Paste Pattern String (Logback / Log4J / Serilog)") },
                placeholder = {
                    Text("e.g. %d{yyyy-MM-dd} [%t] %-5level %logger - %msg OR {Timestamp} [{Level}] {Message}")
                },
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
