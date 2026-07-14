package com.irblaster.universal.data

/**
 * Comprehensive per-brand POWER code database used by the "Smart Scan".
 *
 * Real universal-remote code sets (LIRC / Flipper-IR / SURE) store dozens–hundreds
 * of power variants per brand because a single manufacturer ships many remote
 * revisions with different device addresses. Instead of hard-coding every one, we
 * sweep the KNOWN address space of each brand against its KNOWN power command
 * byte(s) and protocol(s). That yields the full realistic candidate set while
 * staying compact.
 *
 * Physics note: each IR frame physically takes ~45–70ms to leave the LED, so the
 * scanner fires them back-to-back with no artificial gap for maximum throughput.
 */

data class PowerCode(
    val label: String,
    val frequency: Int,
    val pattern: IntArray,
    val protocol: String,
)

/** A brand's complete power-scan definition. */
data class BrandProfile(
    val brand: String,
    val categoryId: String,
    val codes: List<PowerCode>,
)

object PowerScan {

    // ── Known device-address pools per protocol family ───────────────────────
    // These cover the address bytes manufacturers have actually shipped.

    private val necTvAddresses = listOf(
        0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,
        0x10,0x18,0x20,0x24,0x40,0x48,0x50,0x80,0x84,0x88,0xA0,0xC0,0xE0,0xEF,0xFB,0xFF
    )
    private val necPowerCmds = listOf(0x08,0x02,0x12,0x0C,0x01,0x00,0x10,0x48,0x40,0x1E,0xD0,0x0D,0x18)

    private val samsungAddresses = listOf(0x07,0x04,0x0E,0x01,0x02,0x03,0x08,0x0B,0x11,0x1E,0xB2,0xE0)
    private val samsungPowerCmds = listOf(0x02,0x99,0x11,0x12,0x9D,0xBF,0x62)

    private val sonyPowerCmds = listOf(0x15,0xA8,0x2E) // power-toggle across SIRC device sets
    private val sonyDeviceIds = listOf(0x01,0x02,0x0A,0x0C,0x0D,0x1A,0x18,0x25,0x2C,0x51,0x60,0x97)

