package com.example.consumointeligente.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consumointeligente.network.RetrofitClient
import com.example.consumointeligente.network.WholesaleTrendsResponse

@Composable
fun WholesaleScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    var trends by remember { mutableStateOf<WholesaleTrendsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            loading = true
            val response = RetrofitClient.apiService.getWholesaleTrends()
            trends = response
            errorMsg = null
        } catch (e: Exception) {
            Log.e("WholesaleScreen", "Error loading wholesale trends", e)
            errorMsg = "Error al cargar datos del servidor"
            // Fallback estático
            trends = WholesaleTrendsResponse(
                week_start = "2026-06-06",
                week_end = "2026-06-12",
                suben = listOf(
                    "Cebolla cabezona blanca", "Puerro", "Calabaza", "Tangelo", 
                    "Mango de azúcar", "Chócolo mazorca", "Mora de Castilla", 
                    "Repollo morado", "Repollo verde", "Limón común", 
                    "Aguacate Hass", "Manzana nacional", "Pepino cohombro", 
                    "Remolacha", "Guayaba pera", "Pera importada", 
                    "Papa sabanera", "Pimentón", "Ciruela importada", 
                    "Patilla baby", "Uva Isabela"
                ),
                bajan = listOf(
                    "Maracuyá", "Papaya Paulina", "Ciruela roja", "Espinaca", 
                    "Brócoli", "Cebolla junca", "Melón Cantalup", "Zanahoria", 
                    "Banano bocadillo"
                )
            )
        } finally {
            loading = false
        }
    }

    val suben = trends?.suben ?: emptyList()
    val bajan = trends?.bajan ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Cabecera Principal
        Text(
            text = "Precios Mayoristas",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Bogotá D.C. • Boletín Semanal SIPSA",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        // Etiqueta de fecha
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE2E8F0))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            val dateText = if (trends != null) {
                "Semana: ${trends?.week_start} al ${trends?.week_end}"
            } else {
                "Cargando..."
            }
            Text(
                text = dateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mostrar mensaje de error si ocurre y no pudimos cargar en vivo
        if (errorMsg != null) {
            Text(
                text = errorMsg ?: "",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Tabs de Selección
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color(0xFF1E3A1E),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF1E3A1E)
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color(0xFFEF4444) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Lo que más sube",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color(0xFF10B981) else Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Lo que más baja",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading && trends == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1E3A1E))
            }
        } else {
            // Lista de Productos
            val currentList = if (selectedTab == 0) suben else bajan
            val indicatorColor = if (selectedTab == 0) Color(0xFFFEE2E2) else Color(0xFFD1FAE5)
            val iconColor = if (selectedTab == 0) Color(0xFFEF4444) else Color(0xFF10B981)
            val iconImage = if (selectedTab == 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay datos para mostrar",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentList) { product ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconImage,
                                            contentDescription = null,
                                            tint = iconColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = product,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

