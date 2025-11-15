package killua.dev.base.ui.Components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import killua.dev.base.ui.Tokens.SizeTokens


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropDownMenuWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    choice: Int,
    data: List<String>,
    onChoiceChange: (Int) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    val heights by remember {
        mutableStateOf(Array(data.size) { 0 })
    }

    fun updateHeights(index: Int, height: Int) {
        heights[index] = height
    }

    var containerHeight by remember {
        mutableStateOf(0)
    }

    val containerHeightApply by animateIntAsState(targetValue = if (expanded) containerHeight else 0)

    val offsetY by animateIntAsState(targetValue = run {
        val sumHeights = heights.sum()
        var y = 0
        if (sumHeights < containerHeight) {
            if (choice > 0) {
                for (i in 0 until choice) {
                    y -= heights[i]
                }
            }
            y -= heights[choice] / 2
            y -= (containerHeight - sumHeights) / 2
        }
        y
    })

    BaseWidget(
        icon = icon,
        title = title,
        description = description,
        enabled = enabled,
        onClick = {
            expanded = !expanded
        },
        foreContent = {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
            ) {
                DropdownMenu(
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            containerHeight = placeable.height
                            layout(placeable.width, containerHeightApply) {
                                placeable.placeRelative(0, 0)
                            }
                        },
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(
                        x = 0.dp,
                        y = with(LocalDensity.current) {
                            offsetY.toDp()
                        }
                    )
                ) {
                    data.forEachIndexed { index, item ->
                        val backgroundColor =
                            if (index == choice) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        DropdownMenuItem(
                            modifier = Modifier
                                .background(backgroundColor)
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    val height = placeable.height
                                    updateHeights(index, height)
                                    layout(placeable.width, height) {
                                        placeable.placeRelative(0, 0)
                                    }
                                },
                            text = { Text(text = item) },
                            onClick = {
                                onChoiceChange(index)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    ) {
    }
}

@Composable
fun BaseWidget(
    icon: ImageVector? = null,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    foreContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SizeTokens.Level80)
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = SizeTokens.Level24, vertical = SizeTokens.Level16),
        horizontalArrangement = Arrangement.spacedBy(SizeTokens.Level16),
        verticalAlignment = Alignment.CenterVertically
    ) {


        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    style = MaterialTheme.typography.titleLarge,
                )
                description?.let {
                    Text(
                        text = it,
                        color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            foreContent()
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
        ) {
            content()
        }

        Spacer(modifier = Modifier.weight(1f))

        icon?.let {
            Icon(
                modifier = Modifier
                    .size(SizeTokens.Level36),
                imageVector = it,
                contentDescription = null,
            )
        }
    }
}
