package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import com.google.firebase.Timestamp

data class UserViewModel(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val createdAt: Timestamp = Timestamp.now()
)