package killua.dev.core.log.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import killua.dev.core.log.LogcatCaptureService
import killua.dev.core.log.domain.LogEntry
import killua.dev.core.log.domain.LogExportFormat
import killua.dev.core.log.domain.LogFilter
import killua.dev.core.log.domain.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    logService: LogcatCaptureService
) {
    val logs by logService.logs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf(LogLevel.ALL) }
    var autoScroll by remember { mutableStateOf(false) }
    var expandedLog by remember { mutableStateOf<LogEntry?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val currentFilter = remember(searchQuery, selectedPriority) {
        LogFilter(
            query = searchQuery,
            selectedLevel = selectedPriority
        )
    }

    val filteredLogs = remember(logs, currentFilter) {
        logs.filter { currentFilter.matches(it) }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            // 退出 UI 时保持日志捕获运行,继续在后台记录
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
                            containerColor = if (selectedPriority != LogLevel.ALL)
                                MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Icon(Icons.Default.FilterList, "过滤")
                        }
                    }

                    // 导出日志
                    IconButton(
                        onClick = { showExportDialog = true },
                        enabled = logs.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Download, "导出")
                    }

                    // 清除日志
                    IconButton(onClick = {
                        coroutineScope.launch {
                            logService?.clearLogs()
                            searchQuery = ""
                            selectedPriority = LogLevel.ALL
                        }
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
            if (selectedPriority != LogLevel.ALL || searchQuery.isNotEmpty()) {
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
                                if (selectedPriority != LogLevel.ALL) append("优先级: ${selectedPriority.displayName}  ")
                                if (searchQuery.isNotEmpty()) append("搜索: \"$searchQuery\"")
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedPriority = LogLevel.ALL
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
                // 导出进度指示器
                AnimatedVisibility(
                    visible = isExporting,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 日志列表
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(filteredLogs, key = { log -> log.fullLog + log.timestamp }) { log ->
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
        SnackbarHost(hostState = snackbarHostState)

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

        // 导出对话框
        if (showExportDialog) {
            ExportDialog(
                onDismiss = { showExportDialog = false },
                onExport = { format, share ->
                    coroutineScope.launch {
                        isExporting = true
                        try {
                            logService?.exportLogs(format, share)?.fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar(
                                        "导出成功: $it"
                                    )
                                },
                                onFailure = { error ->
                                    snackbarHostState.showSnackbar(
                                        "导出失败: ${error.message}"
                                    )
                                }
                            )
                        } finally {
                            isExporting = false
                            showExportDialog = false
                        }
                    }
                }
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
                color = log.level.getBackgroundColor()
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
                text = log.displayTimestamp,
                fontSize = 10.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )

            PriorityChip(log.level.priority)
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
    val logLevel = LogLevel.fromPriority(priority)
    val (color, backgroundColor) = logLevel.getColors()

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
    selectedPriority: LogLevel,
    onPrioritySelected: (LogLevel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("过滤日志优先级") },
        text = {
            Column {
                LogLevel.entries.forEach { level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPriority == level,
                            onClick = { onPrioritySelected(level) }
                        )
                        Text(
                            text = "${level.displayName} (${level.priority})",
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

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (LogExportFormat, Boolean) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(LogExportFormat.TXT) }
    var shareFile by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出日志") },
        text = {
            Column {
                Text("选择导出格式:", style = MaterialTheme.typography.bodyMedium)

                LogExportFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Text(
                            text = "${format.name} (.${format.extension})",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = shareFile,
                        onClick = { shareFile = !shareFile }
                    )
                    Text(
                        text = "导出后分享文件",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(selectedFormat, shareFile)
                }
            ) {
                Text("导出")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
