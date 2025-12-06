package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.CommentViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventSerializable
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.navigation.TopBar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

fun formatDate(timestamp: Timestamp): String {
    val instant = timestamp.toDate().toInstant()
    val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm")
    return localDateTime.format(formatter)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: EventSerializable,
    onLogout: () -> Unit,
    onClose: () -> Unit,
    onGoToEventEditor: (EventSerializable) -> Unit,
    onGoToProfile: (String) -> Unit
) {
    if (event.id.isEmpty()) onClose()
    val firestore = FirebaseFirestore.getInstance()
    var selectedEvent by remember { mutableStateOf<EventViewModel?>(null) }
    var comments by remember { mutableStateOf<List<CommentViewModel>>(emptyList()) }
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    DisposableEffect(event.id) {
        val listener = firestore.collection("events")
            .document(event.id)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->

                if (e != null || snapshot == null) return@addSnapshotListener

                val newComments = snapshot.documents.mapNotNull {
                    it.toObject(CommentViewModel::class.java)
                }

                comments = newComments

                // ✅ Cargar nombres solo de los userId nuevos
                val missingUserIds = newComments
                    .map { it.userId }
                    .filter { it.isNotBlank() && !userNames.containsKey(it) }
                    .distinct()

                missingUserIds.forEach { uid ->
                    firestore.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val name = doc.getString("name") ?: doc.getString("email") ?: "Usuario"
                            userNames = userNames + (uid to name)
                        }
                }
            }

        onDispose {
            listener.remove()
        }
    }
    DisposableEffect(event.id) {
        val listener = firestore.collection("events")
            .document(event.id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    selectedEvent = snapshot.toObject(EventViewModel::class.java)
                }
            }

        onDispose {
            listener.remove()
        }
    }
    DisposableEffect(event.id) {

        val listener = firestore.collection("events")
            .document(event.id)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->

                if (e != null) {
                    Log.e("Firestore", "Error escuchando comentarios", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    comments = snapshot.documents.mapNotNull {
                        it.toObject(CommentViewModel::class.java)
                    }
                }
            }

        onDispose {
            listener.remove()
        }
    }

    var event = selectedEvent
    if (event == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    val duration = event.durationHours ?: 0
    val reactions = event.reactions ?: emptyMap()

    val expirationMillis =
        event.createdAt.toDate().time + TimeUnit.HOURS.toMillis(duration.toLong())
    val nowMillis = Timestamp.now().toDate().time
    val remainingMillis = (expirationMillis - nowMillis).coerceAtLeast(0L)

    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60

    var newComment by remember { mutableStateOf("") }

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    if (firebaseUser == null) {
        LaunchedEffect(Unit) {
            onLogout()
        }
        return
    }
    val uid = firebaseUser.uid

    Scaffold(
        topBar = {
            TopBar(
                title = event.title,
                showBack = true,
                onBack = { onClose() },
                onLogout = onLogout
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (event.isPublic) "Público" else "Privado",
                style = MaterialTheme.typography.bodySmall
            )


            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(
                    onClick = { onGoToProfile(event.userId) },
                    enabled = event.userId.isNotBlank()
                )
                {
                    Text(
                        text = "Creado por: ${userNames[event.userId] ?: "Cargando..."}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uid == event.userId) {
                    IconButton(onClick = {
                        val eventSerializable = EventSerializable(
                            id = event.id,
                            latitude = event.latitude,
                            longitude = event.longitude,
                        )
                        onGoToEventEditor(eventSerializable)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = event.description.ifBlank { "Sin descripción" },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Duración: ${event.durationHours} h",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = if (remainingMillis > 0) "Tiempo restante: ${remainingHours}h ${remainingMinutes}m"
                else "Evento expirado",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateReaction(firestore, event.id, "👍") }) { Text("👍") }
                    Text("${reactions["👍"] ?: 0}")
                }

                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    IconButton(onClick = {
                        updateReaction(
                            firestore,
                            event.id,
                            "❤️"
                        )
                    }) { Text("❤️") }
                    Text("${reactions["❤️"] ?: 0}")
                }

                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    IconButton(onClick = { updateReaction(firestore, event.id, "🔥") }) { Text("🔥") }
                    Text("${reactions["🔥"] ?: 0}")
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Comentarios", style = MaterialTheme.typography.titleMedium)

            if (comments.isEmpty()) {
                Text("Aún no hay comentarios")
            } else {
                LazyColumn(modifier = Modifier.fillMaxHeight()) {
                    items(comments) { comment ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(
                                    onClick = { onGoToProfile(comment.userId) },
                                    enabled = comment.userId.isNotBlank()
                                ) {
                                    Text(
                                        text = userNames[comment.userId] ?: "Cargando...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = formatDate(comment.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(text = comment.text, style = MaterialTheme.typography.bodyMedium)
                            Divider(modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = newComment,
                onValueChange = { newComment = it },
                label = { Text("Agregar comentario") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (newComment.isNotBlank()) {
                        addCommentToEvent(firestore, event.id, newComment.trim(), uid)
                        newComment = ""
                    }
                }) {
                    Text("Enviar")
                }
            }
        }
    }
}

private fun updateReaction(
    firestore: FirebaseFirestore,
    eventId: String,
    emoji: String
) {
    Log.d("Firestore", "Reacción '$emoji' agregada al evento $eventId")
    firestore.collection("events")
        .document(eventId)
        .update("reactions.$emoji", com.google.firebase.firestore.FieldValue.increment(1))
        .addOnSuccessListener {
            Log.d("Firestore", "Reacción '$emoji' agregada al evento $eventId")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error actualizando reacción", e)
        }
}

fun addCommentToEvent(
    firestore: FirebaseFirestore,
    eventId: String,
    text: String,
    userId: String
) {
    val comment = CommentViewModel(
        eventId = eventId,
        text = text,
        createdAt = com.google.firebase.Timestamp.now(),
        userId = userId
    )

    firestore.collection("events")
        .document(eventId)
        .collection("comments")
        .add(comment)
        .addOnSuccessListener {
            Log.d("Firestore", "Comentario '$comment' agregado al evento $eventId")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error actualizando el comentario", e)
        }
}