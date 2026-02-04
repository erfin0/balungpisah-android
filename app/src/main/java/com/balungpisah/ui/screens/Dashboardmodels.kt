package com.balungpisah.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

// Filter Types
enum class FilterType(val displayName: String) {
    ALL("Semua"),
    WEEK("Minggu Ini"),
    MONTH("Bulan Ini")
}

// Dashboard Summary
data class DashboardSummary(
    @SerializedName("total_reports") val totalReports: Int,
    @SerializedName("pending_count") val pendingCount: Int,
    @SerializedName("resolved_count") val resolvedCount: Int,
    @SerializedName("reports_this_week") val reportsThisWeek: Int,
    @SerializedName("reports_this_month") val reportsThisMonth: Int
)

data class DashboardSummaryResponse(
    val success: Boolean,
    val data: DashboardSummary
)

// Recent Reports
data class RecentReportsData(
    val reports: List<Report>,
    @SerializedName("total_count") val totalCount: Int,
    val days: Int
)

data class RecentReportsResponse(
    val success: Boolean,
    val data: RecentReportsData
)

// Category Stats
data class CategoryStat(
    val id: String,
    val name: String,
    val slug: String,
    val count: Int,
    val color: Color,
    val icon: String
)

data class CategoryWithCount(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val color: String,
    val icon: String,
    @SerializedName("report_count") val reportCount: Int
)

data class CategoryStatsData(
    val categories: List<CategoryWithCount>,
    val reports: List<Report>? = null,
    val pagination: Pagination? = null
)

data class CategoryStatsResponse(
    val success: Boolean,
    val data: CategoryStatsData
)

// Tag Stats
data class TagStatsResponse(
    val success: Boolean,
    val data: TagStatsData
)

data class TagStatsData(
    val tags: List<TagItem>
)

data class TagItem(
    @SerializedName("tag_type") val tagType: String,
    val label: String,
    @SerializedName("report_count") val reportCount: Int
)

data class TagStat(
    val tagType: String,
    val label: String,
    val count: Int
) {
    val color: Color
        get() = when (tagType.lowercase()) {
            "report" -> Color(0xFFF44336)
            "request" -> Color(0xFF2196F3)
            "suggestion" -> Color(0xFF4CAF50)
            "inquiry" -> Color(0xFFFF9800)
            else -> Color(0xFF9C27B0)
        }
    
    val icon: ImageVector
        get() = when (tagType.lowercase()) {
            "report" -> Icons.Default.Warning
            "request" -> Icons.Default.AddCircle
            "suggestion" -> Icons.Default.Lightbulb
            "inquiry" -> Icons.Default.Help
            else -> Icons.Default.Label
        }
}

// Report
data class Report(
    val id: String,
    val title: String,
    val description: String,
    val status: ReportStatus,
    val categories: List<ReportCategory>,
    @SerializedName("created_at") val createdAt: Date,
    val impact: String? = null,
    val timeline: String? = null,
    val location: ReportLocation? = null,
    @SerializedName("tag_type") val tagType: String? = null
)

data class ReportCategory(
    @SerializedName("category_id") val categoryId: String,
    val name: String,
    val slug: String,
    val color: String,
    val icon: String,
    val severity: String
)

enum class ReportStatus {
    @SerializedName("draft") DRAFT,
    @SerializedName("pending") PENDING,
    @SerializedName("verified") VERIFIED,
    @SerializedName("in_progress") IN_PROGRESS,
    @SerializedName("resolved") RESOLVED,
    @SerializedName("rejected") REJECTED;
    
    val displayName: String
        get() = when (this) {
            DRAFT -> "Draft"
            PENDING -> "Pending"
            VERIFIED -> "Terverifikasi"
            IN_PROGRESS -> "Proses"
            RESOLVED -> "Selesai"
            REJECTED -> "Ditolak"
        }
    
    val color: Color
        get() = when (this) {
            DRAFT -> Color.Gray
            PENDING -> Color(0xFFFF9800)
            VERIFIED -> Color(0xFF2196F3)
            IN_PROGRESS -> Color(0xFF9C27B0)
            RESOLVED -> Color(0xFF4CAF50)
            REJECTED -> Color(0xFFF44336)
        }
}

data class ReportLocation(
    @SerializedName("display_name") val displayName: String? = null,
    val city: String? = null,
    @SerializedName("regency_name") val regency: String? = null,
    @SerializedName("province_name") val province: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerializedName("province_id") val provinceId: String? = null,
    @SerializedName("regency_id") val regencyId: String? = null
)

data class Pagination(
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total_items") val totalItems: Int,
    @SerializedName("total_pages") val totalPages: Int
)

// Donut Chart
data class DonutSegment(
    val value: Double,
    val color: Color,
    val label: String
)

// Utility Functions
fun parseColor(hexString: String): Color {
    val hex = hexString.removePrefix("#")
    return try {
        when (hex.length) {
            6 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                Color(r, g, b, 255)
            }
            8 -> {
                val a = hex.substring(0, 2).toInt(16)
                val r = hex.substring(2, 4).toInt(16)
                val g = hex.substring(4, 6).toInt(16)
                val b = hex.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> Color.Gray
        }
    } catch (e: Exception) {
        Color.Gray
    }
}

fun Date.timeAgo(): String {
    val now = Date()
    val diff = now.time - this.time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365
    
    return when {
        years >= 1 -> if (years == 1L) "1 tahun lalu" else "$years tahun lalu"
        months >= 1 -> if (months == 1L) "1 bulan lalu" else "$months bulan lalu"
        weeks >= 1 -> if (weeks == 1L) "1 minggu lalu" else "$weeks minggu lalu"
        days >= 1 -> if (days == 1L) "1 hari lalu" else "$days hari lalu"
        hours >= 1 -> if (hours == 1L) "1 jam lalu" else "$hours jam lalu"
        minutes >= 1 -> if (minutes == 1L) "1 menit lalu" else "$minutes menit lalu"
        else -> "Baru saja"
    }
}