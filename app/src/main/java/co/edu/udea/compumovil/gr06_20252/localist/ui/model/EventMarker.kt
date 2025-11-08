package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import com.google.android.gms.maps.model.LatLng
import java.time.LocalDateTime

data class EventMarker(
    val id: Int,
    val title: String,
    val position: LatLng,
    val description: String = "",
    val isPublic: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val durationHours: Int = 1
)