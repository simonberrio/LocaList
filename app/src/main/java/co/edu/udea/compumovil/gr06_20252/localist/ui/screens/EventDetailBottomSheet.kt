package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.CommentViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailBottomSheet(
    eventId: String,
    firestore: FirebaseFirestore,
    onClose: () -> Unit,
    onReact: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteEvent: () -> Unit,
    onViewProfile: (String) -> Unit
) {
    var selectedEvent by remember { mutableStateOf<EventViewModel?>(null) }
    var comments by remember { mutableStateOf<List<CommentViewModel>>(emptyList()) }

    val eventListener = remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }
    val commentsListener = remember { mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null) }

    LaunchedEffect(eventId) {
        eventListener.value = firestore.collection("events")
            .document(eventId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    selectedEvent = snapshot.toObject(EventViewModel::class.java)
                }
            }
    }

    LaunchedEffect(eventId) {
        commentsListener.value = firestore.collection("events")
            .document(eventId)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    comments = snapshot.documents.mapNotNull {
                        it.toObject(CommentViewModel::class.java)
                    }
                }
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            eventListener.value?.remove()
            commentsListener.value?.remove()
        }
    }
    val event = selectedEvent
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
    val nowMillis = com.google.firebase.Timestamp.now().toDate().time
    val remainingMillis = (expirationMillis - nowMillis).coerceAtLeast(0L)

    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60

    var newComment by remember { mutableStateOf("") }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.90f)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(text = event.title, style = MaterialTheme.typography.titleLarge)

            if (currentUserId == event.userId) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar evento",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar"
                )
            }
        }

        Text(
            text = if (event.isPublic) "Público" else "Privado",
            style = MaterialTheme.typography.bodySmall
        )

        Text(
            text = "Creado por: ${event.userId}",
            modifier = Modifier.clickable {
                onViewProfile(event.userId)
            },
            color = MaterialTheme.colorScheme.primary
        )

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
                IconButton(onClick = { onReact("👍") }) { Text("👍") }
                Text("${reactions["👍"] ?: 0}")
            }

            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                IconButton(onClick = { onReact("❤️") }) { Text("❤️") }
                Text("${reactions["❤️"] ?: 0}")
            }

            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                IconButton(onClick = { onReact("🔥") }) { Text("🔥") }
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
                            Text(
                                text = comment.userId,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.clickable { onViewProfile(comment.userId) },
                                color = MaterialTheme.colorScheme.primary
                            )
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
                    onAddComment(newComment.trim())
                    newComment = ""
                }
            }) {
                Text("Enviar")
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
                        showDeleteDialog = false
                        onDeleteEvent()
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