package com.example.consumointeligente.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SmartListScreen() {
    val items = listOf("Detergente", "Papel Higiénico", "Alimento para Gato")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Lista Inteligente", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Basado en tus ciclos de abastecimiento", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(items.size) { index ->
                ListItem(
                    headlineContent = { Text(items[index]) },
                    leadingContent = {
                        Icon(Icons.Default.Check, contentDescription = "Check")
                    }
                )
                Divider()
            }
        }
    }
}
