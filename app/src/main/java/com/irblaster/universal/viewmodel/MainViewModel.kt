package com.irblaster.universal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irblaster.universal.data.DeviceBrand
import com.irblaster.universal.data.DeviceCategory
import com.irblaster.universal.data.IrDatabase
import com.irblaster.universal.data.IrSignal
import com.irblaster.universal.data.IrTransmitter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanState(
    val isScanning: Boolean = false,
    val totalSignals: Int = 0,
    val currentIndex: Int = 0,
    val currentSignal: IrSignal? = null,
    val markedWorking: List<IrSignal> = emptyList(),
    val delayMs: Long = 80L,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val transmitter = IrTransmitter(application)
    val categories: List<DeviceCategory> = IrDatabase.categories

    private val _selectedCategory = MutableStateFlow<DeviceCategory?>(null)
    val selectedCategory: StateFlow<DeviceCategory?> = _selectedCategory.asStateFlow()

    private val _selectedBrand = MutableStateFlow<DeviceBrand?>(null)
    val selectedBrand: StateFlow<DeviceBrand?> = _selectedBrand.asStateFlow()

    private val _lastTransmitted = MutableStateFlow<IrSignal?>(null)
    val lastTransmitted: StateFlow<IrSignal?> = _lastTransmitted.asStateFlow()

    private val _scanState = MutableStateFlow(ScanState())
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private var scanJob: Job? = null

    fun selectCategory(category: DeviceCategory) {
        _selectedCategory.value = category
        _selectedBrand.value = null
    }

    fun selectBrand(brand: DeviceBrand) {
        _selectedBrand.value = brand
    }

    fun transmitSignal(signal: IrSignal) {
        transmitter.transmit(signal)
        _lastTransmitted.value = signal
    }

    fun startBruteForceScan(categoryId: String, delayMs: Long = 80L) {
        val signals = IrDatabase.generateBruteForceSignals()

        scanJob?.cancel()
        _scanState.value = ScanState(
            isScanning = true,
            totalSignals = signals.size,
            currentIndex = 0,
            delayMs = delayMs
        )

        scanJob = viewModelScope.launch {
            signals.forEachIndexed { index, signal ->
                val current = _scanState.value
                if (!current.isScanning) return@launch

                _scanState.value = current.copy(
                    currentIndex = index,
                    currentSignal = signal
                )
                transmitter.transmit(signal)
                delay(current.delayMs)
            }
            _scanState.value = _scanState.value.copy(isScanning = false)
        }
    }

    fun pauseScan() {
        scanJob?.cancel()
        _scanState.value = _scanState.value.copy(isScanning = false)
    }

    fun markCurrentWorking() {
        val state = _scanState.value
        val signal = state.currentSignal ?: return
        _scanState.value = state.copy(
            markedWorking = state.markedWorking + signal
        )
    }

    fun resetScan() {
        scanJob?.cancel()
        _scanState.value = ScanState()
    }

    override fun onCleared() {
        scanJob?.cancel()
        super.onCleared()
    }
}
