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
import co.edu.udea.compumovil.gr06_20252.localist.ui.navigation.MainScaffold
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.LoginScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.MapScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.ProfileScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.RegisterScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.theme.LocaListTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocaListTheme {

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

                    composable("main") {
                        MainScaffold()
                    }
                }
            }
        }
    }
}