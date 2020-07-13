package axxes.prototype.trucktracker.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import axxes.prototype.trucktracker.model.ContextState

class ViewModelContextState: ViewModel() {

    private val _isGpsEnabled = MutableLiveData<Boolean>()
    val isGpsEnabled: LiveData<Boolean>
        get() = _isGpsEnabled

    private val _isBluetoothEnabled = MutableLiveData<Boolean>()
    val isBluetoothEnabled: LiveData<Boolean>
        get() = _isBluetoothEnabled

    private val _isNetworkEnabled = MutableLiveData<Boolean>()
    val isNetworkEnabled: LiveData<Boolean>
        get() = _isNetworkEnabled

    init{
        _isGpsEnabled.value = false
        _isBluetoothEnabled.value = false
        _isNetworkEnabled.value = false
    }

    fun updateContextState(states: ContextState){
        _isGpsEnabled.value = states.isGpsEnabled
        _isBluetoothEnabled.value = states.isBluetoothEnabled
        _isNetworkEnabled.value = states.isNetworkEnabled
    }
}