package killua.dev.core.log

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val logs by LogcatCaptureService.logs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf("全部") }
    var autoScroll by remember { mutableStateOf(true) }
    var expandedLog by remember { mutableStateOf<LogEntry?>(null) }
    
    val filteredLogs = remember(logs, searchQuery, selectedPriority) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isEmpty() || 
                log.message.contains(searchQuery, ignoreCase = true) ||
                log.tag.contains(searchQuery, ignoreCase = true)
            
            val matchesPriority = selectedPriority == "全部" || log.priority == selectedPriority
            
            matchesSearch && matchesPriority
        }
    }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 自动滚动到底部
    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }
    
    LaunchedEffect(Unit) {
        if (!LogcatCaptureService.isCapturing()) {
            LogcatCaptureService.startCapture()
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // 可选: 在退出时停止捕获,或者保持运行
            // LogcatCaptureService.stopCapture()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("日志查看器")
                        Text(
                            text = "共 ${logs.size} 条 | 显示 ${filteredLogs.size} 条",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != {}) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "返回")
                        }
                    }
                },
                actions = {
                    // 自动滚动开关
                    IconButton(onClick = { autoScroll = !autoScroll }) {
                        Icon(
                            if (autoScroll) Icons.Default.PlayArrow else Icons.Default.Pause,
                            if (autoScroll) "自动滚动开启" else "自动滚动关闭",
                            tint = if (autoScroll) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    
                    // 过滤器
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                            containerColor = if (selectedPriority != "全部") 
                                MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Icon(Icons.Default.FilterList, "过滤")
                        }
                    }
                    
                    // 清除日志
                    IconButton(onClick = { 
                        LogcatCaptureService.clearLogs()
                        searchQuery = ""
                        selectedPriority = "全部"
                    }) {
                        Icon(Icons.Default.Clear, "清除")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("搜索日志 (标签或内容)") },
                leadingIcon = { Icon(Icons.Default.Search, "搜索") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "清除搜索")
                        }
                    }
                },
                singleLine = true
            )
            
            // 过滤提示
            if (selectedPriority != "全部" || searchQuery.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = buildString {
                                if (selectedPriority != "全部") append("优先级: $selectedPriority  ")
                                if (searchQuery.isNotEmpty()) append("搜索: \"$searchQuery\"")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { 
                                searchQuery = ""
                                selectedPriority = "全部"
                            }
                        ) {
                            Text("清除筛选", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            // 空状态
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = if (logs.isEmpty()) "暂无日志" else "没有匹配的日志",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        if (logs.isNotEmpty()) {
                            Text(
                                text = "尝试调整搜索条件或过滤器",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                // 日志列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredLogs, key = { it.fullLog + it.timestamp }) { log ->
                        LogItem(
                            log = log,
                            isExpanded = expandedLog == log,
                            onClick = { 
                                expandedLog = if (expandedLog == log) null else log
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        
        // 过滤对话框
        if (showFilterDialog) {
            FilterDialog(
                selectedPriority = selectedPriority,
                onPrioritySelected = { 
                    selectedPriority = it
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false }
            )
        }
    }
}

@Composable
fun LogItem(
    log: LogEntry,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = when (log.priority) {
                    "E" -> Color(0xFFFFEBEE)
                    "W" -> Color(0xFFFFF3E0)
                    "I" -> Color(0xFFE3F2FD)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = log.timestamp,
                fontSize = 10.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
            
            PriorityChip(log.priority)
        }
        
        Text(
            text = log.tag,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        
        Text(
            text = log.message,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = if (isExpanded) Int.MAX_VALUE else 3
        )
        
        if (isExpanded && log.message.length > 100) {
            Text(
                text = if (isExpanded) "点击折叠" else "点击展开",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun PriorityChip(priority: String) {
    val (color, backgroundColor) = when (priority) {
        "V" -> Color.Gray to Color(0xFFF5F5F5)
        "D" -> Color(0xFF2196F3) to Color(0xFFE3F2FD)
        "I" -> Color(0xFF4CAF50) to Color(0xFFE8F5E9)
        "W" -> Color(0xFFFF9800) to Color(0xFFFFF3E0)
        "E" -> Color(0xFFF44336) to Color(0xFFFFEBEE)
        "A" -> Color(0xFF9C27B0) to Color(0xFFF3E5F5)
        else -> Color.Gray to Color(0xFFF5F5F5)
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = priority,
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun FilterDialog(
    selectedPriority: String,
    onPrioritySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("过滤日志优先级") },
        text = {
            Column {
                val priorities = listOf("ALL", "V", "D", "I", "W", "E", "A")
                val priorityNames = mapOf(
                    "ALL" to "All",
                    "V" to "Verbose",
                    "D" to "Debug",
                    "I" to "Info",
                    "W" to "Warning",
                    "E" to "Error",
                    "A" to "Assert"
                )
                
                priorities.forEach { priority ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPriority == priority,
                            onClick = { onPrioritySelected(priority) }
                        )
                        Text(
                            text = "${priorityNames[priority]} ($priority)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}
