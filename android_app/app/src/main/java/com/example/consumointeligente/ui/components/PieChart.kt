package com.example.consumointeligente.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.consumointeligente.network.CategoryBreakdown
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.atan2

fun getColorForCategory(category: String): Color {
    return when (category.trim()) {
        "Proteína" -> Color(0xFF8B5CF6) // Purple
        "Verduras y Hortalizas" -> Color(0xFF22C55E) // Green
        "Frutas" -> Color(0xFFEAB308) // Yellow
        "Lácteos" -> Color(0xFF3B82F6) // Blue
        "Carbohidratos" -> Color(0xFFF97316) // Orange
        "Cereales" -> Color(0xFFF59E0B) // Amber
        "Granos" -> Color(0xFF78350F) // Brown
        "Productos de Aseo Personal" -> Color(0xFFEC4899) // Pink
        "Aseo de Hogar" -> Color(0xFF06B6D4) // Cyan
        "Snacks o Antojos" -> Color(0xFFEF4444) // Red
        "Mascotas" -> Color(0xFFBEF264) // Lime Green
        "Grasas o Lípidos", "Grasas" -> Color(0xFF0D9488) // Teal
        else -> Color(0xFF64748B) // Gray
    }
}

@Composable
fun InteractivePieChart(
    categories: List<CategoryBreakdown>,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<CategoryBreakdown?>(null) }
    var selectedAngle by remember { mutableStateOf(0f) }
    
    // Calculate total to normalize percentages if they don't perfectly add up to 100
    val totalPercentage = categories.sumOf { it.percentage }
    if (totalPercentage == 0.0) return

    val angles = categories.map { ((it.percentage / totalPercentage) * 360).toFloat() }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = tapOffset.x - centerX
                        val dy = tapOffset.y - centerY
                        
                        // Calculate angle in degrees (0 to 360)
                        var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (touchAngle < 0) touchAngle += 360f
                        
                        // Because canvas arcs start at 3 o'clock (0 degrees) and sweep clockwise
                        var currentStartAngle = 0f
                        var foundCategory: CategoryBreakdown? = null
                        
                        for (i in categories.indices) {
                            val sweepAngle = angles[i]
                            val endAngle = currentStartAngle + sweepAngle
                            
                            // Check if touch angle falls within this slice
                            if (touchAngle >= currentStartAngle && touchAngle < endAngle) {
                                foundCategory = categories[i]
                                selectedAngle = currentStartAngle + (sweepAngle / 2f)
                                break
                            }
                            currentStartAngle = endAngle
                        }
                        
                        if (selectedCategory == foundCategory) {
                            // Toggle off if clicking the same slice again
                            selectedCategory = null
                        } else {
                            selectedCategory = foundCategory
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = 0f
                for (i in categories.indices) {
                    val sweepAngle = angles[i]
                    val color = getColorForCategory(categories[i].category)
                    
                    // Highlight selected slice slightly by changing its radius or color 
                    val isSelected = selectedCategory == categories[i]
                    val alpha = if (selectedCategory == null || isSelected) 1f else 0.5f
                    
                    drawArc(
                        color = color.copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(size.width, size.height),
                        style = Fill
                    )
                    startAngle += sweepAngle
                }
            }
            
            
            if (selectedCategory != null) {
                val color = getColorForCategory(selectedCategory!!.category)
                val radiusDp = 90.dp
                val angleRad = Math.toRadians(selectedAngle.toDouble())
                val offsetX = (Math.cos(angleRad) * radiusDp.value).dp
                val offsetY = (Math.sin(angleRad) * radiusDp.value).dp
                
                androidx.compose.material3.Card(
                    modifier = Modifier.offset(x = offsetX, y = offsetY),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${selectedCategory!!.percentage.roundToInt()}% ${selectedCategory!!.category}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        val formattedAmount = NumberFormat.getNumberInstance(Locale("es", "CO")).format(selectedCategory!!.amount)
                        Text(
                            text = "$ $formattedAmount COP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedCategory == null) {
            Text("Toca una porción del gráfico para ver detalles", color = Color.Gray, modifier = Modifier.padding(16.dp))
        }
    }
}
