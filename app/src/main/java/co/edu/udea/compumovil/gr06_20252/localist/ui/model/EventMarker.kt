package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.util.concurrent.TimeUnit

data class EventMarker(
    var id: String = "",
    var title: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var description: String = "",
    var isPublic: Boolean = true,
    var createdAt: Timestamp = Timestamp.now(),
    var durationHours: Int = 1,
    var comments: MutableList<String> = mutableListOf(),
    var reactions: MutableMap<String, Int> = mutableMapOf(
        "👍" to 0,
        "❤️" to 0,
        "🔥" to 0
    )
) {
    @Exclude
    fun toLatLng() = com.google.android.gms.maps.model.LatLng(latitude, longitude)

    @Exclude
    fun isExpired(): Boolean {
        val expirationMillis = createdAt.toDate().time + TimeUnit.HOURS.toMillis(durationHours.toLong())
        return System.currentTimeMillis() > expirationMillis
    }
}