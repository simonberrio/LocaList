package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}
class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun signUp(email: String, password: String, name: String) {
        if (email.isBlank() || password.length < 6 || name.isBlank()) {
            viewModelScope.launch { _messages.send("Completa todos los campos (contraseña ≥ 6).") }
            return
        }
        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    // Guardar perfil en Firestore
                    val profile = UserProfile(uid = uid, name = name, email = email)
                    firestore.collection("users").document(uid).set(profile)
                        .addOnSuccessListener {
                            viewModelScope.launch { _messages.send("Registro exitoso") }
                        }
                        .addOnFailureListener { e ->
                            viewModelScope.launch { _messages.send("Error guardando perfil: ${e.message}") }
                        }
                }
                .addOnFailureListener { e ->
                    viewModelScope.launch { _messages.send("Error registrando: ${e.message}") }
                }
        }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            viewModelScope.launch { _messages.send("Correo y contraseña requeridos.") }
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Error al iniciar sesión.") }
    }

    fun signOut() {
        auth.signOut()
        viewModelScope.launch { _messages.send("Sesión cerrada") }
    }

    fun currentUserUid(): String? = auth.currentUser?.uid
}
