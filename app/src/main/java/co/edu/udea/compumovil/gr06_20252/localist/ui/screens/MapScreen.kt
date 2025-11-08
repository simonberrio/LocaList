package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
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
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventMarker
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.MapViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var eventMarkers by remember { mutableStateOf(listOf<EventMarker>()) }
    var newMarkerPosition by remember { mutableStateOf<LatLng?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Escucha en tiempo real los eventos
    LaunchedEffect(Unit) {
        firestore.collection("events").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            eventMarkers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(EventMarker::class.java)?.apply { id = doc.id }
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
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
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

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
            properties = MapProperties(isMyLocationEnabled = locationPermission.status.isGranted),
            onMapLongClick = { latLng ->
                newMarkerPosition = latLng
                coroutineScope.launch {
                    bottomSheetState.show()
                }
            }
        ) {
            // Filtra y muestra eventos válidos (no expirados)
            val now = LocalDateTime.now()
            eventMarkers.filter { !it.isExpired()}.forEach { event ->
                Marker(
                    state = MarkerState(position = event.toLatLng()),
                    title = event.title,
                    snippet = event.description
                )
            }
        }

        // BottomSheet para crear evento
        if (newMarkerPosition != null) {
            ModalBottomSheet(
                onDismissRequest = { newMarkerPosition = null },
                sheetState = bottomSheetState
            ) {
                AddEventBottomSheet(
                    onSave = { title, description, isPublic, duration ->
                        val newEvent = EventMarker(
                            title = title,
                            latitude = newMarkerPosition!!.latitude,
                            longitude = newMarkerPosition!!.longitude,
                            description = description,
                            isPublic = isPublic,
                            durationHours = duration,
                            createdAt = com.google.firebase.Timestamp.now()
                        )
                        addEventToFirestore(firestore, newEvent)
                        eventMarkers = eventMarkers + newEvent
                        newMarkerPosition = null
                        coroutineScope.launch { bottomSheetState.hide() }
                    }
                )
            }
        }
    }
}

private fun addEventToFirestore(
    firestore: FirebaseFirestore,
    newEvent: EventMarker
) {
    firestore.collection("events")
        .add(newEvent)
        .addOnSuccessListener {
            android.util.Log.d("Firestore", "Evento agregado correctamente con ID ${it.id}")
        }
        .addOnFailureListener { e ->
            android.util.Log.e("Firestore", "Error al agregar evento", e)
        }
}