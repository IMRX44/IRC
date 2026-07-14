package com.irblaster.universal.data

import android.content.Context
import android.hardware.ConsumerIrManager
import android.os.Build

class IrTransmitter(context: Context) {
    private val irManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    } else null

    val hasIrBlaster: Boolean get() = irManager?.hasIrEmitter() == true

    fun transmit(signal: IrSignal): Boolean = transmitRaw(signal.frequency, signal.pattern)

    /** Blocking transmit of a raw pattern. Blocks for the frame duration, which
     *  naturally paces back-to-back scanning at the hardware's real max rate. */
    fun transmitRaw(frequency: Int, pattern: IntArray): Boolean {
        if (!hasIrBlaster) return false
        return try {
            irManager?.transmit(frequency, pattern)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSupportedFrequencies(): List<IntRange> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return emptyList()
        return irManager?.carrierFrequencies?.map { it.minFrequency..it.maxFrequency } ?: emptyList()
    }
}
