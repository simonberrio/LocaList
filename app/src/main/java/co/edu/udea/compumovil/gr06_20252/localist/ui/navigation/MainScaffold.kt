package co.edu.udea.compumovil.gr06_20252.localist.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.FriendsScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.MapScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.ProfileScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainScaffold(
    onLogout: () -> Unit
) {

    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "map",
            modifier = Modifier.padding(padding)
        ) {

            composable("map") {
                MapScreen(
                    onLogout = onLogout,
                    onGoToProfile = { userId ->
                        navController.navigate("profile/$userId") {
                            popUpTo("map") { inclusive = true }
                        }
                    }
                )
            }

            composable("friends") {
                FriendsScreen(
                    onLogout = onLogout,
                    onGoToProfile = { userId ->
                        navController.navigate("profile/$userId") {
                            popUpTo("map") { inclusive = true }
                        }
                    }
                )
            }

            composable("profile/{userId}") { backStack ->
                val userId = backStack.arguments?.getString("userId")!!
                ProfileScreen(
                    userId = userId,
                    onBack = {
                        navController.navigate("friends") {
                            popUpTo("profile/$userId") { inclusive = true }
                        }
                    },
                    onLogout = onLogout
                )
            }
        }
    }
}
