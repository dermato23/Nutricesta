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
    val amount: Double,
    val year: Int? = null,
    val month_num: Int? = null
)

data class StoreBreakdown(
    val store: String,
    val amount: Double,
    val percentage: Double
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
    val store_breakdown: List<StoreBreakdown> = emptyList(),
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

data class RecipePreferences(
    val time: String = "",
    val diets: List<String> = emptyList(),
    val allergies: List<String> = emptyList(),
    val goal: String = "",
    val dish_types: List<String> = emptyList()
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
    suspend fun getPetStats(
        @Query("pet_type") petType: String? = null,
        @Query("breed") breed: String? = null,
        @Query("age_range") ageRange: String? = null
    ): PetStatsResponse

    @GET("/api/v1/users/1/preferences")
    suspend fun getRecipePreferences(): RecipePreferences

    @POST("/api/v1/users/1/preferences")
    suspend fun updateRecipePreferences(@Body prefs: RecipePreferences): Any

    @POST("/api/v1/nutrition/ask")
    suspend fun askNutritionQuestion(@Body request: AskNutritionRequest): AskNutritionResponse

    @GET("/api/v1/stats/wholesale")
    suspend fun getWholesaleTrends(): WholesaleTrendsResponse
}

data class AskNutritionRequest(
    val question: String,
    val year: Int? = null,
    val month: Int? = null
)

data class AskNutritionResponse(
    val answer: String
)

data class WholesaleTrendsResponse(
    val week_start: String,
    val week_end: String,
    val suben: List<String>,
    val bajan: List<String>
)
