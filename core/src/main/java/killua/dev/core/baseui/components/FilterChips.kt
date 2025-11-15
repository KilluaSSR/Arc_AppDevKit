package killua.dev.base.ui.Components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

interface FilterContentType {
    val labelStringResId: Int
}

/**
 * @param T 过滤项的唯一标识类型
 * @param id 唯一标识符
 * @param label 显示标签
 * @param icon 可选的图标
 * @param count 可选的计数显示
 * @param isEnabled 是否启用
 */
data class FilterChipItem<T>(
    val id: T,
    val label: String,
    val icon: ImageVector? = null,
    val count: Int? = null,
    val isEnabled: Boolean = true
)

/**
 * 状态管理
 */
class FilterChipsState<T> {
    private val _selectedItems = mutableStateOf<Set<T>>(emptySet())
    val selectedItems: Set<T> get() = _selectedItems.value
    
    private val _isExpanded = mutableStateOf(false)
    val isExpanded: Boolean get() = _isExpanded.value
    
    fun selectItem(itemId: T) {
        _selectedItems.value = _selectedItems.value.toMutableSet().apply {
            if (contains(itemId)) {
                remove(itemId)
            } else {
                add(itemId)
            }
        }
    }
    
    fun clearSelection() {
        _selectedItems.value = emptySet()
    }
    
    fun setSelection(items: Set<T>) {
        _selectedItems.value = items
    }
    
    fun toggleExpanded() {
        _isExpanded.value = !_isExpanded.value
    }
    
    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }
}

