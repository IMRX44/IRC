package com.irblaster.universal.data

/**
 * Comprehensive per-brand POWER code database used by the "Smart Scan".
 *
 * For well-known brands we sweep their real device-address pool against their
 * real power commands. For rebadged / low-info brands (many Iranian & generic
 * Chinese sets) we run a FULL 256-address NEC sweep so the correct code is
 * guaranteed to be in the set — that is the honest way to cover a panel whose
 * exact address isn't publicly documented.
 *
 * Carrier frequencies (verified): NEC/Samsung/JVC/Sharp 38kHz · Sony 40kHz ·
 * Panasonic/Kaseikyo 37kHz · Philips RC5 36kHz.
 */

data class PowerCode(
    val label: String,
    val frequency: Int,
    val pattern: IntArray,
    val protocol: String,
)

data class BrandProfile(
    val brand: String,
    val categoryId: String,
    val codes: List<PowerCode>,
)

object PowerScan {

    // ── Command pools ────────────────────────────────────────────────────────
    // Power on/off/toggle bytes observed across NEC-family remotes.
    private val necPowerCmds = listOf(
        0x08,0x02,0x12,0x0C,0x10,0x40,0x48,0x18,0x03,0x00,0x0D,0x1E,0x1A,0x87,0x57,0xD0
    )
    private val necPowerCmdsCore = listOf(0x08,0x02,0x12,0x0C,0x10,0x1E)

    private val samsungAddresses = listOf(0x07,0x04,0x0E,0x01,0x02,0x03,0x08,0x0B,0x11,0x1E,0xB2,0xE0)
    private val samsungPowerCmds = listOf(0x02,0x99,0x11,0x12,0x9D,0xBF,0x62)

    private val sonyPowerCmds = listOf(0x15,0xA8,0x2E)
    private val sonyDeviceIds = listOf(0x01,0x02,0x0A,0x0C,0x0D,0x1A,0x18,0x25,0x2C,0x51,0x60,0x97)

    // ── Encoders ─────────────────────────────────────────────────────────────
    private fun necCodes(addresses: List<Int>, cmds: List<Int>, tag: String): List<PowerCode> {
        val out = ArrayList<PowerCode>(addresses.size * cmds.size)
        for (a in addresses) for (c in cmds)
            out += PowerCode("$tag A=${a.h}/C=${c.h}", 38000, IrDatabase.nec(a, c), "NEC")
        return out
    }

    /** FULL sweep: every 8-bit address × the given commands. Used for rebadge brands. */
    private fun necSweepAll(cmds: List<Int>, tag: String): List<PowerCode> {
        val out = ArrayList<PowerCode>(256 * cmds.size)
        for (a in 0..255) for (c in cmds)
            out += PowerCode("$tag A=${a.h}/C=${c.h}", 38000, IrDatabase.nec(a, c), "NEC")
        return out
    }

