package co.edu.udea.compumovil.gr06_20252.localist.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.*

@Composable
fun MainScaffold() {

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
                MapScreen(navController)
            }

            composable("friends") {
                FriendsScreen()
            }

            composable("profile") {
                ProfileScreen(
                    userId = uid ?: "",
                    onBack = {}
                )
            }

            composable("profile/{userId}") { backStack ->
                val userId = backStack.arguments?.getString("userId")!!
                ProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}