package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.klogviewer.domain.model.DirectoryIdentityNormalizer
import com.klogviewer.domain.model.DirectoryPatternMapping
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternDraft
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole
import com.klogviewer.ui.theme.KLogViewerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectoryMappingsDialog(
    mappings: Map<String, DirectoryPatternMapping>,
    isDarkMode: Boolean = true,
    onOpenInWizard: (String) -> Unit,
    onDeleteMapping: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        KLogViewerTheme(darkTheme = isDarkMode) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .width(760.dp)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Directory Pattern Mappings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "${mappings.size} saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Approved patterns are saved per directory in your preferences. " +
                            "When a file is opened from a mapped directory, its pattern is applied automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Content List
                    if (mappings.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved directory mappings yet.\n" +
                                        "Enable 'Save mapping for directory' when applying a pattern.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(mappings.values.toList(), key = { it.directoryKey }) { mapping ->
                                DirectoryMappingRow(
                                    mapping = mapping,
                                    onOpenInWizard = { onOpenInWizard(mapping.directoryKey) },
                                    onDelete = { onDeleteMapping(mapping.directoryKey) }
                                )
                            }
                        }
                    }

                    // Footer Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryMappingRow(
    mapping: DirectoryPatternMapping,
    onOpenInWizard: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Source Type Badge
            SourceTypeBadge(mapping.sourceType)

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Directory Path
                Text(
                    text = DirectoryIdentityNormalizer.formatDisplay(mapping.directoryKey),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Pattern summary & tokens
                val tokenSummary = mapping.patternDraft.segments
                    .filterIsInstance<PatternSegment.Token>()
                    .joinToString(" → ") { it.token.role.displayName }

                Text(
                    text = "${mapping.patternDraft.name}: $tokenSummary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Last used
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(mapping.lastUsedAt))
                Text(
                    text = "Last used: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenInWizard,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Open in Wizard",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Mapping",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceTypeBadge(sourceType: String) {
    val (bgColor, textColor) = when (sourceType.uppercase()) {
        "SFTP" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "S3" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = sourceType.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Preview
@Composable
fun DirectoryMappingsDialogPreview() {
    val sampleDraft = PatternDraft(
        name = "Logback Standard",
        segments = listOf(
            PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP)),
            PatternSegment.Delimiter(PatternDelimiter(value = " [")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
            PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL)),
            PatternSegment.Delimiter(PatternDelimiter(value = " - ")),
            PatternSegment.Token(PatternToken(role = PatternTokenRole.MESSAGE))
        )
    )

    val sampleMappings = mapOf(
        "local:/var/log/nginx" to DirectoryPatternMapping(
            directoryKey = "local:/var/log/nginx",
            patternDraft = sampleDraft,
            sourceType = "LOCAL",
            lastUsedAt = System.currentTimeMillis()
        ),
        "sftp:deploy@10.0.0.1:22/srv/logs" to DirectoryPatternMapping(
            directoryKey = "sftp:deploy@10.0.0.1:22/srv/logs",
            patternDraft = sampleDraft.copy(name = "Serilog Layout"),
            sourceType = "SFTP",
            lastUsedAt = System.currentTimeMillis() - 86400000L
        ),
        "s3:prod-logs-bucket/2026/08" to DirectoryPatternMapping(
            directoryKey = "s3:prod-logs-bucket/2026/08",
            patternDraft = sampleDraft.copy(name = "AWS CloudWatch Layout"),
            sourceType = "S3",
            lastUsedAt = System.currentTimeMillis() - 172800000L
        )
    )

    DirectoryMappingsDialog(
        mappings = sampleMappings,
        isDarkMode = true,
        onOpenInWizard = {},
        onDeleteMapping = {},
        onDismiss = {}
    )
}

@Preview
@Composable
fun DirectoryMappingsDialogEmptyPreview() {
    DirectoryMappingsDialog(
        mappings = emptyMap(),
        isDarkMode = true,
        onOpenInWizard = {},
        onDeleteMapping = {},
        onDismiss = {}
    )
}
