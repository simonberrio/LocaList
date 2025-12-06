package co.edu.udea.compumovil.gr06_20252.localist.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventSerializable
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.EventDetailScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.EventEditorScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.FriendsScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.MapScreen
import co.edu.udea.compumovil.gr06_20252.localist.ui.screens.ProfileScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Composable
fun MainScaffold(
    onLogout: () -> Unit
) {

    val navController = rememberNavController()
//    val auth = FirebaseAuth.getInstance()
//    var event = EventSerializable()

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
//                    onGoToProfile = { userId ->
//                        navController.navigate("profile/$userId") {
//                            popUpTo("map") { inclusive = true }
//                        }
//                    },
                    onGoToEventEditor = { eventSerializable ->
                        val json = Uri.encode(Json.encodeToString(eventSerializable))
                        navController.navigate("eventEditor/$json")
                    },
                    onGoToEventDetail = { eventSerializable ->
                        val json = Uri.encode(Json.encodeToString(eventSerializable))
                        navController.navigate("eventDetail/$json")
                    }
//                    onGoToEventEditor = { eventViewModel ->
//                        navController.navigate("eventEditor")
//                    }
                )
            }

            composable("eventEditor/{event}") { backStack ->
                val json = backStack.arguments?.getString("event")!!
                val event = Json.decodeFromString<EventSerializable>(json)

                EventEditorScreen(
                    event = event,
                    onLogout = onLogout,
                    onClose = { navController.popBackStack() }
                )
            }

            composable("eventDetail/{event}") { backStack ->
                val json = backStack.arguments?.getString("event")!!
                val event = Json.decodeFromString<EventSerializable>(json)

                EventDetailScreen(
                    event = event,
                    onLogout = onLogout,
                    onClose = { navController.popBackStack() },
                    onGoToProfile = { userId ->
                        navController.navigate("profile/$userId")
                    },
                    onGoToEventEditor = { eventSerializable ->
                        val json = Uri.encode(Json.encodeToString(eventSerializable))
                        navController.navigate("eventEditor/$json")
                    }
                )
            }


            composable("friends") {
                FriendsScreen(
                    onLogout = onLogout,
                    onGoToProfile = { userId ->
                        navController.navigate("eventEditor/$userId")
                    }
                )
            }

            composable("profile/{userId}") { backStack ->
                val userId = backStack.arguments?.getString("userId")!!
                ProfileScreen(
                    userId = userId,
                    onBack = {
                        navController.popBackStack()
                    },
                    onLogout = onLogout
                )
            }
        }
    }
}
