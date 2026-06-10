package com.example.consumointeligente.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Eco
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
import com.example.consumointeligente.network.AvailableMonth
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import java.util.Locale

fun formatCopStats(amount: Double): String {
    return String.format(Locale.US, "%,.0f", amount).replace(',', '.')
}

@Composable
fun NutritionDonutChart(
    protein: Double,
    carbs: Double,
    fat: Double,
    score: Int,
    scoreDiff: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            android.util.Log.d("DonutChart", "Drawing donut: protein=$protein, carbs=$carbs, fat=$fat")
            val strokeWidth = 14.dp.toPx()
            
            // Dibujar pista de fondo
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = (size.minDimension - strokeWidth) / 2,
                style = Stroke(width = strokeWidth)
            )
            
            val total = protein + carbs + fat
            if (total > 0.0) {
                val pPercent = (protein / total).toFloat()
                val cPercent = (carbs / total).toFloat()
                val fPercent = (fat / total).toFloat()
                
                val cSweep = cPercent * 360f
                val pSweep = pPercent * 360f
                val fSweep = fPercent * 360f
                
                // Carbohidratos (Naranja)
                drawArc(
                    color = Color(0xFFF97316),
                    startAngle = -90f,
                    sweepAngle = cSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // Proteínas (Morado)
                drawArc(
                    color = Color(0xFF8B5CF6),
                    startAngle = -90f + cSweep,
                    sweepAngle = pSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // Grasas (Teal)
                drawArc(
                    color = Color(0xFF0D9488),
                    startAngle = -90f + cSweep + pSweep,
                    sweepAngle = fSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                // Pista vacía por defecto
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        
        // Textos del centro
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Mi Score",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$score",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            val diffVal = try {
                scoreDiff.replace("+", "").replace("-", "").trim().toInt()
            } catch (e: Exception) {
                0
            }
            val diffColor = when {
                scoreDiff.startsWith("+") && diffVal > 0 -> Color(0xFF10B981) // Verde
                scoreDiff.startsWith("-") -> Color(0xFFEF4444) // Rojo
                else -> Color(0xFF64748B) // Gris
            }
            Text(
                text = if (scoreDiff.startsWith("+") || scoreDiff.startsWith("-")) scoreDiff else "+$scoreDiff",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = diffColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    var selectedMonth by remember { mutableStateOf<AvailableMonth?>(null) }
    var stats by remember { mutableStateOf<FinancialStatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var userQuestion by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedMonth) {
        isLoading = true
        try {
            stats = RetrofitClient.apiService.getFinancialStats(
                year = selectedMonth?.year,
                month = selectedMonth?.month
            )
            Log.d("StatsScreen", "Stats loaded successfully: $stats")
            Log.d("StatsScreen", "Available months: ${stats?.available_months}")
        } catch (e: Exception) {
            errorMessage = "No se pudieron cargar las estadísticas: ${e.message}"
            Log.e("StatsScreen", "Error Fetching Stats", e)
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Cabecera con Título "Mi Nutrición" y Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mi Nutrición",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            
            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable {
                            dropdownExpanded = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    val filterText = selectedMonth?.name ?: (stats?.month_name ?: "Seleccionar Mes")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = filterText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Filtro",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    val availableList = stats?.available_months ?: emptyList()
                    if (availableList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Sin facturas") },
                            onClick = { dropdownExpanded = false }
                        )
                    } else {
                        availableList.forEach { monthItem ->
                            DropdownMenuItem(
                                text = { Text(monthItem.name) },
                                onClick = {
                                    selectedMonth = monthItem
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = Color(0xFF1E3A1E)
            )
        } else if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        } else if (stats != null) {
            val pGrams = stats!!.protein_g
            val cGrams = stats!!.carbs_g
            val fGrams = stats!!.fat_g
            val scoreVal = stats!!.latest_health_score
            val scoreDiffVal = stats!!.health_score_diff.ifEmpty { "+0" }
            val kcalTotal = (pGrams * 4 + cGrams * 4 + fGrams * 9).toInt()
            
            val totalMacros = pGrams + cGrams + fGrams
            val pPct = if (totalMacros > 0.0) (pGrams / totalMacros * 100).toInt() else 0
            val cPct = if (totalMacros > 0.0) (cGrams / totalMacros * 100).toInt() else 0
            val fPct = if (totalMacros > 0.0) (fGrams / totalMacros * 100).toInt() else 0
            
            // Tarjeta de Contenido de Nutrición
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Fila de Indicador de Calorías
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = "Eco",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF0F172A))) {
                                    append(String.format("%,d", kcalTotal).replace(',', '.'))
                                }
                                withStyle(style = SpanStyle(fontSize = 18.sp, color = Color(0xFF64748B))) {
                                    append("/2300 ")
                                }
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))) {
                                    append("kcal")
                                }
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Donut y Desglose de Macronutrientes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gráfico
                        NutritionDonutChart(
                            protein = pGrams,
                            carbs = cGrams,
                            fat = fGrams,
                            score = scoreVal,
                            scoreDiff = scoreDiffVal,
                            modifier = Modifier.weight(1.1f)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Desglose
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Carbohidratos (Naranja)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 4.dp, height = 16.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFFF97316))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Carbs",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "$cPct%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                            
                            // Proteínas (Morado)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 4.dp, height = 16.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF8B5CF6))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Proteínas",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "$pPct%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                            
                            // Grasas (Teal)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 4.dp, height = 16.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF0D9488))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Grasas",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "$fPct%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Barra de Preguntar a NutriIA
                    OutlinedTextField(
                        value = userQuestion,
                        onValueChange = { userQuestion = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF8FAFC)),
                        placeholder = { Text("Preguntar a NutriIA...", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Sparkles",
                                tint = Color(0xFFF59E0B)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (userQuestion.isNotBlank()) {
                                    Toast.makeText(context, "NutriIA está analizando tu consulta...", Toast.LENGTH_LONG).show()
                                    userQuestion = ""
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Enviar",
                                    tint = Color(0xFF1E3A1E)
                                )
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2E8F0),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            disabledBorderColor = Color(0xFFE2E8F0),
                            errorBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true
                    )
                }
            }
        }
    }
}
