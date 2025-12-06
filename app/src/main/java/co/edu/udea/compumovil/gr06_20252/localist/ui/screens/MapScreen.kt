package co.edu.udea.compumovil.gr06_20252.localist.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventSerializable
import co.edu.udea.compumovil.gr06_20252.localist.ui.model.EventViewModel
import co.edu.udea.compumovil.gr06_20252.localist.ui.navigation.TopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onLogout: () -> Unit,
    onGoToEventEditor: (EventSerializable) -> Unit,
    onGoToEventDetail: (EventSerializable) -> Unit
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var eventViewModels by remember { mutableStateOf(listOf<EventViewModel>()) }
    var newMarkerPosition by remember { mutableStateOf<LatLng?>(null) }

    var showLogoutDialog by remember { mutableStateOf(false) }

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
            onLogout()
        }
        return
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Mapa",
                onLogout = { showLogoutDialog = true }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val hasLocationPermission = locationPermission.status.isGranted

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                onMapLongClick = { latLng ->
                    newMarkerPosition = latLng
                    val newEvent = EventSerializable(
                        id = "",
                        latitude = newMarkerPosition!!.latitude,
                        longitude = newMarkerPosition!!.longitude,
                    )
                    onGoToEventEditor(newEvent)
                }
            ) {
                eventViewModels.filter { !it.isExpired() }.forEach { event ->
                    Marker(
                        state = MarkerState(position = event.toLatLng()),
                        title = event.title,
                        snippet = event.description,
                        onClick = {
                            val eventSerializable = EventSerializable(
                                id = event.id,
                                latitude = event.latitude,
                                longitude = event.longitude
                            )
                            onGoToEventDetail(eventSerializable)
                            true
                        }
                    )
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