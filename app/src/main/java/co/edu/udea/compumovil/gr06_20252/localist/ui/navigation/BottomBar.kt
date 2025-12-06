package co.edu.udea.compumovil.gr06_20252.localist.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BottomBar(navController: NavController) {

    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.primary
    ) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("map") },
            icon = { Icon(Icons.Default.Place, null) },
            label = { Text("Mapa") }
        )

        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("friends") },
            icon = { Icon(Icons.Default.Face, null) },
            label = { Text("Amigos") }
        )

        NavigationBarItem(
            selected = false,
            onClick = {
                uid?.let {
                    navController.navigate("profile/$it")
                }
            },
            icon = { Icon(Icons.Default.AccountCircle, null) },
            label = { Text("Perfil") }
        )
    }
}