/**
 *
 * @param items 过滤项列表
 * @param state 状态管理对象
 * @param onSelectionChanged 选择变化回调
 * @param modifier 修饰符
 * @param maxVisibleItems 最大可见项目数（超过此数量会显示展开/收起按钮）
 * @param showClearAll 是否显示清除所有按钮
 * @param showCount 是否显示计数
 * @param singleSelection 是否单选模式
 * @param chipStyle 样式配置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterChips(
    items: List<FilterChipItem<T>>,
    state: FilterChipsState<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 5,
    showClearAll: Boolean = true,
    showCount: Boolean = true,
    singleSelection: Boolean = false,
    chipStyle: FilterChipStyle = FilterChipStyle()
) {
    val selectedItems by remember { derivedStateOf { state.selectedItems } }
    val isExpanded by remember { derivedStateOf { state.isExpanded } }
    
    LaunchedEffect(selectedItems) {
        onSelectionChanged(selectedItems)
    }
    
    val visibleItems = remember(items, isExpanded, maxVisibleItems) {
        if (isExpanded || items.size <= maxVisibleItems) {
            items
        } else {
            items.take(maxVisibleItems)
        }
    }
    
    val hasMoreItems = items.size > maxVisibleItems
    val resolvedStyle = chipStyle.toComposableStyle()
    
    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(visibleItems) { item ->
                FilterChipItem(
                    item = item,
                    isSelected = selectedItems.contains(item.id),
                    isEnabled = item.isEnabled,
                    showCount = showCount,
                    singleSelection = singleSelection,
                    style = resolvedStyle,
                    onClick = {
                        if (singleSelection) {
                            state.setSelection(setOf(item.id))
                        } else {
                            state.selectItem(item.id)
                        }
                    }
                )
            }
            
            if (hasMoreItems) {
                item {
                    ExpandToggleChip(
                        isExpanded = isExpanded,
                        onClick = { state.toggleExpanded() }
                    )
                }
            }
            
            if (showClearAll && selectedItems.isNotEmpty()) {
                item {
                    ClearAllChip(
                        onClick = { state.clearSelection() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> FilterChipItem(
    item: FilterChipItem<T>,
    isSelected: Boolean,
    isEnabled: Boolean,
    showCount: Boolean,
    singleSelection: Boolean,
    style: FilterChipStyle,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(200),
        label = "chip_scale"
    )
    
    val animatedVisibility by remember(isSelected) {
        derivedStateOf { isSelected }
    }
    
    FilterChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) style.selectedIconColor else style.unselectedIconColor
                    )
                }
                
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isSelected) style.selectedTextColor else style.unselectedTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (showCount && item.count != null) {
                    Text(
                        text = "(${item.count})",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) style.selectedTextColor.copy(alpha = 0.7f) 
                               else style.unselectedTextColor.copy(alpha = 0.7f)
                    )
                }
            }
        },
        selected = isSelected,
        enabled = isEnabled,
        leadingIcon = if (isSelected && !singleSelection) {
            {
                AnimatedVisibility(
                    visible = animatedVisibility,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = style.selectedIconColor
                    )
                }
            }
        } else null,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(style.cornerRadius)),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = if (isSelected) style.selectedContainerColor else style.unselectedContainerColor,
            selectedContainerColor = style.selectedContainerColor,
            labelColor = if (isSelected) style.selectedTextColor else style.unselectedTextColor,
            selectedLabelColor = style.selectedTextColor,
            iconColor = if (isSelected) style.selectedIconColor else style.unselectedIconColor,
            selectedLeadingIconColor = style.selectedIconColor,
            disabledContainerColor = style.disabledContainerColor,
            disabledLabelColor = style.disabledTextColor,
            disabledLeadingIconColor = style.disabledIconColor
        ),
        border = if (style.showBorder) {
            FilterChipDefaults.filterChipBorder(
                enabled = isEnabled,
                selected = isSelected,
                borderColor = if (isSelected) style.selectedBorderColor else style.unselectedBorderColor,
                selectedBorderColor = style.selectedBorderColor
            )
        } else null
    )
}

@Composable
private fun ExpandToggleChip(
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isExpanded) "收起" else "更多",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ClearAllChip(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "清除全部",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

data class FilterChipStyle(
    val selectedContainerColor: Color = Color.Unspecified,
    val unselectedContainerColor: Color = Color.Unspecified,
    val selectedTextColor: Color = Color.Unspecified,
    val unselectedTextColor: Color = Color.Unspecified,
    val selectedIconColor: Color = Color.Unspecified,
    val unselectedIconColor: Color = Color.Unspecified,
    val selectedBorderColor: Color = Color.Unspecified,
    val unselectedBorderColor: Color = Color.Unspecified,
    val disabledContainerColor: Color = Color.Unspecified,
    val disabledTextColor: Color = Color.Unspecified,
    val disabledIconColor: Color = Color.Unspecified,
    val cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    val showBorder: Boolean = false
) {
    @Composable
    fun toComposableStyle(): FilterChipStyle {
        val colorScheme = MaterialTheme.colorScheme
        return copy(
            selectedContainerColor = if (selectedContainerColor == Color.Unspecified) colorScheme.primary else selectedContainerColor,
            unselectedContainerColor = if (unselectedContainerColor == Color.Unspecified) colorScheme.surfaceVariant else unselectedContainerColor,
            selectedTextColor = if (selectedTextColor == Color.Unspecified) colorScheme.onPrimary else selectedTextColor,
            unselectedTextColor = if (unselectedTextColor == Color.Unspecified) colorScheme.onSurfaceVariant else unselectedTextColor,
            selectedIconColor = if (selectedIconColor == Color.Unspecified) colorScheme.onPrimary else selectedIconColor,
            unselectedIconColor = if (unselectedIconColor == Color.Unspecified) colorScheme.onSurfaceVariant else unselectedIconColor,
            selectedBorderColor = if (selectedBorderColor == Color.Unspecified) colorScheme.primary else selectedBorderColor,
            unselectedBorderColor = if (unselectedBorderColor == Color.Unspecified) colorScheme.outline else unselectedBorderColor,
            disabledContainerColor = if (disabledContainerColor == Color.Unspecified) colorScheme.surfaceVariant.copy(alpha = 0.5f) else disabledContainerColor,
            disabledTextColor = if (disabledTextColor == Color.Unspecified) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else disabledTextColor,
            disabledIconColor = if (disabledIconColor == Color.Unspecified) colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else disabledIconColor
        )
    }
}


@Composable
fun <T> rememberFilterChipsState(): FilterChipsState<T> {
    return remember { FilterChipsState() }
}

@Composable
fun <T> SearchableFilterChips(
    items: List<FilterChipItem<T>>,
    state: FilterChipsState<T>,
    onSelectionChanged: (Set<T>) -> Unit,
    searchQuery: String = "",
    onSearchQueryChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    maxVisibleItems: Int = 5,
    showClearAll: Boolean = true,
    showCount: Boolean = true,
    singleSelection: Boolean = false,
    chipStyle: FilterChipStyle = FilterChipStyle()
) {
    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) {
            items
        } else {
            items.filter { 
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.id.toString().contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    Column(modifier = modifier) {
        FilterChips(
            items = filteredItems,
            state = state,
            onSelectionChanged = onSelectionChanged,
            maxVisibleItems = maxVisibleItems,
            showClearAll = showClearAll,
            showCount = showCount,
            singleSelection = singleSelection,
            chipStyle = chipStyle
        )
    }
}


@Composable
fun <T : FilterContentType> ContentFilterChips(
    selectedType: T,
    onTypeChanged: (T) -> Unit,
    availableTypes: Map<T, Int>,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
    counts: Map<T, Int> = emptyMap(),
    chipStyle: FilterChipStyle = FilterChipStyle()
) {
    val filterState = rememberFilterChipsState<T>()
    val context = LocalContext.current

    LaunchedEffect(selectedType) {
        filterState.setSelection(setOf(selectedType))
    }

    val filterItems = remember(counts, availableTypes) {
        availableTypes.map { (type, _) ->
            FilterChipItem(
                id = type,
                label = context.getString(type.labelStringResId),
                count = counts[type]
            )
        }
    }

    FilterChips(
        items = filterItems,
        state = filterState,
        onSelectionChanged = { selectedItems ->
            val selectedType = selectedItems.firstOrNull() ?: availableTypes.keys.first()
            onTypeChanged(selectedType)
        },
        modifier = modifier,
        maxVisibleItems = 4,
        showClearAll = false,
        showCount = showCount,
        singleSelection = true,
        chipStyle = chipStyle
    )
}

@Composable
fun <T : FilterContentType> ContentFilterChipsWithGenericState(
    availableTypes: Map<T, Int>,
    onTypeChanged: (T) -> Unit,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
    counts: Map<T, Int> = emptyMap(),
    chipStyle: FilterChipStyle = FilterChipStyle(),
    initialType: T? = null
) {
    val defaultType = availableTypes.keys.firstOrNull()
        ?: throw IllegalArgumentException("availableTypes cannot be empty")

    var selectedType by rememberSaveable {
        mutableStateOf(initialType ?: defaultType)
    }

    ContentFilterChips(
        selectedType = selectedType,
        onTypeChanged = { newType ->
            selectedType = newType
            onTypeChanged(newType)
        },
        modifier = modifier,
        showCount = showCount,
        counts = counts,
        availableTypes = availableTypes,
        chipStyle = chipStyle
    )
}