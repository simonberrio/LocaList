package co.edu.udea.compumovil.gr06_20252.localist.ui.model

import android.location.Location
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MapViewModel constructor() : ViewModel() {
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _eventViewModels = mutableStateListOf<EventViewModel>()
    val eventViewModels: List<EventViewModel> get() = _eventViewModels
}