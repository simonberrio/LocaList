package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.CommentViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var eventViewModels by remember { mutableStateOf(listOf<EventViewModel>()) }
    var newMarkerPosition by remember { mutableStateOf<LatLng?>(null) }
    var selectedEvent by remember { mutableStateOf<EventViewModel?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Escucha en tiempo real los eventos
    LaunchedEffect(Unit) {
        firestore.collection("events").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            eventViewModels = snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventViewModel::class.java)?.apply { id = doc.id }
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        }
    }
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        location?.let {
                            userLocation = LatLng(it.latitude, it.longitude)
                        }
                    }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState()
    // Cuando obtenga la ubicación, mover la cámara automáticamente
    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(
                    LatLng(loc.latitude, loc.longitude),
                    16f
                ),
                durationMs = 1000
            )
        }
    }
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    if (firebaseUser == null) {
        LaunchedEffect(Unit) {
            navController.navigate("login") {
                popUpTo("map") { inclusive = true }
            }
        }
        return
    }

    val uid = firebaseUser.uid
    var userName by remember { mutableStateOf("") }
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    userName = doc.getString("name") ?: doc.getString("email") ?: "Usuario"
                }
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    LaunchedEffect(selectedEvent) {
        if (selectedEvent != null) {
            sheetState.show()
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val hasLocationPermission = locationPermission.status.isGranted

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                onMapLongClick = { latLng ->
                    newMarkerPosition = latLng
                    coroutineScope.launch {
                        bottomSheetState.show()
                    }
                }
            ) {
                // Muestra eventos no expirados
                eventViewModels.filter { !it.isExpired() }.forEach { event ->
                    Marker(
                        state = MarkerState(position = event.toLatLng()),
                        title = event.title,
                        snippet = event.description,
                        onClick = {
                            // Cuando se haga click en un marker, abrimos el detalle
                            selectedEvent = event
                            coroutineScope.launch { bottomSheetState.show() }
                            // Retornar true si queremos consumir el evento (evita que InfoWindow se abra)
                            true
                        }
                    )
                }
            }

            // BottomSheet para crear evento
            if (newMarkerPosition != null) {
                ModalBottomSheet(
                    onDismissRequest = { newMarkerPosition = null },
                    sheetState = bottomSheetState
                ) {
                    SnackbarHost(hostState = snackbarHostState)

                    AddEventBottomSheet(
                        onSave = { title, description, isPublic, duration ->
                            val newEvent = EventViewModel(
                                title = title,
                                latitude = newMarkerPosition!!.latitude,
                                longitude = newMarkerPosition!!.longitude,
                                description = description,
                                isPublic = isPublic,
                                durationHours = duration,
                                createdAt = com.google.firebase.Timestamp.now(),
                                userId = uid,
                                userName = userName
                            )

                            val errorMessage = newEvent.validateEvent(newEvent)

                            if (errorMessage != null) {
                                showSnackbar(errorMessage)
                            } else {
                                addEventToFirestore(
                                    firestore,
                                    newEvent,
                                    onSuccess = { showSnackbar("Evento creado exitosamente") },
                                    onError = { showSnackbar("Error al crear el evento") }
                                )
                                newMarkerPosition = null
                                coroutineScope.launch { bottomSheetState.hide() }
                            }
                        }
                    )
                }
            }

            // BottomSheet para ver detalles del evento (si hay uno seleccionado)
            if (selectedEvent != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedEvent = null },
                    sheetState = bottomSheetState
                ) {
                    EventDetailBottomSheet(
                        eventId = selectedEvent!!.id,
                        firestore,
                        onClose = {
                            selectedEvent = null
                            coroutineScope.launch { bottomSheetState.hide() }
                        },
                        onReact = { emoji ->
                            selectedEvent?.id?.let { eventId ->
                                updateReaction(firestore, eventId, emoji)
                            }
                        },
                        onAddComment = { comment ->
                            selectedEvent?.id?.let { eventId ->
                                addCommentToEvent(firestore, eventId, comment, uid, userName)
                            }
                        },
                        onDeleteEvent = {
                            selectedEvent?.id?.let { eventId ->
                                deleteEventToFirestore(
                                    firestore,
                                    eventId = eventId,
                                    onSuccess = { showSnackbar("Evento eliminado exitosamente") },
                                    onError = { showSnackbar("Error al eliminar el evento") }
                                )
                            }

                        }
                    )
                }
            }
        }
    }
}

private fun addEventToFirestore(
    firestore: FirebaseFirestore,
    newEvent: EventViewModel,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    firestore.collection("events")
        .add(newEvent)
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

private fun updateReaction(
    firestore: FirebaseFirestore,
    eventId: String,
    emoji: String
) {
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
    userId: String,
    userName: String
) {
    val comment = CommentViewModel(
        eventId = eventId,
        text = text,
        createdAt = com.google.firebase.Timestamp.now(),
        userId = userId,
        userName = userName
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