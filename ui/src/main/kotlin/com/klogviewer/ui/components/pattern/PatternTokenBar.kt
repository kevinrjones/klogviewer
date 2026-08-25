package com.klogviewer.ui.components.pattern

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klogviewer.domain.model.PatternDelimiter
import com.klogviewer.domain.model.PatternSegment
import com.klogviewer.domain.model.PatternToken
import com.klogviewer.domain.model.PatternTokenRole

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PatternTokenBar(
    segments: List<PatternSegment>,
    hoveredSegmentId: String?,
    focusedTokenId: String?,
    isDarkMode: Boolean,
    onTokenClick: (PatternToken) -> Unit,
    onSegmentHovered: (String?) -> Unit,
    onRemoveSegment: (String) -> Unit,
    onReorderSegment: (String, Boolean) -> Unit,
    onDelimiterUpdated: (String, String) -> Unit,
    onAddToken: (Int, PatternTokenRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAddMenuExpanded by remember { mutableStateOf(false) }
    var addMenuTargetIndex by remember { mutableStateOf(0) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        FlowRow(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.forEachIndexed { index, segment ->
                val isHovered = hoveredSegmentId == segment.id
                val isFocused = when (segment) {
                    is PatternSegment.Token -> focusedTokenId == segment.token.id
                    else -> false
                }

                when (segment) {
                    is PatternSegment.Token -> {
                        PatternTokenPill(
                            token = segment.token,
                            isHovered = isHovered,
                            isFocused = isFocused,
                            isDarkMode = isDarkMode,
                            onClick = { onTokenClick(segment.token) },
                            onHoverChange = { hovered -> onSegmentHovered(if (hovered) segment.id else null) },
                            onRemove = { onRemoveSegment(segment.id) },
                            onMoveLeft = { onReorderSegment(segment.id, true) },
                            onMoveRight = { onReorderSegment(segment.id, false) }
                        )
                    }
                    is PatternSegment.Delimiter -> {
                        PatternDelimiterChip(
                            delimiter = segment.delimiter,
                            isHovered = isHovered,
                            isDarkMode = isDarkMode,
                            onValueChange = { newValue -> onDelimiterUpdated(segment.delimiter.id, newValue) },
                            onHoverChange = { hovered -> onSegmentHovered(if (hovered) segment.id else null) }
                        )
                    }
                }
            }

            // Add Token Button at the end
            Box {
                OutlinedIconButton(
                    onClick = {
                        addMenuTargetIndex = segments.size
                        isAddMenuExpanded = true
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Field",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isAddMenuExpanded,
                    onDismissRequest = { isAddMenuExpanded = false }
                ) {
                    PatternTokenRole.entries.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.displayName) },
                            onClick = {
                                onAddToken(addMenuTargetIndex, role)
                                isAddMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PatternTokenPill(
    token: PatternToken,
    isHovered: Boolean,
    isFocused: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onHoverChange: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isMouseHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(isMouseHovered) {
        onHoverChange(isMouseHovered)
    }

    val baseColor = PatternTheme.roleColor(token.role, isDarkMode)
    val bgColor = PatternTheme.roleBackgroundColor(token.role, isDarkMode)
    val borderColor = if (isHovered || isFocused) baseColor else baseColor.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .hoverable(interactionSource)
            .border(
                width = if (isHovered || isFocused) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = token.effectiveName,
                    style = TextStyle(color = baseColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )

                if (token.formatPattern.isNotBlank()) {
                    Text(
                        text = "(${token.formatPattern})",
                        style = TextStyle(color = baseColor.copy(alpha = 0.8f), fontSize = 10.sp)
                    )
                }
            }

            if (isMouseHovered || isFocused) {
                TokenPillActions(baseColor, onMoveLeft, onMoveRight, onRemove)
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun TokenPillActions(
    baseColor: Color,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit
) {
    IconButton(onClick = onMoveLeft, modifier = Modifier.size(16.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Move Left", tint = baseColor)
    }
    IconButton(onClick = onMoveRight, modifier = Modifier.size(16.dp)) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Move Right", tint = baseColor)
    }
    IconButton(onClick = onRemove, modifier = Modifier.size(16.dp)) {
        Icon(Icons.Default.Close, contentDescription = "Remove", tint = baseColor)
    }
}

@Composable
fun PatternDelimiterChip(
    delimiter: PatternDelimiter,
    isHovered: Boolean,
    isDarkMode: Boolean,
    onValueChange: (String) -> Unit,
    onHoverChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isMouseHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(isMouseHovered) {
        onHoverChange(isMouseHovered)
    }

    val color = PatternTheme.delimiterColor(isDarkMode)

    Surface(
        modifier = modifier
            .hoverable(interactionSource)
            .border(
                width = if (isHovered) 1.5.dp else 0.5.dp,
                color = if (isHovered) color else color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            ),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent
    ) {
        BasicTextField(
            value = delimiter.value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            singleLine = true,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}

@Preview
@Composable
fun PatternTokenBarPreview() {
    val sampleSegments = listOf(
        PatternSegment.Token(PatternToken(role = PatternTokenRole.TIMESTAMP, formatPattern = "yyyy-MM-dd")),
        PatternSegment.Delimiter(PatternDelimiter(value = " [")),
        PatternSegment.Token(PatternToken(role = PatternTokenRole.THREAD)),
        PatternSegment.Delimiter(PatternDelimiter(value = "] ")),
        PatternSegment.Token(PatternToken(role = PatternTokenRole.LEVEL))
    )

    MaterialTheme {
        Surface {
            PatternTokenBar(
                segments = sampleSegments,
                hoveredSegmentId = null,
                focusedTokenId = null,
                isDarkMode = true,
                onTokenClick = {},
                onSegmentHovered = {},
                onRemoveSegment = {},
                onReorderSegment = { _, _ -> },
                onDelimiterUpdated = { _, _ -> },
                onAddToken = { _, _ -> }
            )
        }
    }
}
