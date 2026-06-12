package com.example.consumointeligente.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import android.widget.Toast
import com.example.consumointeligente.ui.utils.PdfOcrHelper
import com.example.consumointeligente.network.RetrofitClient
import com.example.consumointeligente.network.OCRRequest
import com.example.consumointeligente.network.ReceiptResponse
import com.example.consumointeligente.network.FinancialStatsResponse
import com.example.consumointeligente.network.PetStatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.clickable

fun formatCop(amount: Double): String {
    return String.format(Locale.US, "%,.0f", amount).replace(',', '.')
}

fun formatAbbreviated(amount: Double): String {
    val inK = amount / 1000.0
    return if (inK >= 1.0) {
        val rounded = Math.round(inK).toInt()
        "$${rounded}k"
    } else {
        "$0k"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Mi Canasta", "Mascotas")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var receiptResult by remember { mutableStateOf<ReceiptResponse?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf<FinancialStatsResponse?>(null) }
    var petStats by remember { mutableStateOf<PetStatsResponse?>(null) }
    var showSuccessPopup by remember { mutableStateOf(false) }
    var expensePeriodFilter by remember { mutableStateOf(0) } // 0 = Año, 1 = Mes, 2 = Semana
    var petExpensePeriodFilter by remember { mutableStateOf(0) } // 0 = Año, 1 = Mes, 2 = Semana
    var chartTypeFilter by remember { mutableStateOf(0) } // 0 = Por Mes, 1 = Por Comercio
    
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && petStats == null) {
            try {
                petStats = RetrofitClient.apiService.getPetStats()
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error Fetching Pet Stats", e)
            }
        }
    }
    
    LaunchedEffect(receiptResult) {
        if (receiptResult != null) {
            showSuccessPopup = true
        }
        try {
            stats = RetrofitClient.apiService.getFinancialStats()
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error Fetching Stats", e)
        }
    }

    LaunchedEffect(showSuccessPopup) {
        if (showSuccessPopup) {
            kotlinx.coroutines.delay(3000)
            showSuccessPopup = false
        }
    }
    
    // Launcher para seleccionar un documento PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isLoading = true
            errorMessage = ""
            coroutineScope.launch {
                try {
                    val extractedText = PdfOcrHelper.processPdfFromUri(context, it)
                    val response = RetrofitClient.apiService.uploadReceipt(OCRRequest(extractedText))
                    receiptResult = response
                } catch (e: Exception) {
                    errorMessage = "Error de conexión: ${e.message}"
                    Log.e("HomeScreen", "Error Upload", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = Color(0xFF1E3A1E), // Premium dark green for FAB
                contentColor = Color(0xFFBEF264)   // Premium lime green for icon
            ) {
                Icon(Icons.Default.Add, contentDescription = "Escanear Factura")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)) // Beautiful premium light gray-blue background
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. User Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hola, admin 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A) // Slate-900
                    )
                    val currentMonthName = stats?.month_name ?: "Junio"
                    Text(
                        text = "$currentMonthName 2026",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B) // Slate-500
                    )
                }
                
                // Notification Bell icon
                IconButton(onClick = { /* TODO: Open Notifications */ }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Color(0xFF475569) // Slate-600
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Avatar with initials "AD" in lime green background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFBEF264)), // Lime green
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AD",
                        color = Color(0xFF1E3A1E), // Dark green text
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bubble selection tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF1E3A1E),
                indicator = {},
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1E3A1E) else Color(0xFFE2E8F0))
                            .height(40.dp),
                        text = {
                            Text(
                                text = title,
                                color = if (isSelected) Color(0xFFBEF264) else Color(0xFF475569),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Color(0xFF1E3A1E)
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            if (showSuccessPopup) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)) // light green
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Factura procesada con éxito",
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (selectedTab == 0) {
                // MI CANASTA / MI MERCADO
                
                // 2. Lime Green Card (Total Gastos con Filtro de Período)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFBEF264)) // Premium Lime Green
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val titleText = when (expensePeriodFilter) {
                                0 -> "TOTAL GASTOS DEL AÑO"
                                1 -> "TOTAL GASTOS DEL MES"
                                else -> "TOTAL GASTOS DE LA SEMANA"
                            }
                            Text(
                                text = titleText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A1E), // Premium dark green text
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.weight(1f) // Consumir espacio restante para dar espacio al control de la derecha
                            )
                            
                            // Segmented control filter (Año, Mes, Semana)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E3A1E).copy(alpha = 0.08f))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("Año", "Mes", "Semana").forEachIndexed { index, label ->
                                    val isFilterSelected = expensePeriodFilter == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isFilterSelected) Color(0xFF1E3A1E) else Color.Transparent)
                                            .clickable { expensePeriodFilter = index }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFilterSelected) Color(0xFFBEF264) else Color(0xFF1E3A1E),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val displayTotal = when (expensePeriodFilter) {
                            0 -> stats?.yearly_total ?: 0.0
                            1 -> stats?.monthly_total ?: 0.0
                            else -> stats?.weekly_total ?: 0.0
                        }
                        Text(
                            text = "$ ${formatCop(displayTotal)} COP",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E3A1E)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Variation indicator text
                        val variationText = stats?.month_diff_text ?: "0% más en el mes de este año"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E3A1E).copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = variationText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E3A1E)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 3. Monthly Bar Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (chartTypeFilter == 0) "Gastos por Mes" else "Gastos por Comercio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Segmented control to choose chart type
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                listOf("Mes", "Comercio").forEachIndexed { index, label ->
                                    val isSelected = chartTypeFilter == index
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) Color(0xFF1E3A1E) else Color.Transparent)
                                            .clickable { chartTypeFilter = index }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFFBEF264) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        if (chartTypeFilter == 0) {
                            val historyList = stats?.monthly_history ?: emptyList()
                            if (historyList.isNotEmpty()) {
                                val maxAmount = historyList.maxOfOrNull { it.amount } ?: 1.0
                                val maxVal = if (maxAmount > 0) maxAmount else 1.0
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    historyList.forEachIndexed { index, item ->
                                        val isCurrent = index == historyList.lastIndex
                                        val barColor = if (isCurrent) Color(0xFF1E3A1E) else Color(0xFFBEF264)
                                        val ratio = (item.amount / maxVal).toFloat()
                                        val barHeight = (ratio * 100).coerceAtLeast(6f).dp
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(48.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .height(barHeight)
                                                    .width(28.dp)
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                    .background(barColor)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = item.month,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            
                                            Text(
                                                text = formatAbbreviated(item.amount),
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No hay datos de historial disponibles.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            val storeList = stats?.store_breakdown ?: emptyList()
                            if (storeList.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    storeList.forEach { storeItem ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = storeItem.store,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "$ ${formatCop(storeItem.amount)} COP (${storeItem.percentage}%)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E3A1E)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { (storeItem.percentage / 100f).toFloat() },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                color = Color(0xFF1E3A1E),
                                                trackColor = Color(0xFFF1F5F9)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No hay datos de comercios registrados.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Category breakdown / alerts / recipes
                if (stats != null && stats!!.categories_breakdown.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Distribución de Gastos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            stats!!.categories_breakdown.forEach { cat ->
                                if (cat.amount > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat.category,
                                            fontSize = 14.sp,
                                            color = Color(0xFF334155),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "$ ${formatCop(cat.amount)} COP (${cat.percentage}%)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Alerta Predictiva",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Posible alza en frutas por temporada de lluvias. Sugerimos comprar ahora.",
                            fontSize = 14.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }
                
            } else if (selectedTab == 1) {
                // MASCOTAS (Mismo estilo visual y de análisis que Mi Canasta)
                if (petStats == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color(0xFF1E3A1E)
                    )
                } else {
                    // 1. Lime Green Card (Total Gastos Mascotas con Filtro de Período)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFBEF264)) // Premium Lime Green
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val titleText = when (petExpensePeriodFilter) {
                                    0 -> "TOTAL GASTOS MASCOTAS (AÑO)"
                                    1 -> "TOTAL GASTOS MASCOTAS (MES)"
                                    else -> "TOTAL GASTOS MASCOTAS (SEMANA)"
                                }
                                Text(
                                    text = titleText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A1E), // Premium dark green text
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Segmented control filter (Año, Mes, Semana)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E3A1E).copy(alpha = 0.08f))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    listOf("Año", "Mes", "Semana").forEachIndexed { index, label ->
                                        val isFilterSelected = petExpensePeriodFilter == index
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isFilterSelected) Color(0xFF1E3A1E) else Color.Transparent)
                                                .clickable { petExpensePeriodFilter = index }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFilterSelected) Color(0xFFBEF264) else Color(0xFF1E3A1E),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val displayTotal = when (petExpensePeriodFilter) {
                                0 -> petStats!!.yearly_total
                                1 -> petStats!!.monthly_total
                                else -> petStats!!.weekly_total
                            }
                            Text(
                                text = "$ ${formatCop(displayTotal)} COP",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E3A1E)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 2. Monthly Bar Chart (Gastos Mascotas por Mes)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Gastos Mascotas por Mes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            val historyList = petStats!!.monthly_history
                            if (historyList.isNotEmpty()) {
                                val maxAmount = historyList.maxOfOrNull { it.amount } ?: 1.0
                                val maxVal = if (maxAmount > 0) maxAmount else 1.0
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    historyList.forEachIndexed { index, item ->
                                        val isCurrent = index == historyList.lastIndex
                                        val barColor = if (isCurrent) Color(0xFF1E3A1E) else Color(0xFFBEF264)
                                        val ratio = (item.amount / maxVal).toFloat()
                                        val barHeight = (ratio * 100).coerceAtLeast(6f).dp
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(48.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .height(barHeight)
                                                    .width(28.dp)
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                    .background(barColor)
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = item.month,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            
                                            Text(
                                                text = formatAbbreviated(item.amount),
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No hay datos de historial disponibles.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 3. Recomendación Nutricional Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // Warm yellow/amber
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Recomendación Nutricional",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = petStats!!.recommendation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }
        }
    }
}

