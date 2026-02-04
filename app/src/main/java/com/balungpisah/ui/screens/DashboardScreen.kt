package com.balungpisah.ui.screens

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    context: Context,
    onNavigateToReportsList: () -> Unit = {}
) {
    val viewModel = remember { DashboardViewModel(context) }
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, "Filter")
                    }
                    IconButton(
                        onClick = { scope.launch { viewModel.loadDashboardData() } },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isRefreshing),
            onRefresh = { scope.launch { viewModel.refresh() } },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Summary Cards Section
                item {
                    SummaryCardsSection(uiState.summary, uiState.selectedFilter)
                }

                // Tag Type Bar Chart Section
                item {
                    TagTypeBarChartSection(uiState.tagStats)
                }

                // Status Statistics Chart
                item {
                    StatusChartSection(uiState.summary)
                }

                // Category Breakdown Section
                item {
                    CategoryBreakdownSection(uiState.categoryStats)
                }

                // Recent Reports Section
                item {
                    RecentReportsSection(
                        reports = uiState.recentReports,
                        onViewAll = onNavigateToReportsList
                    )
                }
            }
        }

        if (showFilters) {
            FilterBottomSheet(
                selectedFilter = uiState.selectedFilter,
                selectedCategory = uiState.selectedCategory,
                selectedStatus = uiState.selectedStatus,
                categories = uiState.categoryStats,
                onDismiss = { showFilters = false },
                onFilterChanged = { filter -> viewModel.updateFilter(filter) },
                onCategoryChanged = { category -> viewModel.updateCategory(category) },
                onStatusChanged = { status -> viewModel.updateStatus(status) }
            )
        }
    }
}

@Composable
private fun SummaryCardsSection(
    summary: DashboardSummary?,
    selectedFilter: FilterType
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SummaryCard(
                title = "Total Laporan",
                value = "${summary?.totalReports ?: 0}",
                icon = Icons.Default.Description,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Selesai",
                value = "${summary?.resolvedCount ?: 0}",
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SummaryCard(
                title = "Pending",
                value = "${summary?.pendingCount ?: 0}",
                icon = Icons.Default.Schedule,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = selectedFilter.displayName,
                value = when (selectedFilter) {
                    FilterType.ALL -> "${summary?.totalReports ?: 0}"
                    FilterType.WEEK -> "${summary?.reportsThisWeek ?: 0}"
                    FilterType.MONTH -> "${summary?.reportsThisMonth ?: 0}"
                },
                icon = Icons.Default.CalendarMonth,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TagTypeBarChartSection(tagStats: List<TagStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Laporan per Tipe",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (tagStats.isEmpty()) {
            EmptyStateView(
                message = "Memuat data tipe laporan...",
                icon = Icons.Default.BarChart
            )
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val maxCount = tagStats.maxOfOrNull { it.count } ?: 1

                    for (stat in tagStats) {
                        TagStatRow(stat = stat, maxCount = maxCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun TagStatRow(stat: TagStat, maxCount: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (maxCount > 0) stat.count.toFloat() / maxCount else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon and Label
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(stat.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stat.icon,
                    contentDescription = null,
                    tint = stat.color,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = stat.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        // Progress Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(stat.color, stat.color.copy(alpha = 0.8f))
                        )
                    )
            )
        }

        // Count
        Text(
            text = "${stat.count}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(35.dp)
        )
    }
}

@Composable
private fun StatusChartSection(summary: DashboardSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Statistik Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (summary == null || summary.totalReports == 0) {
            EmptyStateView(
                message = "Belum ada data statistik",
                icon = Icons.Default.PieChart
            )
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DonutChart(
                        segments = listOf(
                            DonutSegment(
                                value = summary.resolvedCount.toDouble(),
                                color = Color(0xFF4CAF50),
                                label = "Selesai"
                            ),
                            DonutSegment(
                                value = summary.pendingCount.toDouble(),
                                color = Color(0xFFFF9800),
                                label = "Pending"
                            ),
                            DonutSegment(
                                value = (summary.totalReports - summary.resolvedCount - summary.pendingCount).toDouble().coerceAtLeast(0.0),
                                color = Color.Gray,
                                label = "Belum Terverifikasi"
                            )
                        ),
                        total = summary.totalReports.toDouble(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ChartLegendItem(
                            color = Color(0xFF4CAF50),
                            label = "Selesai",
                            value = summary.resolvedCount
                        )
                        ChartLegendItem(
                            color = Color(0xFFFF9800),
                            label = "Pending",
                            value = summary.pendingCount
                        )
                        ChartLegendItem(
                            color = Color.Gray,
                            label = "Belum Terverifikasi",
                            value = summary.totalReports - summary.resolvedCount - summary.pendingCount
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendItem(
    color: Color,
    label: String,
    value: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryBreakdownSection(categoryStats: List<CategoryStat>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Laporan per Kategori",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        when {
            categoryStats.isEmpty() -> {
                EmptyStateView(
                    message = "Memuat kategori...",
                    icon = Icons.Default.Apps
                )
            }
            categoryStats.none { it.count > 0 } -> {
                EmptyStateView(
                    message = "Belum ada laporan di kategori manapun",
                    icon = Icons.Default.Inbox
                )
            }
            else -> {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categoriesWithReports = categoryStats.filter { it.count > 0 }.take(5)
                        for (stat in categoriesWithReports) {
                            CategoryStatRow(stat = stat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryStatRow(stat: CategoryStat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(stat.color)
            )
            Text(
                text = stat.name,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Text(
            text = "${stat.count}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentReportsSection(
    reports: List<Report>,
    onViewAll: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Laporan Terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (reports.isNotEmpty()) {
                TextButton(onClick = onViewAll) {
                    Text("Lihat Semua")
                }
            }
        }

        if (reports.isEmpty()) {
            EmptyStateView(
                message = "Belum ada laporan",
                icon = Icons.Default.Description
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val recentReportsList = reports.take(5)
                for (report in recentReportsList) {
                    ReportRowCompact(report = report)
                }
            }
        }
    }
}

@Composable
private fun ReportRowCompact(report: Report) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )

                report.categories.firstOrNull()?.let { category ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(parseColor(category.color))
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusBadge(status = report.status)
                Text(
                    text = report.createdAt.timeAgo(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ReportStatus) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = status.color.copy(alpha = 0.2f)
    ) {
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = status.color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyStateView(
    message: String,
    icon: ImageVector
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}