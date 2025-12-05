package co.edu.udea.compumovil.gr06_20252.localist.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Map : BottomNavItem("map", Icons.Default.Place, "Mapa")
    object Friends : BottomNavItem("friends", Icons.Default.Face, "Amigos")
    object Profile : BottomNavItem("profile", Icons.Default.AccountCircle, "Perfil")
}