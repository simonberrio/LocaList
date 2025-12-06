package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.UserViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.navigation.TopBar
//import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var user by remember { mutableStateOf<UserViewModel?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    var editedName by remember { mutableStateOf("") }
    var editedBio by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val loadedUser = snapshot.toObject(UserViewModel::class.java)
                    user = loadedUser
                    editedName = loadedUser?.name ?: ""
                    editedBio = loadedUser?.bio ?: ""
                }
            }
    }

    if (user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isCurrentUser = userId == currentUserId
    val isFriend = user!!.friends.contains(currentUserId)
    val hasSentRequest = user!!.friendRequests.contains(currentUserId)

    var showLogoutDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopBar(
                title = "Perfil",
                showBack = !isCurrentUser,
                onBack = onBack,
                onLogout = { showLogoutDialog = true }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
//            AsyncImage(
//                model = user!!.photoUrl.ifBlank {
//                    "https://ui-avatars.com/api/?name=${user!!.name}"
//                },
//                contentDescription = null,
//                modifier = Modifier
//                    .size(110.dp)
//                    .align(Alignment.CenterHorizontally)
//            )

            Spacer(Modifier.height(16.dp))

            // 👤 Nombre
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(user!!.name, style = MaterialTheme.typography.headlineSmall)
            }

            Spacer(Modifier.height(8.dp))

            // 📧 Email
            Text(user!!.email, style = MaterialTheme.typography.labelMedium)

            Spacer(Modifier.height(12.dp))

            // 📝 Bio
            if (isEditing) {
                OutlinedTextField(
                    value = editedBio,
                    onValueChange = { editedBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    if (user!!.bio.isBlank()) "Sin biografía"
                    else user!!.bio,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))

            if (isCurrentUser) {
                if (isEditing) {
                    Button(
                        onClick = {
                            firestore.collection("users")
                                .document(userId)
                                .update(
                                    mapOf(
                                        "name" to editedName,
                                        "bio" to editedBio
                                    )
                                )
                            isEditing = false
                        }
                    ) {
                        Text("Guardar cambios")
                    }
                } else {
                    OutlinedButton(
                        onClick = { isEditing = true }
                    ) {
                        Text("Editar perfil")
                    }
                }
            } else {
                when {
                    isFriend -> {
                        OutlinedButton(onClick = {
                            removeFriend(firestore, currentUserId!!, userId)
                        }) {
                            Text("Eliminar amigo")
                        }
                    }

                    hasSentRequest -> {
                        OutlinedButton(onClick = {}) {
                            Text("Solicitud enviada")
                        }
                    }

                    user!!.friendRequests.contains(userId) -> {
                        Button(onClick = {
                            acceptFriendRequest(firestore, currentUserId!!, userId)
                        }) {
                            Text("Aceptar solicitud")
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(onClick = {
                            rejectFriendRequest(firestore, currentUserId!!, userId)
                        }) {
                            Text("Rechazar")
                        }
                    }

                    else -> {
                        Button(onClick = {
                            sendFriendRequest(firestore, currentUserId!!, userId)
                        }) {
                            Text("Agregar amigo")
                        }
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

fun sendFriendRequest(
    firestore: FirebaseFirestore,
    myId: String,
    friendId: String
) {
    val friendRef = firestore.collection("users").document(friendId)

    friendRef.update(
        "friendRequests",
        com.google.firebase.firestore.FieldValue.arrayUnion(myId)
    )
}

fun acceptFriendRequest(
    firestore: FirebaseFirestore,
    myId: String,
    friendId: String
) {
    val myRef = firestore.collection("users").document(myId)
    val friendRef = firestore.collection("users").document(friendId)

    myRef.update(
        mapOf(
            "friendRequests" to com.google.firebase.firestore.FieldValue.arrayRemove(friendId),
            "friends" to com.google.firebase.firestore.FieldValue.arrayUnion(friendId)
        )
    )

    friendRef.update(
        "friends",
        com.google.firebase.firestore.FieldValue.arrayUnion(myId)
    )
}

fun rejectFriendRequest(
    firestore: FirebaseFirestore,
    myId: String,
    friendId: String
) {
    firestore.collection("users")
        .document(myId)
        .update(
            "friendRequests",
            com.google.firebase.firestore.FieldValue.arrayRemove(friendId)
        )
}

fun removeFriend(
    firestore: FirebaseFirestore,
    myId: String,
    friendId: String
) {
    val myRef = firestore.collection("users").document(myId)
    val friendRef = firestore.collection("users").document(friendId)

    myRef.update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(friendId))
    friendRef.update("friends", com.google.firebase.firestore.FieldValue.arrayRemove(myId))
}