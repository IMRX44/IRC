package com.irblaster.universal.data

/**
 * Builds a full manual remote (all buttons) for a brand.
 *
 * - If the brand has a hand-verified layout in IrDatabase, that is used as-is.
 * - Otherwise a category-appropriate STANDARD layout is generated from the
 *   brand's representative device address. Most NEC TVs/receivers share the same
 *   command map (only the address differs), so this is a genuine best-effort —
 *   and once Smart Scan confirms the exact working address, buildFromCode()
 *   produces a layout that matches the real device.
 */
object RemoteBuilder {

    // Common NEC command maps (byte per function) ---------------------------
    private val tvMap = linkedMapOf(
        "Power" to 0x08, "Vol +" to 0x02, "Vol -" to 0x03, "Mute" to 0x09,
        "CH +" to 0x00, "CH -" to 0x01, "Menu" to 0x47, "Source" to 0x0B,
        "OK" to 0x44, "Back" to 0x0E, "Home" to 0x0C, "▲" to 0x40,
        "▼" to 0x41, "◄" to 0x07, "►" to 0x06,
        "1" to 0x11, "2" to 0x12, "3" to 0x13, "4" to 0x14, "5" to 0x15,
        "6" to 0x16, "7" to 0x17, "8" to 0x18, "9" to 0x19, "0" to 0x10,
    )
    private val projMap = linkedMapOf(
        "Power" to 0x08, "Source" to 0x0B, "Menu" to 0x47, "Vol +" to 0x02,
        "Vol -" to 0x03, "Mute" to 0x09, "Auto" to 0x0A, "Blank" to 0x0C,
        "▲" to 0x40, "▼" to 0x41, "◄" to 0x07, "►" to 0x06, "OK" to 0x44,
    )
    private val stbMap = linkedMapOf(
        "Power" to 0x08, "CH +" to 0x00, "CH -" to 0x01, "Vol +" to 0x02,
        "Vol -" to 0x03, "Menu" to 0x47, "OK" to 0x44, "Back" to 0x0E,
        "Guide" to 0x4C, "Info" to 0x4D, "▲" to 0x40, "▼" to 0x41,
        "◄" to 0x07, "►" to 0x06,
        "1" to 0x11, "2" to 0x12, "3" to 0x13, "4" to 0x14, "5" to 0x15,
        "6" to 0x16, "7" to 0x17, "8" to 0x18, "9" to 0x19, "0" to 0x10,
    )
    private val dvdMap = linkedMapOf(
        "Power" to 0x08, "Play" to 0x2C, "Pause" to 0x30, "Stop" to 0x38,
        "Next" to 0x28, "Prev" to 0x29, "FF" to 0x34, "RW" to 0x33,
        "Menu" to 0x47, "OK" to 0x44, "Eject" to 0x2E,
    )
    private val soundMap = linkedMapOf(
        "Power" to 0x08, "Vol +" to 0x02, "Vol -" to 0x03, "Mute" to 0x09,
        "Source" to 0x0B, "Bluetooth" to 0x5B, "Bass +" to 0x13, "Bass -" to 0x14,
    )
    private val fanMap = linkedMapOf(
        "Power" to 0x08, "Speed +" to 0x02, "Speed -" to 0x03, "Swing" to 0x0C,
        "Timer" to 0x0D, "Mode" to 0x0E, "Natural" to 0x0F,
    )
    private val lightMap = linkedMapOf(
        "On" to 0x40, "Off" to 0x41, "Bright +" to 0x5C, "Bright -" to 0x5D,
        "Red" to 0x58, "Green" to 0x59, "Blue" to 0x45, "White" to 0x44,
        "Flash" to 0x47, "Fade" to 0x4F, "Strobe" to 0x4E, "Smooth" to 0x4D,
    )

    private fun mapFor(category: String) = when (category) {
        "tv" -> tvMap
        "projector" -> projMap
        "stb" -> stbMap
        "dvd" -> dvdMap
        "sound" -> soundMap
        "fan" -> fanMap
        "light" -> lightMap
        else -> tvMap
    }

    /** Standard NEC remote for a brand at a representative address. */
    fun buildNec(brandName: String, category: String, addr: Int, freq: Int = 38000): DeviceBrand {
        val signals = mapFor(category).map { (name, cmd) ->
            IrSignal(name, freq, IrDatabase.nec(addr, cmd), "A=0x${addr.toString(16).uppercase()}")
        }
        return DeviceBrand(brandName, signals)
    }

    /** Full remote generated from a confirmed Smart-Scan power code. */
    fun buildFromCode(brandName: String, category: String, code: PowerCode): DeviceBrand {
        return when (code.protocol) {
            "Samsung" -> DeviceBrand(brandName, mapFor(category).map { (name, cmd) ->
                IrSignal(name, code.frequency, IrDatabase.samsung(code.addr.coerceAtLeast(0), cmd), "Samsung")
            })
            "SIRC" -> DeviceBrand(brandName, mapFor(category).map { (name, cmd) ->
                val f = IrDatabase.sony12(code.addr.coerceAtLeast(0), cmd)
                IrSignal(name, code.frequency, f + f + f, "Sony")
            })
            "NEC" -> buildNec(brandName, category, code.addr.coerceAtLeast(0), code.frequency)
            else -> DeviceBrand(brandName, listOf(
                IrSignal("Power", code.frequency, code.pattern, code.label)
            ))
        }
    }

    /**
     * The full brand list for a category's manual remote screen: every Smart-Scan
     * brand, using its verified layout when available, otherwise a generated one.
     */
    fun brandsForRemote(category: String): List<DeviceBrand> {
        val curated: Map<String, DeviceBrand> =
            (IrDatabase.categories.firstOrNull { it.id == category }?.brands ?: emptyList())
                .associateBy { it.name }

        val result = ArrayList<DeviceBrand>()
        val seen = HashSet<String>()

        for (profile in PowerScan.brandsFor(category)) {
            if (profile.brand.startsWith("⚡")) continue // universal sweep isn't a manual remote
            val verified = curated[profile.brand]
            if (verified != null) {
                result += verified
            } else {
                val rep = profile.codes.firstOrNull { it.addr >= 0 }
                val brand = if (rep != null)
                    buildFromCode(profile.brand, category, rep)
                else
                    DeviceBrand(profile.brand, listOf(
                        IrSignal("Power", profile.codes.first().frequency,
                            profile.codes.first().pattern, profile.brand)
                    ))
                result += brand
            }
            seen += profile.brand
        }
        // Include any curated brands that weren't in the scan list
        for ((name, brand) in curated) if (name !in seen) result += brand
        return result
    }

    /** True when the brand's layout was hand-verified (not generated). */
    fun isVerified(category: String, brandName: String): Boolean =
        (IrDatabase.categories.firstOrNull { it.id == category }?.brands ?: emptyList())
            .any { it.name == brandName }
}
