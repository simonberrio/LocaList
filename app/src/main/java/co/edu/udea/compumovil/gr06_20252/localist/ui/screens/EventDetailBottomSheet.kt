package co.edu.udea.compumovil.gr06_20252.localist.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventViewModel
import com.google.firebase.firestore.FirebaseFirestore
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
    onAddComment: (String) -> Unit
) {
    var selectedEvent by remember { mutableStateOf<EventViewModel?>(null) }

    // Listener Firestore → actualiza reacciones y todo el evento
    LaunchedEffect(eventId) {
        firestore.collection("events")
            .document(eventId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {
                    selectedEvent = snapshot.toObject(EventViewModel::class.java)
                }
            }
    }

    // Mientras carga
    if (selectedEvent == null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val event = selectedEvent!!

    val createdInstant = event.createdAt.toDate().toInstant()
    val localDateTime = createdInstant.atZone(ZoneId.systemDefault()).toLocalDateTime()
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")

    val expirationMillis =
        event.createdAt.toDate().time + TimeUnit.HOURS.toMillis(event.durationHours.toLong())
    val nowMillis = com.google.firebase.Timestamp.now().toDate().time
    val remainingMillis = (expirationMillis - nowMillis).coerceAtLeast(0L)

    val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
    val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60

    var newComment by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = event.title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (event.isPublic) "Público" else "Privado",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = event.description.ifBlank { "Sin descripción" },
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Duración: ${event.durationHours} h",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = if (remainingMillis > 0) "Tiempo restante: ${remainingHours}h ${remainingMinutes}m"
            else "Evento expirado",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                IconButton(onClick = { onReact("👍") }) { Text("👍") }
                Text("${event.reactions["👍"] ?: 0}")
            }

            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                IconButton(onClick = { onReact("❤️") }) { Text("❤️") }
                Text("${event.reactions["❤️"] ?: 0}")
            }

            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                IconButton(onClick = { onReact("🔥") }) { Text("🔥") }
                Text("${event.reactions["🔥"] ?: 0}")
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
            OutlinedButton(onClick = onClose) {
                Text("Cerrar")
            }
        }
    }
}