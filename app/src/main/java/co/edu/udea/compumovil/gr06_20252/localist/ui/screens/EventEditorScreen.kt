package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventSerializable
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.navigation.TopBar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    event: EventSerializable,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    val isEditing = event.id.isNotEmpty()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    var eventId by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationHours by remember { mutableStateOf("1") }
    var createdAt by remember { mutableStateOf<Timestamp?>(null) }
    var userId by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }


    var loading by remember { mutableStateOf(isEditing) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    if (firebaseUser == null) {
        LaunchedEffect(Unit) {
            onLogout()
        }
        return
    }

    val uid = firebaseUser.uid
//    var userName by remember { mutableStateOf("") }
//    LaunchedEffect(uid) {
//        if (uid.isNotEmpty()) {
//            firestore.collection("users")
//                .document(uid)
//                .get()
//                .addOnSuccessListener { doc ->
//                    userName = doc.getString("name") ?: doc.getString("email") ?: "Usuario"
//                }
//        }
//    }

    LaunchedEffect(event.id) {
        if (isEditing) {
            firestore.collection("events")
                .document(event.id)
                .get()
                .addOnSuccessListener { doc ->
                    val ev = doc.toObject(EventViewModel::class.java)
                    if (ev != null) {
                        eventId = ev.id
                        title = ev.title
                        description = ev.description
                        durationHours = ev.durationHours?.toString() ?: "1"
                        isPublic = ev.isPublic
                        createdAt = ev.createdAt
                        userId = ev.userId
                    }
                    loading = false
                }
                .addOnFailureListener {
                    error = "Error cargando el evento"
                    loading = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                title = if (isEditing) "Editar evento" else "Crear evento",
                showBack = true,
                onBack = { onClose() },
                onLogout = onLogout
            )
        }
    ) { padding ->

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = durationHours,
                onValueChange = { durationHours = it },
                label = { Text("Duración (horas)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isPublic,
                    onCheckedChange = { isPublic = it }
                )
                Text("Evento público")
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isEditing) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar evento",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    onClick = {
                        if (isEditing) {
                            val newEvent = EventViewModel(
                                id = eventId,
                                title = title,
                                latitude = event.latitude,
                                longitude = event.longitude,
                                description = description,
                                isPublic = isPublic,
                                durationHours = durationHours.toIntOrNull() ?: 1,
                                createdAt = createdAt!!,
                                userId = userId
                            )
                            Log.d("newEvent.latitude", newEvent.latitude.toString())
                            Log.d("newEvent.longitude", newEvent.longitude.toString())
                            val errorMessage = newEvent.validateEvent(newEvent)
                            Log.d("errorMessage", errorMessage ?: "")

                            if (errorMessage != null) {
                                showSnackbar(errorMessage)
                            } else {
                                updateEventInFirestore(
                                    firestore,
                                    event.id,
                                    newEvent,
                                    onSuccess = {
                                        showSnackbar("Evento actualizado exitosamente")
                                        onClose()
                                    },
                                    onError = { showSnackbar("Error al actualizar el evento") }
                                )
                            }

                        } else {
                            val newEvent = EventViewModel(
                                title = title,
                                latitude = event.latitude,
                                longitude = event.longitude,
                                description = description,
                                isPublic = isPublic,
                                durationHours = durationHours.toIntOrNull() ?: 1,
                                createdAt = Timestamp.now(),
                                userId = uid
                            )

                            val errorMessage = newEvent.validateEvent(newEvent)

                            if (errorMessage != null) {
                                showSnackbar(errorMessage)
                            } else {
                                addEventToFirestore(
                                    firestore,
                                    newEvent,
                                    onSuccess = {
                                        showSnackbar("Evento creado exitosamente")
                                        onClose()
                                    },
                                    onError = { showSnackbar("Error al crear el evento") }
                                )
                            }
                        }
                    }
                ) {
                    Text(if (isEditing) "Guardar cambios" else "Crear evento")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar evento") },
            text = { Text("¿Realmente deseas eliminar este evento? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteEventToFirestore(
                            firestore,
                            event.id,
                            onSuccess = {
                                showSnackbar("Evento creado exitosamente")
                            },
                            onError = { showSnackbar("Error al crear el evento") }
                        )
                        showDeleteDialog = false
                        onClose()
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun addEventToFirestore(
    firestore: FirebaseFirestore,
    newEvent: EventViewModel,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val docRef = firestore.collection("events").document()

    val eventWithId = newEvent.copy(id = docRef.id)

    docRef.set(eventWithId)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError() }
}

private fun deleteEventToFirestore(
    firestore: FirebaseFirestore,
    eventId: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    firestore.collection("events")
        .document(eventId)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError() }
}

private fun updateEventInFirestore(
    firestore: FirebaseFirestore,
    eventId: String,
    updatedEvent: EventViewModel,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    Log.d("updatedEvent", updatedEvent.id)
    firestore.collection("events")
        .document(eventId)
        .set(updatedEvent)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onError() }
}