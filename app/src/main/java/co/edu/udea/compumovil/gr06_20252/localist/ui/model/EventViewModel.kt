package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class EventSerializable(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

data class EventViewModel(
    var id: String = "",
    var title: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var description: String = "",
    var isPublic: Boolean = true,
    var durationHours: Int = 1,
    var createdAt: Timestamp = Timestamp.now(),
    val userId: String = "",
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
        val creationMillis = createdAt.toDate().time
        val expirationMillis = creationMillis + TimeUnit.HOURS.toMillis(durationHours.toLong())

        val nowMillis = Timestamp.now().toDate().time

        return nowMillis > expirationMillis
    }

    fun validateEvent(event: EventViewModel): String? {
        if (event.title.isBlank()) return "El título no puede estar vacío"
        if (event.description.isBlank()) return "La descripción no puede estar vacía"
        if (event.durationHours <= 0) return "La duración debe ser mayor que 0"
        if (event.durationHours > 72) return "La duración debe ser menor que 72"
        if (event.latitude == 0.0 && event.longitude == 0.0) return "Debe seleccionar una ubicación válida"

        return null // Todo ok
    }
}