package com.balungpisah.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.balungpisah.data.network.NetworkClient
import com.balungpisah.util.AppConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(private val context: Context) : ViewModel() {

    private val networkClient = NetworkClient.getInstance(context)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadDashboardData()
        }
    }

    suspend fun loadDashboardData() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val summaryDeferred = async { loadSummary() }
            val recentReportsDeferred = async { loadRecentReports() }
            val categoryStatsDeferred = async { loadCategoryStats() }
            val tagStatsDeferred = async { loadTagStats() }

            summaryDeferred.await()
            recentReportsDeferred.await()
            categoryStatsDeferred.await()
            tagStatsDeferred.await()

            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    suspend fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadDashboardData()
    }

    private suspend fun loadSummary() {
        networkClient.request<DashboardSummaryResponse>(
            endpoint = "/api/dashboard/summary",
            method = "GET",
            responseClass = DashboardSummaryResponse::class.java
        ).onSuccess { response ->
            _uiState.update { it.copy(summary = response.data) }
        }.onFailure { error ->
            println("Error loading summary: $error")
        }
    }

    private suspend fun loadRecentReports() {
        networkClient.request<RecentReportsResponse>(
            endpoint = "/api/dashboard/recent",
            method = "GET",
            responseClass = RecentReportsResponse::class.java
        ).onSuccess { response ->
            _uiState.update { it.copy(recentReports = response.data.reports) }
        }.onFailure { error ->
            println("Error loading recent reports: $error")
        }
    }

    private suspend fun loadCategoryStats() {
        networkClient.request<CategoryStatsResponse>(
            endpoint = "/api/dashboard/by-category",
            method = "GET",
            responseClass = CategoryStatsResponse::class.java
        ).onSuccess { response ->
            val stats = response.data.categories.map { category ->
                CategoryStat(
                    id = category.id,
                    name = category.name,
                    slug = category.slug,
                    count = category.reportCount,
                    color = parseColor(category.color),
                    icon = category.icon
                )
            }.sortedByDescending { it.count }

            _uiState.update { it.copy(categoryStats = stats) }
        }.onFailure { error ->
            println("Error loading category stats: $error")
        }
    }

    private suspend fun loadTagStats() {
        val allTypes = listOf(
            TagStat(tagType = "report", label = "Laporan", count = 0),
            TagStat(tagType = "request", label = "Permintaan", count = 0),
            TagStat(tagType = "suggestion", label = "Saran", count = 0),
            TagStat(tagType = "inquiry", label = "Pertanyaan", count = 0)
        )

        networkClient.request<TagStatsResponse>(
            endpoint = "/api/dashboard/by-tag",
            method = "GET",
            responseClass = TagStatsResponse::class.java
        ).onSuccess { response ->
            val updatedStats = allTypes.map { stat ->
                val apiTag = response.data.tags.find {
                    it.tagType.equals(stat.tagType, ignoreCase = true)
                }
                stat.copy(count = apiTag?.reportCount ?: 0)
            }
            _uiState.update { it.copy(tagStats = updatedStats) }
        }.onFailure { error ->
            println("Error loading tag stats: $error")
            _uiState.update { it.copy(tagStats = allTypes) }
        }
    }

    fun updateFilter(filter: FilterType) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun updateCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun updateStatus(status: ReportStatus?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val summary: DashboardSummary? = null,
    val recentReports: List<Report> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val tagStats: List<TagStat> = emptyList(),
    val selectedFilter: FilterType = FilterType.ALL,
    val selectedCategory: String? = null,
    val selectedStatus: ReportStatus? = null
)