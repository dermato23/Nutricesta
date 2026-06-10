package com.example.consumointeligente.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.graphics.Color

sealed class BottomNavItem(val route: String, val icon: ImageVector, val title: String, val color: Color) {
    object Home : BottomNavItem("home", Icons.Default.Home, "Mi Mercado", Color(0xFF4CAF50))
    object Recipes : BottomNavItem("recipes", Icons.Default.RestaurantMenu, "Recetas", Color(0xFFF57F17))
    object Stats : BottomNavItem("stats", Icons.Default.ShoppingCart, "Mi Nutrición", Color(0xFF2196F3))
    object SmartList : BottomNavItem("smart_list", Icons.Default.List, "Mi Lista", Color(0xFFFF9800))
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Recipes,
        BottomNavItem.Stats,
        BottomNavItem.SmartList
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = item.color,
                    selectedTextColor = item.color,
                    indicatorColor = item.color.copy(alpha = 0.2f)
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
