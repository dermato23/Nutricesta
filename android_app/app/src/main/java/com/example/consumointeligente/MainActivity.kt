package com.example.consumointeligente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.consumointeligente.ui.components.BottomNavBar
import com.example.consumointeligente.ui.screens.HomeScreen
import com.example.consumointeligente.ui.screens.SmartListScreen
import com.example.consumointeligente.ui.screens.StatsScreen
import com.example.consumointeligente.ui.screens.RecipesScreen
import com.example.consumointeligente.theme.ConsumoInteligenteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsumoInteligenteTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen()
            }
            composable("recipes") {
                RecipesScreen()
            }
            composable("stats") {
                StatsScreen()
            }
            composable("smart_list") {
                SmartListScreen()
            }
        }
    }
}
