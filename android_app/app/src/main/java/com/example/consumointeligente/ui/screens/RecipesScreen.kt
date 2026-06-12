package com.example.consumointeligente.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.consumointeligente.network.RetrofitClient
import com.example.consumointeligente.network.FinancialStatsResponse
import com.example.consumointeligente.network.RecipePreferences
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen() {
    val coroutineScope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<FinancialStatsResponse?>(null) }
    var preferences by remember { mutableStateOf<RecipePreferences?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRegenerating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showPreferencesModal by remember { mutableStateOf(false) }
    var showSetupScreen by remember { mutableStateOf(false) }

    // Cargar datos iniciales (recetas y preferencias)
    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            try {
                preferences = RetrofitClient.apiService.getRecipePreferences()
                stats = RetrofitClient.apiService.getFinancialStats()
                
                // Si el usuario no tiene configuradas preferencias (ej: time o goal vacíos), se muestra setup inicial
                if (preferences == null || preferences!!.time.isEmpty() || preferences!!.goal.isEmpty()) {
                    showSetupScreen = true
                }
            } catch (e: Exception) {
                errorMessage = "Error cargando datos: ${e.message}"
                Log.e("RecipesScreen", "Error Fetching Data", e)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF1E3A1E))
        }
    } else if (showSetupScreen) {
        // Pantalla de configuración inicial de Preferencias
        PreferencesForm(
            initialPrefs = preferences ?: RecipePreferences(),
            title = "Preferencias de Recetas",
            onSave = { newPrefs ->
                showSetupScreen = false
                isRegenerating = true
                coroutineScope.launch {
                    try {
                        RetrofitClient.apiService.updateRecipePreferences(newPrefs)
                        // Volver a cargar para ver las nuevas recetas regeneradas
                        preferences = newPrefs
                        stats = RetrofitClient.apiService.getFinancialStats()
                    } catch (e: Exception) {
                        Log.e("RecipesScreen", "Error saving preferences", e)
                    } finally {
                        isRegenerating = false
                    }
                }
            },
            onDismiss = {
                // Si ya había preferencias cargadas, simplemente cerrar, de lo contrario obligar a guardar
                if (preferences != null && preferences!!.time.isNotEmpty()) {
                    showSetupScreen = false
                }
            },
            showCloseButton = false
        )
    } else {
        // Pantalla Principal de Recetas
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera de Recetas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recetas Sugeridas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    
                    // Botón para editar preferencias
                    IconButton(
                        onClick = { showPreferencesModal = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes de Preferencias",
                            tint = Color(0xFF1E3A1E)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isRegenerating) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF1E3A1E),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Regenerando recetas con tus preferencias...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A1E)
                            )
                        }
                    }
                }
                
                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                } else if (stats != null && stats!!.recipes.isNotEmpty()) {
                    Text(
                        text = "Aprovecha tu mercado de esta semana:",
                        fontSize = 16.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    stats!!.recipes.forEach { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Día y Título
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFBEF264))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = recipe.day,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E3A1E)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = recipe.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = recipe.description,
                                    fontSize = 14.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 20.sp
                                )
                                
                                if (recipe.protein_pct != null && recipe.carbs_pct != null && recipe.lipids_pct != null) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Proteínas: ${recipe.protein_pct}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF8B5CF6) // Morado
                                        )
                                        Text(
                                            text = "Carbs: ${recipe.carbs_pct}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF97316) // Naranja
                                        )
                                        Text(
                                            text = "Grasas: ${recipe.lipids_pct}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D9488) // Teal
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay recetas disponibles.\nSube una factura de mercado para comenzar.",
                            textAlign = TextAlign.Center,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
            
            // Modal de edición de preferencias
            if (showPreferencesModal) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showPreferencesModal = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.Center)
                            .clickable(enabled = false) {}, // Evitar que clics en la tarjeta cierren el modal
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        PreferencesForm(
                            initialPrefs = preferences ?: RecipePreferences(),
                            title = "Editar Preferencias",
                            onSave = { newPrefs ->
                                showPreferencesModal = false
                                isRegenerating = true
                                coroutineScope.launch {
                                    try {
                                        RetrofitClient.apiService.updateRecipePreferences(newPrefs)
                                        preferences = newPrefs
                                        stats = RetrofitClient.apiService.getFinancialStats()
                                    } catch (e: Exception) {
                                        Log.e("RecipesScreen", "Error updating preferences", e)
                                    } finally {
                                        isRegenerating = false
                                    }
                                }
                            },
                            onDismiss = { showPreferencesModal = false },
                            showCloseButton = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferencesForm(
    initialPrefs: RecipePreferences,
    title: String,
    onSave: (RecipePreferences) -> Unit,
    onDismiss: () -> Unit,
    showCloseButton: Boolean
) {
    var selectedTime by remember { mutableStateOf(initialPrefs.time.ifEmpty { "Menos de 30 min" }) }
    var selectedDiets by remember { mutableStateOf(initialPrefs.diets) }
    var selectedAllergies by remember { mutableStateOf(initialPrefs.allergies) }
    var selectedGoal by remember { mutableStateOf(initialPrefs.goal.ifEmpty { "Comer saludable" }) }
    var selectedDishTypes by remember { mutableStateOf(initialPrefs.dish_types) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Título del Formulario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E3A1E)
            )
            if (showCloseButton) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 1. Tiempo de Preparación
        Text(
            text = "Tiempo de preparación",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val timeOptions = listOf("Menos de 15 min", "Menos de 30 min")
        FlowChipsGroup(
            options = timeOptions,
            selectedOptions = listOf(selectedTime),
            onSelectionChanged = { selectedTime = it.first() },
            isMultiSelect = false
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 2. Dietas
        Text(
            text = "¿Sigues alguna de las siguientes dietas?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val dietOptions = listOf("Vegana", "Vegetariana", "Pescatariana", "Keto", "Paleo", "Baja en carbohidratos")
        FlowChipsGroup(
            options = dietOptions,
            selectedOptions = selectedDiets,
            onSelectionChanged = { selectedDiets = it },
            isMultiSelect = true
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 3. Alergias
        Text(
            text = "¿Alguna alergia o intolerancia alimentaria?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val allergyOptions = listOf("Gluten", "Lácteos", "Huevo", "Soya", "Pescado", "Maní", "Nueces", "Mariscos")
        FlowChipsGroup(
            options = allergyOptions,
            selectedOptions = selectedAllergies,
            onSelectionChanged = { selectedAllergies = it },
            isMultiSelect = true
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 4. Objetivos
        Text(
            text = "¿Cuál es tu objetivo?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val goalOptions = listOf("Comer saludable", "Económico", "Planificar mejor", "Aprender a cocinar", "Rápido y fácil")
        FlowChipsGroup(
            options = goalOptions,
            selectedOptions = listOf(selectedGoal),
            onSelectionChanged = { selectedGoal = it.first() },
            isMultiSelect = false
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // 5. Tipo de Plato
        Text(
            text = "Tipo de plato",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B)
        )
        Spacer(modifier = Modifier.height(8.dp))
        val dishOptions = listOf("Desayuno", "Brunch", "Almuerzo", "Entradas", "Snack", "Postre", "Cena", "Bebidas")
        FlowChipsGroup(
            options = dishOptions,
            selectedOptions = selectedDishTypes,
            onSelectionChanged = { selectedDishTypes = it },
            isMultiSelect = true
        )
        
        Spacer(modifier = Modifier.height(28.dp))
        
        // Botón Guardar
        Button(
            onClick = {
                onSave(
                    RecipePreferences(
                        time = selectedTime,
                        diets = selectedDiets,
                        allergies = selectedAllergies,
                        goal = selectedGoal,
                        dish_types = selectedDishTypes
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A1E), // Dark Green
                contentColor = Color(0xFFBEF264)   // Lime Green
            )
        ) {
            Text("Guardar Preferencias", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun FlowChipsGroup(
    options: List<String>,
    selectedOptions: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    isMultiSelect: Boolean
) {
    // Organizar elementos en filas para evitar colisiones
    val chunkedOptions = options.chunked(2)
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        chunkedOptions.forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    val bgColor = if (isSelected) Color(0xFF1E3A1E) else Color(0xFFF1F5F9)
                    val textColor = if (isSelected) Color(0xFFBEF264) else Color(0xFF475569)
                    val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor)
                            .then(borderModifier)
                            .clickable {
                                if (isMultiSelect) {
                                    val newSelection = if (isSelected) {
                                        selectedOptions - option
                                    } else {
                                        selectedOptions + option
                                    }
                                    onSelectionChanged(newSelection)
                                } else {
                                    onSelectionChanged(listOf(option))
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFBEF264),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = option,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
