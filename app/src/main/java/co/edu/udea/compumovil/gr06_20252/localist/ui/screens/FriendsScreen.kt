package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.UserViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.navigation.TopBar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun FriendsScreen(
    onLogout: () -> Unit,
    onGoToProfile: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    if (firebaseUser == null) {
        LaunchedEffect(Unit) {
            onLogout()
        }
        return
    }

    val uid = firebaseUser.uid

    var friends by remember { mutableStateOf<List<UserViewModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        friends = loadFriends(firestore, uid)
        loading = false
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopBar(
                title = "Amigos",
                onLogout = { showLogoutDialog = true }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            if (loading) {
                CircularProgressIndicator()
            } else {
                LazyColumn {
                    items(friends) { friend ->
                        FriendItem(
                            user = friend,
                            actionText = "Ver perfil",
                            onActionClick = { onGoToProfile(friend.uid) }
                        )
                    }
                }
            }
        }


        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Cerrar sesión") },
                text = { Text("¿Realmente deseas cerrar sesión?") },
                confirmButton = {
                    Button(onClick = {
                        showLogoutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }) {
                        Text("Sí, cerrar sesión")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun FriendItem(
    user: UserViewModel,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text(user.name, style = MaterialTheme.typography.bodyLarge)
            Text(user.email, style = MaterialTheme.typography.bodySmall)
        }

        Button(onClick = onActionClick) {
            Text(actionText)
        }
    }
}

private suspend fun loadFriends(
    firestore: FirebaseFirestore,
    uid: String
): List<UserViewModel> {
    val userDoc = firestore.collection("users").document(uid).get().await()
    val friendsList = userDoc.get("friends") as? List<String> ?: emptyList()

    if (friendsList.isEmpty()) return emptyList()

    val friendsDocs = firestore.collection("users")
        .whereIn("uid", friendsList)
        .get()
        .await()

    return friendsDocs.documents.mapNotNull { it.toObject(UserViewModel::class.java) }
}