package com.irblaster.universal.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irblaster.universal.data.BrandProfile
import com.irblaster.universal.data.DeviceBrand
import com.irblaster.universal.data.DeviceCategory
import com.irblaster.universal.data.IrDatabase
import com.irblaster.universal.data.IrSignal
import com.irblaster.universal.data.IrTransmitter
import com.irblaster.universal.data.PowerCode
import com.irblaster.universal.data.PowerScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Smart brand-driven power scan state. */
data class SmartScanState(
    val active: Boolean = false,
    val running: Boolean = false,
    val categoryId: String = "",
    val brand: String = "",
    val codes: List<PowerCode> = emptyList(),
    val index: Int = 0,
    val foundCode: PowerCode? = null,
    val finished: Boolean = false,
    val gapMs: Long = 0L,
) {
    val current: PowerCode? get() = codes.getOrNull(index)
    val progress: Float get() = if (codes.isEmpty()) 0f else (index + 1f) / codes.size
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val transmitter = IrTransmitter(application)
    val categories: List<DeviceCategory> = IrDatabase.categories

    private val _selectedCategory = MutableStateFlow<DeviceCategory?>(null)
    val selectedCategory: StateFlow<DeviceCategory?> = _selectedCategory.asStateFlow()

    private val _selectedBrand = MutableStateFlow<DeviceBrand?>(null)
    val selectedBrand: StateFlow<DeviceBrand?> = _selectedBrand.asStateFlow()

    private val _lastTransmitted = MutableStateFlow<IrSignal?>(null)
    val lastTransmitted: StateFlow<IrSignal?> = _lastTransmitted.asStateFlow()

    private val _smart = MutableStateFlow(SmartScanState())
    val smart: StateFlow<SmartScanState> = _smart.asStateFlow()

    private var smartJob: Job? = null

    fun selectCategory(category: DeviceCategory) {
        _selectedCategory.value = category
        _selectedBrand.value = null
    }

    fun selectBrand(brand: DeviceBrand) {
        _selectedBrand.value = brand
    }

    fun transmitSignal(signal: IrSignal) {
        viewModelScope.launch(Dispatchers.IO) { transmitter.transmit(signal) }
        _lastTransmitted.value = signal
    }

    // ── Smart brand scan ─────────────────────────────────────────────────────

    fun brandProfilesFor(categoryId: String): List<BrandProfile> = PowerScan.brandsFor(categoryId)

    fun startSmartScan(categoryId: String, brand: String, gapMs: Long = 0L) {
        val codes = PowerScan.forBrand(categoryId, brand)
        smartJob?.cancel()
        _smart.value = SmartScanState(
            active = true, running = true, categoryId = categoryId,
            brand = brand, codes = codes, index = 0, gapMs = gapMs
        )
        runSmartLoop()
    }

    private fun runSmartLoop() {
        smartJob?.cancel()
        smartJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val s = _smart.value
                if (!s.running || s.foundCode != null) break
                val code = s.codes.getOrNull(s.index) ?: run {
                    _smart.value = s.copy(running = false, finished = true)
                    return@launch
                }
                transmitter.transmitRaw(code.frequency, code.pattern)
                if (s.gapMs > 0) delay(s.gapMs)
                val cur = _smart.value
                if (!cur.running) break
                if (cur.index >= cur.codes.lastIndex) {
                    _smart.value = cur.copy(running = false, finished = true)
                    break
                }
                _smart.value = cur.copy(index = cur.index + 1)
            }
        }
    }

    fun pauseSmart() {
        _smart.value = _smart.value.copy(running = false)
        smartJob?.cancel()
    }

    fun resumeSmart() {
        if (_smart.value.foundCode != null) return
        _smart.value = _smart.value.copy(running = true, finished = false)
        runSmartLoop()
    }

    /** Manual step forward: pauses auto, sends the next code once. */
    fun stepNext() {
        smartJob?.cancel()
        val s = _smart.value
        val next = (s.index + 1).coerceAtMost(s.codes.lastIndex)
        _smart.value = s.copy(running = false, index = next, finished = false)
        resendCurrent()
    }

    fun stepPrev() {
        smartJob?.cancel()
        val s = _smart.value
        val prev = (s.index - 1).coerceAtLeast(0)
        _smart.value = s.copy(running = false, index = prev, finished = false)
        resendCurrent()
    }

    fun resendCurrent() {
        val code = _smart.value.current ?: return
        viewModelScope.launch(Dispatchers.IO) { transmitter.transmitRaw(code.frequency, code.pattern) }
    }

    /** User confirmed the device reacted — lock in this code. */
    fun confirmWorked() {
        smartJob?.cancel()
        val s = _smart.value
        _smart.value = s.copy(running = false, foundCode = s.current)
    }

    fun exitSmart() {
        smartJob?.cancel()
        _smart.value = SmartScanState()
    }

    override fun onCleared() {
        smartJob?.cancel()
        super.onCleared()
    }
}