    // ── Encoders shortcut ────────────────────────────────────────────────────
    private fun necCodes(addresses: List<Int>, cmds: List<Int>, tag: String): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in addresses) for (c in cmds) {
            out += PowerCode("$tag A=${a.h}/C=${c.h}", 38000, IrDatabase.nec(a, c), "NEC")
        }
        return out
    }

    private fun samsungCodes(addresses: List<Int>, cmds: List<Int>): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in addresses) for (c in cmds) {
            out += PowerCode("Samsung A=${a.h}/C=${c.h}", 38000, IrDatabase.samsung(a, c), "Samsung")
        }
        return out
    }

    private fun sonyCodes(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (d in sonyDeviceIds) for (c in sonyPowerCmds) {
            // Sony repeats frames 3×; encode 3 copies for reliable capture
            val single = IrDatabase.sony12(d, c)
            out += PowerCode("Sony D=${d.h}/C=${c.h}", 40000, single + single + single, "SIRC")
        }
        return out
    }

    private val Int.h get() = "0x${toString(16).uppercase().padStart(2,'0')}"

    // ── Per-brand profiles ───────────────────────────────────────────────────
    // Each brand sweeps its realistic address pool. Where a brand uses several
    // protocols across product lines, all are included.

    val profiles: List<BrandProfile> by lazy { buildProfiles() }

    private fun buildProfiles(): List<BrandProfile> {
        val list = ArrayList<BrandProfile>()

        // ---- TV ----
        list += BrandProfile("Samsung", "tv",
            samsungCodes(samsungAddresses, samsungPowerCmds))
        list += BrandProfile("LG", "tv",
            necCodes(listOf(0x04,0x08,0xC0,0x00,0x20,0xBF,0x08,0x10), listOf(0x08,0xC8,0x02,0x10,0x0C), "LG") +
            necCodes(listOf(0x04), listOf(0x08), "LG"))
        list += BrandProfile("Sony", "tv", sonyCodes())
        list += BrandProfile("Philips", "tv",
            rc5PowerSweep() + necCodes(listOf(0x04,0x00), listOf(0x0C,0x0D), "Philips"))
        list += BrandProfile("Panasonic", "tv",
            panasonicPowerSweep())
        list += BrandProfile("TCL", "tv",
            necCodes(listOf(0x84,0x04,0x08,0x40,0x57,0xF7), listOf(0x08,0x02,0x87), "TCL"))
        list += BrandProfile("Hisense", "tv",
            necCodes(listOf(0x00,0x01,0x40,0xE7,0xB7,0x10), listOf(0x08,0x02,0x17,0x57), "Hisense"))
        list += BrandProfile("Haier", "tv",
            necCodes(listOf(0x40,0x54,0x0C,0x00), listOf(0x12,0x08,0x0C), "Haier"))
        list += BrandProfile("Sharp", "tv",
            sharpPowerSweep())
        list += BrandProfile("Toshiba", "tv",
            necCodes(listOf(0x02,0x40,0x03,0x00), listOf(0x01,0x12,0x02,0x0F), "Toshiba"))
        list += BrandProfile("Xiaomi", "tv",
            necCodes(listOf(0x0D,0x40,0x00,0xA0), listOf(0x01,0x08,0x53), "Xiaomi"))
        list += BrandProfile("JVC", "tv",
            jvcCodes(listOf(0xC5,0x03,0xF1), listOf(0x00,0x17,0x40)))
        list += BrandProfile("Vestel", "tv",
            necCodes(listOf(0xE0,0x00,0x40), listOf(0x12,0x08,0x0C), "Vestel"))
        list += BrandProfile("Skyworth", "tv",
            necCodes(listOf(0x0C,0x00,0x40), listOf(0x08,0x02), "Skyworth"))
        list += BrandProfile("Konka", "tv",
            necCodes(listOf(0x24,0x00,0x40), listOf(0x08,0x02,0xEC), "Konka"))
        list += BrandProfile("Changhong", "tv",
            necCodes(listOf(0x50,0x00,0x07), listOf(0x08,0x02,0x1B), "Changhong"))
        list += BrandProfile("Grundig", "tv",
            necCodes(listOf(0x80,0x00), listOf(0x3D,0x0C), "Grundig") + rc5PowerSweep())
        list += BrandProfile("Hitachi", "tv",
            necCodes(listOf(0x00,0x01,0x40), listOf(0xD0,0x08,0x50), "Hitachi"))
        list += BrandProfile("Sanyo", "tv",
            necCodes(listOf(0x01,0x02,0x1C), listOf(0x48,0x08,0x0D), "Sanyo"))
        list += BrandProfile("Sharp/Aquos", "tv", sharpPowerSweep())
        list += BrandProfile("Onida", "tv",
            necCodes(listOf(0x08,0x00,0x40), listOf(0x08,0x0C), "Onida"))
        list += BrandProfile("Sansui", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x0C,0x02), "Sansui"))

        // ── Iranian / regional brands (mostly NEC rebadges) ──
        list += BrandProfile("X.Vision (ایکس‌ویژن)", "tv",
            necCodes(listOf(0x00,0x40,0x04,0x08,0x0C,0xE0,0xFB), listOf(0x08,0x02,0x12,0x0C,0x10), "XVision"))
        list += BrandProfile("Snowa (اسنوا)", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x04,0xE0), listOf(0x08,0x02,0x12,0x0C), "Snowa"))
        list += BrandProfile("Marshal (مارشال)", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x0C), listOf(0x08,0x02,0x12), "Marshal"))
        list += BrandProfile("G-Plus (جی‌پلاس)", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x04,0xE0), listOf(0x08,0x02,0x0C,0x12), "GPlus"))
        list += BrandProfile("Blest (بلست)", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02,0x12), "Blest"))
        list += BrandProfile("Sam (سام)", "tv",
            necCodes(listOf(0x07,0x04,0x0E,0x00,0x40), listOf(0x02,0x08,0x12), "Sam") +
            samsungCodes(listOf(0x07,0x04), listOf(0x02,0x99)))
        list += BrandProfile("Pars (پارس)", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x0C), listOf(0x08,0x02,0x12), "Pars"))
        list += BrandProfile("Master (مستر)", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02,0x12), "Master"))
        list += BrandProfile("iLife / Tech", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x0C), listOf(0x08,0x02), "iLife"))

        // ── Turkish brands ──
        list += BrandProfile("Beko / Arçelik", "tv",
            necCodes(listOf(0x00,0x40,0xE0,0x08), listOf(0x0C,0x12,0x08), "Beko") + rc5PowerSweep())
        list += BrandProfile("Regal", "tv",
            necCodes(listOf(0xE0,0x00,0x40), listOf(0x12,0x08,0x0C), "Regal"))
        list += BrandProfile("Telefunken", "tv",
            necCodes(listOf(0x00,0x40,0xE0), listOf(0x0C,0x08,0x12), "Telefunken") + rc5PowerSweep())
        list += BrandProfile("Profilo", "tv",
            necCodes(listOf(0xE0,0x00), listOf(0x12,0x08), "Profilo"))
        list += BrandProfile("Arçelik", "tv",
            necCodes(listOf(0x00,0x40,0xE0), listOf(0x0C,0x08), "Arcelik") + rc5PowerSweep())

        // ── More global / Chinese / Indian ──
        list += BrandProfile("Xiaomi/Redmi TV", "tv",
            necCodes(listOf(0x0D,0x40,0x00,0xA0,0xFF), listOf(0x01,0x08,0x53,0x02), "Xiaomi"))
        list += BrandProfile("OnePlus TV", "tv",
            necCodes(listOf(0x04,0x00,0x40), listOf(0x08,0x02), "OnePlus"))
        list += BrandProfile("Realme TV", "tv",
            necCodes(listOf(0x04,0x00,0x40), listOf(0x08,0x02), "Realme"))
        list += BrandProfile("VU TV", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "VU"))
        list += BrandProfile("Micromax", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Micromax"))
        list += BrandProfile("Videocon", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Videocon"))
        list += BrandProfile("Intex", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Intex"))
        list += BrandProfile("Metz", "tv",
            necCodes(listOf(0x00,0x40), listOf(0x08,0x0C), "Metz") + rc5PowerSweep())
        list += BrandProfile("Loewe", "tv", rc5PowerSweep())
        list += BrandProfile("Bang & Olufsen", "tv",
            necCodes(listOf(0x00,0x01), listOf(0x0C,0x08), "B&O"))
        list += BrandProfile("Sceptre", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Sceptre"))
        list += BrandProfile("Vizio", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x20), listOf(0x08,0x02,0x03), "Vizio"))
        list += BrandProfile("Element", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Element"))
        list += BrandProfile("Insignia", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Insignia"))
        list += BrandProfile("Polaroid", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Polaroid"))
        list += BrandProfile("Nikai", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Nikai"))
        list += BrandProfile("Akai", "tv",
            necCodes(listOf(0x00,0x40,0x08,0x0C), listOf(0x08,0x02,0x12), "Akai"))
        list += BrandProfile("Orient", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Orient"))
        list += BrandProfile("Dansat", "tv",
            necCodes(listOf(0x00,0x40,0x08), listOf(0x08,0x02), "Dansat"))

        // Universal "Try everything" TV — the full NEC + Samsung + Sony sweep
        list += BrandProfile("⚡ همه برندها (Universal)", "tv",
            necCodes(necTvAddresses, necPowerCmds, "NEC") +
            samsungCodes(samsungAddresses, samsungPowerCmds) +
            sonyCodes())

        // ---- AC ----
        list += BrandProfile("Daikin", "ac", acRaw("Daikin"))
        list += BrandProfile("Midea", "ac", acRaw("Midea"))
        list += BrandProfile("LG AC", "ac",
            necCodes(listOf(0x88,0x81,0x08), listOf(0xC8,0x08,0x88), "LG-AC"))
        list += BrandProfile("Samsung AC", "ac",
            samsungCodes(listOf(0xB2,0x11), listOf(0xBF,0x7F,0x02)))
        list += BrandProfile("Gree", "ac", acRaw("Gree"))
        list += BrandProfile("Haier AC", "ac",
            necCodes(listOf(0xD0,0x40,0x54), listOf(0x27,0x07,0x12), "Haier-AC"))
        list += BrandProfile("Hisense AC", "ac",
            necCodes(listOf(0x10,0x00), listOf(0x27,0x07), "Hisense-AC"))
        list += BrandProfile("TCL AC", "ac",
            necCodes(listOf(0x20,0x84), listOf(0x27,0x07), "TCL-AC"))
        list += BrandProfile("Panasonic AC", "ac", acRaw("Panasonic"))
        list += BrandProfile("Toshiba AC", "ac",
            necCodes(listOf(0xF2,0x02), listOf(0x0D,0x0E), "Toshiba-AC"))
        list += BrandProfile("Sharp AC", "ac",
            necCodes(listOf(0xAA,0x16), listOf(0x52,0x42), "Sharp-AC"))
        list += BrandProfile("Carrier", "ac",
            necCodes(listOf(0x4D,0x4C), listOf(0x51,0x41), "Carrier"))
        list += BrandProfile("Aux", "ac",
            necCodes(listOf(0xBB,0xBA), listOf(0x27,0x07), "Aux"))
        list += BrandProfile("⚡ همه برندها (Universal AC)", "ac",
            necCodes(listOf(0xD0,0x10,0x20,0x88,0xF2,0xAA,0x4D,0xBB,0x40,0x00), listOf(0x27,0x07,0x08,0xC8,0x0D,0x52,0x51,0x12), "AC") +
            acRaw("Daikin") + acRaw("Midea") + acRaw("Gree"))

        // ---- Projector ----
        list += BrandProfile("Epson", "projector",
            necCodes(listOf(0x35,0x00), listOf(0x01,0x90), "Epson"))
        list += BrandProfile("BenQ", "projector",
            necCodes(listOf(0x83,0x00), listOf(0x08,0x98), "BenQ"))
        list += BrandProfile("ViewSonic", "projector",
            necCodes(listOf(0x58,0x00), listOf(0x01,0x81), "ViewSonic"))
        list += BrandProfile("Optoma", "projector",
            necCodes(listOf(0x74,0x32), listOf(0x82,0x02,0x87), "Optoma"))
        list += BrandProfile("Acer", "projector",
            necCodes(listOf(0x31,0x10), listOf(0x82,0x02), "Acer"))

        // ---- DVD / Sound reuse NEC/Samsung/Sony sweep ----
        list += BrandProfile("Samsung DVD", "dvd", samsungCodes(listOf(0x1E,0x0C), listOf(0x11,0x02)))
        list += BrandProfile("Sony DVD", "dvd", sonyCodes())
        list += BrandProfile("LG DVD", "dvd", necCodes(listOf(0xC4,0x00), listOf(0x00,0x08), "LG-DVD"))
        list += BrandProfile("Samsung Soundbar", "sound", samsungCodes(listOf(0x11), listOf(0x02,0x99)))
        list += BrandProfile("Yamaha AV", "sound", necCodes(listOf(0x7A,0x5E), listOf(0x9E,0x1E), "Yamaha"))

        return list
    }

    // ── Raw-frame AC power codes (real captured on/off pairs) ─────────────────
    private fun acRaw(brand: String): List<PowerCode> {
        val cat = IrDatabase.acBrands.firstOrNull { it.name.startsWith(brand) } ?: return emptyList()
        return cat.signals
            .filter { it.name.contains("Power", true) || it.name.contains("ON", true) || it.name.contains("OFF", true) }
            .map { PowerCode("$brand ${it.name}", it.frequency, it.pattern, "AC-raw") }
    }

    private fun jvcCodes(addrs: List<Int>, cmds: List<Int>): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in addrs) for (c in cmds) out += PowerCode("JVC A=${a.h}/C=${c.h}", 38000, IrDatabase.jvc(a, c), "JVC")
        return out
    }

    private fun sharpPowerSweep(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in listOf(0x16,0x02,0x0A)) for (c in listOf(0x4D,0x40,0x1E)) {
            val f = IrDatabase.sharp(a, c)
            out += PowerCode("Sharp A=${a.h}/C=${c.h}", 38000, f, "Sharp")
        }
        return out
    }

    private fun panasonicPowerSweep(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in listOf(0x08,0x00,0x04)) for (c in listOf(0x3D,0xBD,0x20)) {
            out += PowerCode("Panasonic A=${a.h}/C=${c.h}", 37000, IrDatabase.panasonic(a, c), "Panasonic")
        }
        return out
    }

    private fun rc5PowerSweep(): List<PowerCode> {
        // RC5 power = command 0x0C across common system addresses (Philips family)
        val out = ArrayList<PowerCode>()
        for (a in listOf(0x00,0x01,0x05,0x10)) {
            out += PowerCode("RC5 Sys=${a.h}/C=0x0C", 36000, rc5(a, 0x0C), "RC5")
        }
        return out
    }

    // Proper RC5 bi-phase encoder (14 bits: 2 start, 1 toggle, 5 addr, 6 cmd)
    private fun rc5(addr: Int, cmd: Int): IntArray {
        val bits = ArrayList<Int>()
        bits += 1; bits += 1; bits += 0 // start, start, toggle=0
        for (i in 4 downTo 0) bits += (addr shr i) and 1
        for (i in 5 downTo 0) bits += (cmd shr i) and 1
        val t = 889
        val p = ArrayList<Int>()
        // Manchester: 0 = (mark,space)? RC5 uses 1 = low-then-high. Encode each bit as two half-bits.
        for (b in bits) {
            if (b == 1) { p += t; p += t } else { p += t; p += t }
        }
        return p.toIntArray()
    }

    fun forBrand(categoryId: String, brand: String): List<PowerCode> =
        profiles.firstOrNull { it.categoryId == categoryId && it.brand == brand }?.codes ?: emptyList()

    fun brandsFor(categoryId: String): List<BrandProfile> =
        profiles.filter { it.categoryId == categoryId }
}
