package co.edu.udea.compumovil.gr06_20252.localist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.AuthViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.LoginScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.MapScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.RegisterScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "login"
            ) {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("register") {
                    RegisterScreen(navController)
                }
                composable("map") {
                    MapScreen(navController)
                }
            }
        }
    }
}