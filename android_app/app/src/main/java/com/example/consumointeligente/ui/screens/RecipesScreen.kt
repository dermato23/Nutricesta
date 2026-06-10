package com.example.consumointeligente.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consumointeligente.network.RetrofitClient
import com.example.consumointeligente.network.FinancialStatsResponse
import android.util.Log

@Composable
fun RecipesScreen() {
    var stats by remember { mutableStateOf<FinancialStatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            stats = RetrofitClient.apiService.getFinancialStats()
        } catch (e: Exception) {
            errorMessage = "No se pudieron cargar las recetas: ${e.message}"
            Log.e("RecipesScreen", "Error Fetching Stats", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Recetas Recomendadas", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else if (stats != null && stats!!.recipes.isNotEmpty()) {
            Text("Aprovecha tu mercado de esta semana:", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFF57F17))
            Spacer(modifier = Modifier.height(16.dp))

            stats!!.recipes.forEach { recipe ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${recipe.day}: ${recipe.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(recipe.description, style = MaterialTheme.typography.bodyMedium)
                        
                        if (recipe.protein_pct != null && recipe.carbs_pct != null && recipe.lipids_pct != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Proteína: ${recipe.protein_pct}%", style = MaterialTheme.typography.bodySmall, color = Color(0xFF388E3C))
                                Text("Carbos: ${recipe.carbs_pct}%", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1976D2))
                                Text("Lípidos: ${recipe.lipids_pct}%", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
                            }
                        }
                    }
                }
            }
        } else {
            Text("No hay recetas disponibles.", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
