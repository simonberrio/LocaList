package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import com.google.firebase.Timestamp

data class CommentViewModel(
    var id: String = "",
    var eventId: String = "",
    var text: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var userId: String = ""
)