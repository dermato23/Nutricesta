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
import kotlinx.coroutines.launch
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
            val minDim = size.minDimension - strokeWidth
            val arcSize = androidx.compose.ui.geometry.Size(minDim, minDim)
            val topLeftOffset = androidx.compose.ui.geometry.Offset(
                x = (size.width - minDim) / 2f,
                y = (size.height - minDim) / 2f
            )
            
            // Dibujar pista de fondo
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = minDim / 2f,
                center = center,
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
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // Proteínas (Morado)
                drawArc(
                    color = Color(0xFF8B5CF6),
                    startAngle = -90f + cSweep,
                    sweepAngle = pSweep,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                // Grasas (Teal)
                drawArc(
                    color = Color(0xFF0D9488),
                    startAngle = -90f + cSweep + pSweep,
                    sweepAngle = fSweep,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            } else {
                // Pista vacía por defecto
                drawArc(
                    color = Color(0xFFE2E8F0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = arcSize,
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
    val coroutineScope = rememberCoroutineScope()
    var selectedMonth by remember { mutableStateOf<AvailableMonth?>(null) }
    var stats by remember { mutableStateOf<FinancialStatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var userQuestion by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var aiResponseText by remember { mutableStateOf<String?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }
    
    LaunchedEffect(selectedMonth) {
        isLoading = true
        aiResponseText = null // Reset answer on startup or month change
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
            val healthReasonVal = stats!!.latest_health_reason.ifEmpty { "Tu mercado tiene una buena base de alimentos frescos. Continúa incorporando variedad." }
            
            val totalMacros = pGrams + cGrams + fGrams
            val pPct = if (totalMacros > 0.0) (pGrams / totalMacros * 100).toInt() else 0
            val cPct = if (totalMacros > 0.0) (cGrams / totalMacros * 100).toInt() else 0
            val fPct = if (totalMacros > 0.0) (fGrams / totalMacros * 100).toInt() else 0

            val carbsAdvice = when {
                cPct > 55 -> "Nivel de carbohidratos elevado. Te sugiero balancear disminuyendo azúcares/procesados y priorizando carbohidratos complejos como la avena y el arroz integral en tus próximas compras."
                cPct < 40 -> "Nivel de carbohidratos bajo. Si te sientes fatigado, considera añadir fuentes saludables como tubérculos o granos enteros."
                else -> "Buen nivel de energía. Te sugiero priorizar carbohidratos complejos como la avena y el arroz integral en tus próximas compras."
            }
            
            val proteinAdvice = when {
                pPct < 25 -> "Nivel de proteína bajo. Considera incorporar más carnes magras, pescados, huevos o legumbres para alcanzar tu requerimiento diario."
                else -> "Estás cerca de tu meta ideal. El pollo, lentejas y huevos que has comprado son excelentes fuentes de proteína."
            }
            
            val fatAdvice = when {
                fPct > 35 -> "Nivel de grasas elevado. Intenta moderar los aceites refinados y embutidos, y priorizar grasas saludables."
                else -> "Balance saludable. Las grasas provenientes del aguacate y frutos secos son excelentes para regular tus hormonas."
            }
            
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
                            IconButton(
                                onClick = {
                                    if (userQuestion.isNotBlank() && !isAiThinking) {
                                        val questionText = userQuestion
                                        userQuestion = ""
                                        isAiThinking = true
                                        coroutineScope.launch {
                                            try {
                                                val req = com.example.consumointeligente.network.AskNutritionRequest(
                                                    question = questionText,
                                                    year = selectedMonth?.year ?: stats?.available_months?.firstOrNull()?.year,
                                                    month = selectedMonth?.month ?: stats?.available_months?.firstOrNull()?.month
                                                )
                                                val resp = RetrofitClient.apiService.askNutritionQuestion(req)
                                                aiResponseText = resp.answer
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error al consultar a NutriIA: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isAiThinking = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isAiThinking && userQuestion.isNotBlank()
                            ) {
                                if (isAiThinking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF1E3A1E),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Enviar",
                                        tint = if (userQuestion.isNotBlank()) Color(0xFF1E3A1E) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE2E8F0),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            disabledBorderColor = Color(0xFFE2E8F0),
                            errorBorderColor = Color(0xFFE2E8F0)
                        ),
                        singleLine = true,
                        enabled = !isAiThinking
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tarjeta de Respuesta de IA
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "NutriIA",
                            tint = Color(0xFF1E3A1E),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "NutriIA - Análisis y Recomendación",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A1E)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (aiResponseText != null) {
                        Text(
                            text = aiResponseText!!,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "¡Hola! He analizado tus consumos y compras del mes. Aquí tienes algunas observaciones importantes para tu nutrición:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Detalle de macronutrientes
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF97316))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                                            append("Carbohidratos ($cPct%): ")
                                        }
                                        append(carbsAdvice)
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8B5CF6))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                                            append("Proteínas ($pPct%): ")
                                        }
                                        append(proteinAdvice)
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0D9488))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))) {
                                            append("Grasas ($fPct%): ")
                                        }
                                        append(fatAdvice)
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "💡 Análisis de compra: $healthReasonVal",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A1E)
                        )
                    }
                }
            }
        }
    }
}