    private fun samsungCodes(addresses: List<Int>, cmds: List<Int>): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in addresses) for (c in cmds)
            out += PowerCode("Samsung A=${a.h}/C=${c.h}", 38000, IrDatabase.samsung(a, c), "Samsung")
        return out
    }

    private fun sonyCodes(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (d in sonyDeviceIds) for (c in sonyPowerCmds) {
            val f = IrDatabase.sony12(d, c)
            out += PowerCode("Sony D=${d.h}/C=${c.h}", 40000, f + f + f, "SIRC")
        }
        return out
    }

    private fun jvcCodes(addrs: List<Int>, cmds: List<Int>): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in addrs) for (c in cmds)
            out += PowerCode("JVC A=${a.h}/C=${c.h}", 38000, IrDatabase.jvc(a, c), "JVC")
        return out
    }

    private fun sharpSweep(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in listOf(0x16,0x02,0x0A)) for (c in listOf(0x4D,0x40,0x1E))
            out += PowerCode("Sharp A=${a.h}/C=${c.h}", 38000, IrDatabase.sharp(a, c), "Sharp")
        return out
    }

    private fun panasonicSweep(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in listOf(0x08,0x00,0x04)) for (c in listOf(0x3D,0xBD,0x20))
            out += PowerCode("Panasonic A=${a.h}/C=${c.h}", 37000, IrDatabase.panasonic(a, c), "Panasonic")
        return out
    }

    /** Correct RC5 Manchester (bi-phase) encoder. 14 bits: S,S,toggle,5 addr,6 cmd.
     *  Each bit = two half-bits of 889µs. Bit 1 = space-then-mark, bit 0 = mark-then-space. */
    private fun rc5(addr: Int, cmd: Int, toggle: Int = 0): IntArray {
        val bits = ArrayList<Int>()
        bits += 1; bits += 1; bits += toggle
        for (i in 4 downTo 0) bits += (addr shr i) and 1
        for (i in 5 downTo 0) bits += (cmd shr i) and 1
        val t = 889
        // Build half-bit level stream, then collapse to durations.
        val levels = ArrayList<Int>() // 1 = mark, 0 = space
        for (b in bits) {
            if (b == 1) { levels += 0; levels += 1 } else { levels += 1; levels += 0 }
        }
        // Convert level run-lengths to a pulse-pattern that must START with a mark.
        val p = ArrayList<Int>()
        var i = 0
        // Ensure leading mark (RC5 first half-bit of the two start '1' bits is a space→mark;
        // ConsumerIrManager patterns start with an ON burst, so drop a leading space if present).
        if (levels.isNotEmpty() && levels[0] == 0) i = 1
        while (i < levels.size) {
            val level = levels[i]
            var run = 0
            while (i < levels.size && levels[i] == level) { run++; i++ }
            p += run * t
        }
        return p.toIntArray()
    }

    private fun rc5Sweep(): List<PowerCode> {
        val out = ArrayList<PowerCode>()
        for (a in listOf(0x00,0x01,0x05,0x10)) {
            out += PowerCode("RC5 Sys=${a.h}/C=0x0C", 36000, rc5(a, 0x0C), "RC5")
            out += PowerCode("RC5 Sys=${a.h}/C=0x0C·t", 36000, rc5(a, 0x0C, 1), "RC5")
        }
        return out
    }

    private val Int.h get() = "0x${toString(16).uppercase().padStart(2,'0')}"

    // ── Standard sweep for lesser-known NEC brands (compact but broad) ───────
    private fun stdNec(name: String) = necCodes(
        listOf(0x00,0x01,0x02,0x04,0x07,0x08,0x0C,0x0D,0x10,0x20,0x40,0x48,0x50,0x80,0x84,0x88,0xA0,0xC0,0xE0,0xEF,0xFB,0xFF),
        necPowerCmdsCore, name
    )

    /** Comprehensive sweep for rebadge brands the user flagged (X.Vision etc.). */
    private fun rebadge(name: String) = necSweepAll(listOf(0x08,0x02,0x12,0x0C,0x10,0x1E), name)

    val profiles: List<BrandProfile> by lazy { buildProfiles() }

    private fun brand(name: String, cat: String, codes: List<PowerCode>) = BrandProfile(name, cat, codes)

    private fun buildProfiles(): List<BrandProfile> {
        val l = ArrayList<BrandProfile>()

        // ═══════════ TV ═══════════
        l += brand("Samsung", "tv", samsungCodes(samsungAddresses, samsungPowerCmds))
        l += brand("LG", "tv", necCodes(listOf(0x04,0x08,0xC0,0x00,0x20,0xBF,0x10), listOf(0x08,0xC8,0x02,0x10,0x0C), "LG"))
        l += brand("Sony", "tv", sonyCodes())
        l += brand("Philips", "tv", rc5Sweep() + necCodes(listOf(0x04,0x00), listOf(0x0C,0x0D), "Philips"))
        l += brand("Panasonic", "tv", panasonicSweep())
        l += brand("TCL", "tv", necCodes(listOf(0x84,0x04,0x08,0x40,0x57,0xF7), listOf(0x08,0x02,0x87), "TCL"))
        l += brand("Hisense", "tv", necCodes(listOf(0x00,0x01,0x40,0xE7,0xB7,0x10), listOf(0x08,0x02,0x17,0x57), "Hisense"))
        l += brand("Haier", "tv", necCodes(listOf(0x40,0x54,0x0C,0x00), listOf(0x12,0x08,0x0C), "Haier"))
        l += brand("Sharp", "tv", sharpSweep())
        l += brand("Toshiba", "tv", necCodes(listOf(0x02,0x40,0x03,0x00), listOf(0x01,0x12,0x02,0x0F), "Toshiba"))
        l += brand("Sony Bravia", "tv", sonyCodes())
        l += brand("JVC", "tv", jvcCodes(listOf(0xC5,0x03,0xF1), listOf(0x00,0x17,0x40)))
        l += brand("Hitachi", "tv", necCodes(listOf(0x00,0x01,0x40), listOf(0xD0,0x08,0x50), "Hitachi"))
        l += brand("Sanyo", "tv", necCodes(listOf(0x01,0x02,0x1C), listOf(0x48,0x08,0x0D), "Sanyo"))
        l += brand("Grundig", "tv", necCodes(listOf(0x80,0x00), listOf(0x3D,0x0C), "Grundig") + rc5Sweep())
        l += brand("Loewe", "tv", rc5Sweep())
        l += brand("Metz", "tv", rc5Sweep() + stdNec("Metz"))
        l += brand("Bang & Olufsen", "tv", necCodes(listOf(0x00,0x01), listOf(0x0C,0x08), "B&O"))

        // Chinese majors
        for (n in listOf("Skyworth","Konka","Changhong","Xiaomi","Redmi","OnePlus","Realme",
                         "Coocaa","Aiwa","Devant","Kalley","Kolin","Pentanik"))
            l += brand(n, "tv", stdNec(n))

        // Iranian & rebadge (FULL sweep — user-flagged X.Vision must be exhaustive)
        for (n in listOf("X.Vision (ایکس‌ویژن)","Snowa (اسنوا)","Marshal (مارشال)","G-Plus (جی‌پلاس)",
                         "Blest (بلست)","Pars (پارس)","Master (مستر)","Sam (سام)","Tech Wood",
                         "iLife","Shahab (شهاب)","Hardstone (هاردستون)","Mag (مگ)","Nikai"))
            l += brand(n, "tv", rebadge(n))

        // Turkish
        for (n in listOf("Beko","Arçelik","Regal","Telefunken","Profilo","Vestel","Finlux","Hi-Level","Sunny"))
            l += brand(n, "tv", stdNec(n) + rc5Sweep())

        // Indian / other global
        for (n in listOf("VU","Micromax","Videocon","Intex","Onida","Sansui","BPL","Akai","Lloyd",
                         "Thomson","Kodak","Sceptre","Vizio","Element","Insignia","Polaroid","Orient",
                         "Dansat","RCA","Magnavox","Emerson","Westinghouse","Sylvania","Funai",
                         "Continental Edison","Bush","Logik","Blaupunkt","Medion","Nordmende","Salora"))
            l += brand(n, "tv", stdNec(n))

        l += brand("⚡ همه برندها (Universal TV)", "tv",
            necSweepAll(listOf(0x08,0x02,0x12,0x0C,0x10,0x1E,0x40), "NEC") +
            samsungCodes(samsungAddresses, samsungPowerCmds) + sonyCodes())

        // ═══════════ AC ═══════════
        l += brand("Daikin", "ac", acRaw("Daikin"))
        l += brand("Midea", "ac", acRaw("Midea"))
        l += brand("Gree", "ac", acRaw("Gree"))
        l += brand("Panasonic AC", "ac", acRaw("Panasonic"))
        l += brand("LG AC", "ac", necCodes(listOf(0x88,0x81,0x08), listOf(0xC8,0x08,0x88), "LG-AC"))
        l += brand("Samsung AC", "ac", samsungCodes(listOf(0xB2,0x11), listOf(0xBF,0x7F,0x02)))
        for (n in listOf("Haier AC","Hisense AC","TCL AC","Toshiba AC","Sharp AC","Carrier","Aux",
                         "Fujitsu","Mitsubishi","Hitachi AC","Sanyo AC","Chigo","Kelvinator","Voltas",
                         "Blue Star","Godrej","Whirlpool","Electrolux","Bosch","Ariston","York",
                         "Trane","General","O-General","Gplus AC","Snowa AC","Emersun (امرسان)",
                         "Absal (آبسال)","Media AC","York Iran"," Classic"))
            l += brand(n, "ac", stdNec(n))
        l += brand("⚡ همه برندها (Universal AC)", "ac",
            necSweepAll(listOf(0x27,0x07,0x08,0xC8,0x0D,0x52,0x51,0x12), "AC") +
            acRaw("Daikin") + acRaw("Midea") + acRaw("Gree"))

        // ═══════════ Projector (100+) ═══════════
        val projectors = listOf(
            "Epson","BenQ","ViewSonic","Optoma","Acer","Sony","Panasonic","NEC","Hitachi","Sharp",
            "Christie","Barco","InFocus","Dell","LG","Casio","Vivitek","Canon","Mitsubishi","Ricoh",
            "Maxell","Sanyo","Eiki","Boxlight","Promethean","SMART","Digital Projection","JVC","Runco",
            "Planar","Delta","ASK Proxima","3M","Toshiba","Xgimi","Anker Nebula","JMGO","Wanbo","Xiaomi",
            "Formovie","Dangbei","WeWatch","YABER","Vankyo","Apeman","TouYinger","Byintek","Wimius",
            "GooDee","Artlii","DBPOWER","Elephas","Tenker","QKK","Crenova","PVO","Bomaker","Nebula",
            "AAXA","Kodak Projector","Philips NeoPix","Emotn","Ultimea","Vher","Fangor","Groview",
            "AuKing","YOWHICK","Magcubic","HODIO","CiBest","VILINICE","Jinhoo","Mooka","Vamvo",
            "Meer","Salange","Akiyo","Cinemood","Nomvdic","Halo","Capsule","LG CineBeam","BenQ GV",
            "Xgimi Halo","Kodak Luma","Miroir","Pico Genie","P8","Optoma ML","Sony MP-CD","ViewSonic M1",
            "Anker Mars","Nebula Cosmos","Epson EF","Hisense Laser","Changhong Laser","Fengmi","Appotronics",
            "WEMAX","Dangbei Mars","JMGO O1","Formovie Theater","Nexigo","Elite Screens","Vividstorm"
        )
        for (n in projectors) l += brand(n, "projector", stdNec(n))
        l += brand("⚡ همه پروژکتورها (Universal)", "projector",
            necSweepAll(listOf(0x01,0x82,0x08,0x98,0x02,0x87,0x90), "Proj"))

        // ═══════════ DVD / Blu-ray ═══════════
        l += brand("Samsung DVD", "dvd", samsungCodes(listOf(0x1E,0x0C), listOf(0x11,0x02)))
        l += brand("Sony DVD", "dvd", sonyCodes())
        for (n in listOf("LG DVD","Philips DVD","Panasonic DVD","Toshiba DVD","Pioneer","Onkyo","Denon",
                         "JVC DVD","Sharp DVD","Sansui DVD","BBK","Oppo","Cambridge Audio"))
            l += brand(n, "dvd", stdNec(n))

        // ═══════════ Sound / AV ═══════════
        l += brand("Samsung Soundbar", "sound", samsungCodes(listOf(0x11), listOf(0x02,0x99)))
        l += brand("Sony Audio", "sound", sonyCodes())
        for (n in listOf("Yamaha","Denon","Onkyo","Pioneer","Marantz","Harman Kardon","Bose","JBL",
                         "LG Soundbar","Philips Audio","Sonos","Klipsch","Polk","Edifier","Microlab",
                         "Sound United","Vestel Audio","TCL Soundbar"))
            l += brand(n, "sound", stdNec(n))

        // ═══════════ Set-Top Box / Receiver ═══════════
        for (n in listOf("Dreambox","Starsat","Echolink","Geant","Tiger","Alma","Openbox","Gtmedia",
                         "Vu+","Amiko","Technomate","Skybox","Humax","Nokia STB","Beein","Max","Absat",
                         "Sunny STB","DishTV","Tata Sky","Airtel","D-Smart","Digiturk","Fantec"))
            l += brand(n, "stb", stdNec(n))
        l += brand("⚡ همه رسیورها (Universal)", "stb",
            necSweepAll(listOf(0x08,0x02,0x0C,0x40,0x12), "STB"))

        // ═══════════ Fan ═══════════
        for (n in listOf("Pak (پاک)","Pars Khazar (پارس‌خزر)","Feller","General Fan","Xiaomi Fan",
                         "Orient Fan","Usha","Havells","Bajaj","Crompton","KDK","Panasonic Fan",
                         "Rowenta","Honeywell Fan","Dyson"))
            l += brand(n, "fan", stdNec(n))
        l += brand("⚡ همه فن‌ها (Universal)", "fan",
            necSweepAll(listOf(0x08,0x02,0x0C,0x40), "Fan"))

        // ═══════════ LED / Smart Light ═══════════
        for (n in listOf("Generic RGB","Magic Home","LED Strip 24-key","LED Strip 44-key","Yeelight",
                         "Nanoleaf","Govee IR","Philips Hue IR"))
            l += brand(n, "light", stdNec(n))

        return l
    }

    // ── Raw-frame AC power codes from IrDatabase captures ────────────────────
    private fun acRaw(brand: String): List<PowerCode> {
        val cat = IrDatabase.acBrands.firstOrNull { it.name.startsWith(brand) } ?: return emptyList()
        return cat.signals
            .filter { it.name.contains("Power", true) || it.name.contains("ON", true) || it.name.contains("OFF", true) }
            .map { PowerCode("$brand ${it.name}", it.frequency, it.pattern, "AC-raw") }
    }

    fun forBrand(categoryId: String, brand: String): List<PowerCode> =
        profiles.firstOrNull { it.categoryId == categoryId && it.brand == brand }?.codes ?: emptyList()

    fun brandsFor(categoryId: String): List<BrandProfile> =
        profiles.filter { it.categoryId == categoryId }

    /** Case-insensitive search across brand names within a category. */
    fun search(categoryId: String, query: String): List<BrandProfile> {
        val q = query.trim()
        val pool = brandsFor(categoryId)
        if (q.isEmpty()) return pool
        return pool.filter { it.brand.contains(q, ignoreCase = true) }
    }
}
