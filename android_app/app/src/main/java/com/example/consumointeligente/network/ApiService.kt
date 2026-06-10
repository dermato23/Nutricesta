package com.example.consumointeligente.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class AvailableMonth(
    val year: Int,
    val month: Int,
    val name: String
)

data class OCRRequest(val raw_text: String)

data class ReceiptItem(
    val item_name: String,
    val category: String,
    val precio_neto: Double,
    val id: Int,
    val receipt_id: Int
)

data class ReceiptResponse(
    val id: Int,
    val store_name: String,
    val total_amount: Double,
    val items: List<ReceiptItem>
)

data class CategoryBreakdown(
    val category: String,
    val percentage: Double,
    val amount: Double = 0.0
)

data class MonthHistoryItem(
    val month: String,
    val amount: Double
)

data class FinancialStatsResponse(
    val monthly_total: Double,
    val monthly_savings: Double = 0.0,
    val yearly_savings: Double = 0.0,
    val yearly_total: Double = 0.0,
    val weekly_total: Double = 0.0,
    val month_name: String = "",
    val month_diff_text: String = "",
    val monthly_history: List<MonthHistoryItem> = emptyList(),
    val latest_health_score: Int = 0,
    val latest_health_reason: String = "",
    val categories_breakdown: List<CategoryBreakdown> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val health_score_diff: String = "",
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
    val available_months: List<AvailableMonth> = emptyList()
)

data class Recipe(
    val day: String,
    val title: String,
    val description: String,
    val protein_pct: Int? = 0,
    val carbs_pct: Int? = 0,
    val lipids_pct: Int? = 0
)

data class PetStatsResponse(
    val monthly_total: Double,
    val yearly_total: Double = 0.0,
    val weekly_total: Double = 0.0,
    val monthly_history: List<MonthHistoryItem> = emptyList(),
    val recommendation: String
)

interface ApiService {
    @POST("/api/v1/receipts/upload")
    suspend fun uploadReceipt(@Body request: OCRRequest): ReceiptResponse

    @GET("/api/v1/stats/financial")
    suspend fun getFinancialStats(
        @Query("year") year: Int? = null,
        @Query("month") month: Int? = null
    ): FinancialStatsResponse

    @GET("/api/v1/stats/pets")
    suspend fun getPetStats(): PetStatsResponse
}
