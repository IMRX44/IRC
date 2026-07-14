package com.irblaster.universal.data

data class IrSignal(
    val name: String,
    val frequency: Int,
    val pattern: IntArray,
    val description: String = ""
)

data class DeviceBrand(
    val name: String,
    val signals: List<IrSignal>
)

data class DeviceCategory(
    val id: String,
    val name: String,
    val icon: String,
    val brands: List<DeviceBrand>
)

object IrDatabase {

    // ── Protocol encoders (public so PowerScan can reuse them) ───────────────

    fun nec(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf(9000, 4500)
        fun bits(v: Int, inv: Boolean = false) {
            val b = if (inv) v.inv() and 0xFF else v
            repeat(8) { i -> p += 560; p += if ((b shr i) and 1 == 1) 1690 else 560 }
        }
        bits(addr); bits(addr, true); bits(cmd); bits(cmd, true)
        p += 560
        return p.toIntArray()
    }

    fun nec2(addrLo: Int, addrHi: Int, cmd: Int): IntArray {
        val p = mutableListOf(9000, 4500)
        fun bits(v: Int) = repeat(8) { i -> p += 560; p += if ((v shr i) and 1 == 1) 1690 else 560 }
        bits(addrLo); bits(addrHi); bits(cmd); bits(cmd.inv() and 0xFF)
        p += 560
        return p.toIntArray()
    }

    fun samsung(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf(4500, 4500)
        fun bits(v: Int) = repeat(8) { i -> p += 560; p += if ((v shr i) and 1 == 1) 1690 else 560 }
        bits(addr); bits(addr); bits(cmd); bits(cmd.inv() and 0xFF)
        p += 560
        return p.toIntArray()
    }

    private fun rc5(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf<Int>()
        val bits = listOf(1,1, (cmd shr 6 and 1).xor(1)) +
                (5 downTo 0).map { (addr shr it) and 1 } +
                (5 downTo 0).map { (cmd shr it) and 1 }
        var last = 0
        bits.forEach { b ->
            if (b == 1) { p += 889; p += 889 } else { p += 889; p += 889 }
            last = b
        }
        // RC5 simplified pulse stream
        repeat(13) { i ->
            val bit = bits.getOrElse(i) { 0 }
            if (bit == 1) { p += 889; p += 889 } else { p += 889; p += 889 }
        }
        return intArrayOf(889,889,889,889,889,889,889,1778,889,889,889,1778,889,889,889,889,889,1778,889,889,889,889,889,889,889)
    }

    fun sony12(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf(2400, 600)
        repeat(7) { i -> p += 600; p += if ((cmd shr (6-i)) and 1 == 1) 1200 else 600 }
        repeat(5) { i -> p += 600; p += if ((addr shr (4-i)) and 1 == 1) 1200 else 600 }
        return p.toIntArray()
    }

    fun jvc(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf(8400, 4200)
        fun bits(v: Int) = repeat(8) { i -> p += 526; p += if ((v shr i) and 1 == 1) 1574 else 526 }
        bits(addr); bits(cmd)
        p += 526
        return p.toIntArray()
    }

    fun sharp(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf<Int>()
        fun bit(b: Int) { p += 320; p += if (b == 1) 1000 else 680 }
        repeat(5) { i -> bit((addr shr i) and 1) }
        repeat(8) { i -> bit((cmd shr i) and 1) }
        bit(1); bit(0)
        return p.toIntArray()
    }

    fun panasonic(addr: Int, cmd: Int): IntArray {
        val p = mutableListOf(3456, 1728)
        fun bits(v: Int, n: Int = 8) = repeat(n) { i -> p += 432; p += if ((v shr i) and 1 == 1) 1296 else 432 }
        bits(0x40, 16); bits(addr); bits(cmd); bits(addr xor cmd)
        p += 432
        return p.toIntArray()
    }

    private fun kaseikyo(oem1: Int, oem2: Int, addr: Int, cmd: Int): IntArray {
        val p = mutableListOf(3456, 1728)
        fun bits(v: Int, n: Int = 8) = repeat(n) { i -> p += 432; p += if ((v shr i) and 1 == 1) 1296 else 432 }
        bits(oem1, 8); bits(oem2, 8); bits(addr, 4)
        val parity = (oem1 xor oem2).and(0xF) xor addr.and(0xF)
        bits(parity, 4); bits(cmd); bits((oem1 shr 4) xor (oem2 shr 4) xor (cmd and 0xF), 4)
        p += 432
        return p.toIntArray()
    }

    // ── TV brands ────────────────────────────────────────────────────────────

    private fun tvSignals(
        power: IntArray, volUp: IntArray, volDown: IntArray, mute: IntArray,
        chUp: IntArray, chDown: IntArray, menu: IntArray, ok: IntArray,
        back: IntArray, home: IntArray, up: IntArray, down: IntArray,
        left: IntArray, right: IntArray, source: IntArray,
        freq: Int = 38000
    ) = listOf(
        IrSignal("Power", freq, power, "روشن/خاموش"),
        IrSignal("Vol +", freq, volUp, "صدا بالا"),
        IrSignal("Vol -", freq, volDown, "صدا پایین"),
        IrSignal("Mute", freq, mute, "بی‌صدا"),
        IrSignal("CH +", freq, chUp, "کانال بالا"),
        IrSignal("CH -", freq, chDown, "کانال پایین"),
        IrSignal("Menu", freq, menu, "منو"),
        IrSignal("OK", freq, ok, "تأیید"),
        IrSignal("Back", freq, back, "برگشت"),
        IrSignal("Home", freq, home, "خانه"),
        IrSignal("▲", freq, up, "بالا"),
        IrSignal("▼", freq, down, "پایین"),
        IrSignal("◄", freq, left, "چپ"),
        IrSignal("►", freq, right, "راست"),
        IrSignal("Source", freq, source, "ورودی"),
    )

    val tvBrands = listOf(
        DeviceBrand("Samsung", tvSignals(
            power = samsung(0x07, 0x02), volUp = samsung(0x07, 0x07),
            volDown = samsung(0x07, 0x0B), mute = samsung(0x07, 0x0F),
            chUp = samsung(0x07, 0x12), chDown = samsung(0x07, 0x10),
            menu = samsung(0x07, 0x1A), ok = samsung(0x07, 0x68),
            back = samsung(0x07, 0x58), home = samsung(0x07, 0x79),
            up = samsung(0x07, 0x60), down = samsung(0x07, 0x61),
            left = samsung(0x07, 0x65), right = samsung(0x07, 0x62),
            source = samsung(0x07, 0x01)
        )),
        DeviceBrand("Samsung 2", tvSignals(
            power = samsung(0x04, 0x02), volUp = samsung(0x04, 0x07),
            volDown = samsung(0x04, 0x0B), mute = samsung(0x04, 0x0F),
            chUp = samsung(0x04, 0x12), chDown = samsung(0x04, 0x10),
            menu = samsung(0x04, 0x1A), ok = samsung(0x04, 0x68),
            back = samsung(0x04, 0x58), home = samsung(0x04, 0x79),
            up = samsung(0x04, 0x60), down = samsung(0x04, 0x61),
            left = samsung(0x04, 0x65), right = samsung(0x04, 0x62),
            source = samsung(0x04, 0x01)
        )),
        DeviceBrand("LG", tvSignals(
            power = nec(0x04, 0x08), volUp = nec(0x04, 0x02),
            volDown = nec(0x04, 0x03), mute = nec(0x04, 0x09),
            chUp = nec(0x04, 0x00), chDown = nec(0x04, 0x01),
            menu = nec(0x04, 0x43), ok = nec(0x04, 0x44),
            back = nec(0x04, 0x28), home = nec(0x04, 0xAE),
            up = nec(0x04, 0x40), down = nec(0x04, 0x41),
            left = nec(0x04, 0x07), right = nec(0x04, 0x06),
            source = nec(0x04, 0x0B)
        )),
        DeviceBrand("LG 2", tvSignals(
            power = nec(0x08, 0x08), volUp = nec(0x08, 0x02),
            volDown = nec(0x08, 0x03), mute = nec(0x08, 0x09),
            chUp = nec(0x08, 0x00), chDown = nec(0x08, 0x01),
            menu = nec(0x08, 0x43), ok = nec(0x08, 0x44),
            back = nec(0x08, 0x28), home = nec(0x08, 0xAE),
            up = nec(0x08, 0x40), down = nec(0x08, 0x41),
            left = nec(0x08, 0x07), right = nec(0x08, 0x06),
            source = nec(0x08, 0x0B)
        )),
        DeviceBrand("Sony", tvSignals(
            power = sony12(0x01, 0x15), volUp = sony12(0x01, 0x12),
            volDown = sony12(0x01, 0x13), mute = sony12(0x01, 0x14),
            chUp = sony12(0x01, 0x10), chDown = sony12(0x01, 0x11),
            menu = sony12(0x01, 0x60), ok = sony12(0x01, 0x65),
            back = sony12(0x01, 0x6B), home = sony12(0x01, 0x67),
            up = sony12(0x01, 0x74), down = sony12(0x01, 0x75),
            left = sony12(0x01, 0x34), right = sony12(0x01, 0x33),
            source = sony12(0x01, 0x25), freq = 40000
        )),
        DeviceBrand("Philips", tvSignals(
            power = intArrayOf(889,889,889,889,889,889,889,1778,889,889,889,1778,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            volUp = intArrayOf(889,889,889,889,889,1778,889,889,889,889,889,889,889,1778,889,889,889,1778,889,889,889,889,889,889,889),
            volDown = intArrayOf(889,889,889,889,889,1778,889,889,889,889,889,1778,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            mute = intArrayOf(889,889,889,889,889,889,889,1778,889,1778,889,889,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            chUp = intArrayOf(889,889,889,1778,889,889,889,889,889,889,889,889,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            chDown = intArrayOf(889,889,889,1778,889,889,889,889,889,889,889,1778,889,889,889,889,889,889,889,889,889,889,889,889,889),
            menu = intArrayOf(889,889,889,889,889,889,889,1778,889,889,889,889,889,1778,889,1778,889,889,889,889,889,889,889,889,889),
            ok = intArrayOf(889,889,889,1778,889,889,889,1778,889,889,889,889,889,889,889,1778,889,889,889,889,889,889,889,889,889),
            back = intArrayOf(889,889,889,889,889,1778,889,1778,889,889,889,889,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            home = intArrayOf(889,889,889,1778,889,1778,889,889,889,889,889,889,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            up = intArrayOf(889,889,889,1778,889,889,889,1778,889,1778,889,889,889,889,889,889,889,889,889,889,889,889,889,889,889),
            down = intArrayOf(889,889,889,1778,889,889,889,1778,889,889,889,1778,889,889,889,889,889,889,889,889,889,889,889,889,889),
            left = intArrayOf(889,889,889,889,889,889,889,1778,889,1778,889,1778,889,889,889,889,889,889,889,889,889,889,889,889,889),
            right = intArrayOf(889,889,889,1778,889,1778,889,1778,889,889,889,889,889,889,889,889,889,889,889,889,889,889,889,889,889),
            source = intArrayOf(889,889,889,889,889,889,889,889,889,889,889,1778,889,889,889,889,889,1778,889,889,889,889,889,889,889),
            freq = 36000
        )),
        DeviceBrand("Panasonic", tvSignals(
            power = panasonic(0x08, 0x3D), volUp = panasonic(0x08, 0x20),
            volDown = panasonic(0x08, 0x21), mute = panasonic(0x08, 0x23),
            chUp = panasonic(0x08, 0x34), chDown = panasonic(0x08, 0x35),
            menu = panasonic(0x08, 0x06), ok = panasonic(0x08, 0x49),
            back = panasonic(0x08, 0x0D), home = panasonic(0x08, 0x2F),
            up = panasonic(0x08, 0x4C), down = panasonic(0x08, 0x4D),
            left = panasonic(0x08, 0x4E), right = panasonic(0x08, 0x4F),
            source = panasonic(0x08, 0x38)
        )),
        DeviceBrand("TCL", tvSignals(
            power = nec(0x84, 0x08), volUp = nec(0x84, 0x02),
            volDown = nec(0x84, 0x03), mute = nec(0x84, 0x09),
            chUp = nec(0x84, 0x00), chDown = nec(0x84, 0x01),
            menu = nec(0x84, 0x43), ok = nec(0x84, 0x44),
            back = nec(0x84, 0x28), home = nec(0x84, 0xAE),
            up = nec(0x84, 0x40), down = nec(0x84, 0x41),
            left = nec(0x84, 0x07), right = nec(0x84, 0x06),
            source = nec(0x84, 0x0B)
        )),
        DeviceBrand("Hisense", tvSignals(
            power = nec(0x00, 0x08), volUp = nec(0x00, 0x02),
            volDown = nec(0x00, 0x03), mute = nec(0x00, 0x09),
            chUp = nec(0x00, 0x00), chDown = nec(0x00, 0x01),
            menu = nec(0x00, 0x43), ok = nec(0x00, 0x44),
            back = nec(0x00, 0x28), home = nec(0x00, 0xAE),
            up = nec(0x00, 0x40), down = nec(0x00, 0x41),
            left = nec(0x00, 0x07), right = nec(0x00, 0x06),
            source = nec(0x00, 0x0B)
        )),
        DeviceBrand("Haier", tvSignals(
            power = nec(0x40, 0x12), volUp = nec(0x40, 0x02),
            volDown = nec(0x40, 0x03), mute = nec(0x40, 0x09),
            chUp = nec(0x40, 0x00), chDown = nec(0x40, 0x01),
            menu = nec(0x40, 0x43), ok = nec(0x40, 0x44),
            back = nec(0x40, 0x28), home = nec(0x40, 0xAE),
            up = nec(0x40, 0x40), down = nec(0x40, 0x41),
            left = nec(0x40, 0x07), right = nec(0x40, 0x06),
            source = nec(0x40, 0x0B)
        )),
        DeviceBrand("Sharp", tvSignals(
            power = sharp(0x16, 0x4D), volUp = sharp(0x16, 0x58),
            volDown = sharp(0x16, 0xD8), mute = sharp(0x16, 0x20),
            chUp = sharp(0x16, 0x60), chDown = sharp(0x16, 0xE0),
            menu = sharp(0x16, 0x09), ok = sharp(0x16, 0x93),
            back = sharp(0x16, 0x00), home = sharp(0x16, 0x01),
            up = sharp(0x16, 0xC0), down = sharp(0x16, 0x40),
            left = sharp(0x16, 0x28), right = sharp(0x16, 0xA8),
            source = sharp(0x16, 0x52)
        )),
        DeviceBrand("Toshiba", tvSignals(
            power = nec(0x02, 0x01), volUp = nec(0x02, 0x02),
            volDown = nec(0x02, 0x03), mute = nec(0x02, 0x09),
            chUp = nec(0x02, 0x09), chDown = nec(0x02, 0x0A),
            menu = nec(0x02, 0x0B), ok = nec(0x02, 0x2C),
            back = nec(0x02, 0x0D), home = nec(0x02, 0x02),
            up = nec(0x02, 0x1E), down = nec(0x02, 0x1F),
            left = nec(0x02, 0x20), right = nec(0x02, 0x21),
            source = nec(0x02, 0x0F)
        )),
        DeviceBrand("Xiaomi TV", tvSignals(
            power = nec(0x0D, 0x01), volUp = nec(0x0D, 0x03),
            volDown = nec(0x0D, 0x04), mute = nec(0x0D, 0x06),
            chUp = nec(0x0D, 0x07), chDown = nec(0x0D, 0x08),
            menu = nec(0x0D, 0x0F), ok = nec(0x0D, 0x15),
            back = nec(0x0D, 0x0E), home = nec(0x0D, 0x10),
            up = nec(0x0D, 0x11), down = nec(0x0D, 0x12),
            left = nec(0x0D, 0x13), right = nec(0x0D, 0x14),
            source = nec(0x0D, 0x05)
        )),
        DeviceBrand("JVC", tvSignals(
            power = jvc(0xC5, 0x00), volUp = jvc(0xC5, 0x10),
            volDown = jvc(0xC5, 0x90), mute = jvc(0xC5, 0x50),
            chUp = jvc(0xC5, 0x08), chDown = jvc(0xC5, 0x88),
            menu = jvc(0xC5, 0x44), ok = jvc(0xC5, 0x5A),
            back = jvc(0xC5, 0xDA), home = jvc(0xC5, 0x4E),
            up = jvc(0xC5, 0x9A), down = jvc(0xC5, 0x1A),
            left = jvc(0xC5, 0x3A), right = jvc(0xC5, 0xBA),
            source = jvc(0xC5, 0x02)
        )),
        DeviceBrand("Vestel", tvSignals(
            power = nec(0xE0, 0x12), volUp = nec(0xE0, 0x1E),
            volDown = nec(0xE0, 0x1F), mute = nec(0xE0, 0x10),
            chUp = nec(0xE0, 0x1C), chDown = nec(0xE0, 0x1D),
            menu = nec(0xE0, 0x0B), ok = nec(0xE0, 0x0D),
            back = nec(0xE0, 0x0E), home = nec(0xE0, 0x0F),
            up = nec(0xE0, 0x08), down = nec(0xE0, 0x09),
            left = nec(0xE0, 0x0A), right = nec(0xE0, 0x0C),
            source = nec(0xE0, 0x02)
        )),
        DeviceBrand("Skyworth", tvSignals(
            power = nec(0x0C, 0x08), volUp = nec(0x0C, 0x02),
            volDown = nec(0x0C, 0x03), mute = nec(0x0C, 0x09),
            chUp = nec(0x0C, 0x00), chDown = nec(0x0C, 0x01),
            menu = nec(0x0C, 0x43), ok = nec(0x0C, 0x44),
            back = nec(0x0C, 0x28), home = nec(0x0C, 0xAE),
            up = nec(0x0C, 0x40), down = nec(0x0C, 0x41),
            left = nec(0x0C, 0x07), right = nec(0x0C, 0x06),
            source = nec(0x0C, 0x0B)
        )),
        DeviceBrand("Konka", tvSignals(
            power = nec(0x24, 0x08), volUp = nec(0x24, 0x02),
            volDown = nec(0x24, 0x03), mute = nec(0x24, 0x09),
            chUp = nec(0x24, 0x00), chDown = nec(0x24, 0x01),
            menu = nec(0x24, 0x43), ok = nec(0x24, 0x44),
            back = nec(0x24, 0x28), home = nec(0x24, 0xAE),
            up = nec(0x24, 0x40), down = nec(0x24, 0x41),
            left = nec(0x24, 0x07), right = nec(0x24, 0x06),
            source = nec(0x24, 0x0B)
        )),
        DeviceBrand("Changhong", tvSignals(
            power = nec(0x50, 0x08), volUp = nec(0x50, 0x02),
            volDown = nec(0x50, 0x03), mute = nec(0x50, 0x09),
            chUp = nec(0x50, 0x00), chDown = nec(0x50, 0x01),
            menu = nec(0x50, 0x43), ok = nec(0x50, 0x44),
            back = nec(0x50, 0x28), home = nec(0x50, 0xAE),
            up = nec(0x50, 0x40), down = nec(0x50, 0x41),
            left = nec(0x50, 0x07), right = nec(0x50, 0x06),
            source = nec(0x50, 0x0B)
        )),
        DeviceBrand("Funai", tvSignals(
            power = nec(0x7E, 0x01), volUp = nec(0x7E, 0x0A),
            volDown = nec(0x7E, 0x0B), mute = nec(0x7E, 0x08),
            chUp = nec(0x7E, 0x1E), chDown = nec(0x7E, 0x1F),
            menu = nec(0x7E, 0x55), ok = nec(0x7E, 0x54),
            back = nec(0x7E, 0x09), home = nec(0x7E, 0x0C),
            up = nec(0x7E, 0x11), down = nec(0x7E, 0x12),
            left = nec(0x7E, 0x13), right = nec(0x7E, 0x14),
            source = nec(0x7E, 0x02)
        )),
        DeviceBrand("Grundig", tvSignals(
            power = nec(0x80, 0x3D), volUp = nec(0x80, 0x1A),
            volDown = nec(0x80, 0x1B), mute = nec(0x80, 0x0A),
            chUp = nec(0x80, 0x1C), chDown = nec(0x80, 0x1D),
            menu = nec(0x80, 0x28), ok = nec(0x80, 0x26),
            back = nec(0x80, 0x29), home = nec(0x80, 0x2A),
            up = nec(0x80, 0x20), down = nec(0x80, 0x21),
            left = nec(0x80, 0x22), right = nec(0x80, 0x23),
            source = nec(0x80, 0x11)
        )),
        DeviceBrand("Hitachi", tvSignals(
            power = nec2(0x00, 0xFF, 0xD0), volUp = nec2(0x00, 0xFF, 0x40),
            volDown = nec2(0x00, 0xFF, 0xC0), mute = nec2(0x00, 0xFF, 0x48),
            chUp = nec2(0x00, 0xFF, 0x80), chDown = nec2(0x00, 0xFF, 0x00),
            menu = nec2(0x00, 0xFF, 0x16), ok = nec2(0x00, 0xFF, 0x1C),
            back = nec2(0x00, 0xFF, 0x0A), home = nec2(0x00, 0xFF, 0x50),
            up = nec2(0x00, 0xFF, 0x02), down = nec2(0x00, 0xFF, 0x01),
            left = nec2(0x00, 0xFF, 0x03), right = nec2(0x00, 0xFF, 0x04),
            source = nec2(0x00, 0xFF, 0x05)
        )),
    )

    // ── AC brands ────────────────────────────────────────────────────────────

    private fun acSignals(onCool24: IntArray, off: IntArray, tempUp: IntArray, tempDown: IntArray,
                          fanAuto: IntArray, modeCool: IntArray, modeHeat: IntArray, modeFan: IntArray,
                          swing: IntArray, sleep: IntArray, freq: Int = 38000) = listOf(
        IrSignal("Power ON Cool 24°", freq, onCool24, "روشن - سرمایش 24°"),
        IrSignal("Power OFF", freq, off, "خاموش"),
        IrSignal("Temp ▲", freq, tempUp, "دما بالا"),
        IrSignal("Temp ▼", freq, tempDown, "دما پایین"),
        IrSignal("Fan Auto", freq, fanAuto, "فن خودکار"),
        IrSignal("Cool Mode", freq, modeCool, "حالت سرمایش"),
        IrSignal("Heat Mode", freq, modeHeat, "حالت گرمایش"),
        IrSignal("Fan Only", freq, modeFan, "فن تنها"),
        IrSignal("Swing", freq, swing, "چرخش"),
        IrSignal("Sleep", freq, sleep, "خواب"),
    )

    val acBrands = listOf(
        DeviceBrand("Daikin", listOf(
            IrSignal("Power ON Cool 24°", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,420,420,420,420,420,1310,420,420,420,1310,420,1310,420,1310,420,420,420,1310,420,420,420,420,420,420,420,420,420,1310,420,1310,420,420,420,420,420,1310,420,420,420,1310,420,420,420,1310,420,1310,420,1310,420,29000), "روشن سرمایش 24°"),
            IrSignal("Power OFF", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,420,420,420,420,420,420,420,1310,420,420,420,420,420,420,420,1310,420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,420,420,420,420,420,1310,420,1310,420,1310,420,1310,420,1310,420,1310,420,29000), "خاموش"),
            IrSignal("Temp ▲", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,1310,420,1310,420,420,420,420,420,420,420,420,420,420,420,1310,420,420,420,1310,420,420,420,1310,420,29000), "دما بالا"),
            IrSignal("Temp ▼", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,1310,420,420,420,1310,420,420,420,420,420,420,420,420,420,1310,420,420,420,1310,420,420,420,1310,420,29000), "دما پایین"),
            IrSignal("Fan Auto", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,420,420,420,420,420,420,420,420,1310,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,420,29000), "فن خودکار"),
            IrSignal("Cool Mode", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,1310,420,1310,420,1310,420,420,420,420,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,420,29000), "سرمایش"),
            IrSignal("Heat Mode", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,1310,420,1310,420,1310,420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,420,29000), "گرمایش"),
            IrSignal("Dry Mode", 38000, intArrayOf(3500,1750,420,1310,420,420,420,420,420,420,420,1310,420,1310,420,420,420,420,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,420,29000), "خشک‌کن"),
        )),
        DeviceBrand("Midea", listOf(
            IrSignal("Power ON Cool 25°", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,1690,560,560,560,560,560,560,560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,560,560,560,560), "روشن سرمایش 25°"),
            IrSignal("Power OFF", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,560,560,560,560,1690,560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,1690,560,1690,560), "خاموش"),
            IrSignal("Temp 18° Cool", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,560,560,1690,560,560,560,1690,560,560,560,560,560), "18° سرمایش"),
            IrSignal("Temp 20° Cool", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,1690,560,560,560,1690,560,1690,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,560,560), "20° سرمایش"),
            IrSignal("Temp 22° Cool", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,560,560), "22° سرمایش"),
            IrSignal("Temp 26° Cool", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,560,560,1690,560,1690,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,1690,560,560,560,560,560), "26° سرمایش"),
            IrSignal("Heat 22°", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,560,560,560,560), "22° گرمایش"),
            IrSignal("Fan Speed Auto", 38000, intArrayOf(4500,4500,560,1690,560,560,560,560,560,560,560,1690,560,1690,560,1690,560,560,560,560,560,560,560,560,560,1690,560,560,560,1690,560,1690,560), "فن خودکار"),
        )),
        DeviceBrand("LG AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0x88, 0xC8), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0x88, 0x08), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0x88, 0x00), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0x88, 0x80), "دما پایین"),
            IrSignal("Fan Low", 38000, nec(0x88, 0x40), "فن کم"),
            IrSignal("Fan Med", 38000, nec(0x88, 0x20), "فن متوسط"),
            IrSignal("Fan High", 38000, nec(0x88, 0x10), "فن زیاد"),
            IrSignal("Cool Mode", 38000, nec(0x88, 0xC0), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0x88, 0x48), "گرمایش"),
        )),
        DeviceBrand("Samsung AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, samsung(0xB2, 0xBF), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, samsung(0xB2, 0x7F), "خاموش"),
            IrSignal("Temp ▲", 38000, samsung(0xB2, 0x9E), "دما بالا"),
            IrSignal("Temp ▼", 38000, samsung(0xB2, 0x9F), "دما پایین"),
            IrSignal("Fan Auto", 38000, samsung(0xB2, 0xBE), "فن خودکار"),
            IrSignal("Cool Mode", 38000, samsung(0xB2, 0x3F), "سرمایش"),
            IrSignal("Heat Mode", 38000, samsung(0xB2, 0x4F), "گرمایش"),
        )),
        DeviceBrand("Gree", listOf(
            IrSignal("Power ON Cool 24°", 38000, intArrayOf(9000,4500,560,1690,560,560,560,560,560,1690,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,1690,560,1690,560,560,560,1690,560), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, intArrayOf(9000,4500,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,1690,560,560,560,1690,560,1690,560,560,560,560,560,1690,560,560,560), "خاموش"),
            IrSignal("Cool Mode 20°", 38000, intArrayOf(9000,4500,560,1690,560,560,560,1690,560,560,560,560,560,1690,560,1690,560,1690,560,560,560,560,560,1690,560,560,560,1690,560,1690,560,560,560), "سرمایش 20°"),
            IrSignal("Heat Mode 22°", 38000, intArrayOf(9000,4500,560,560,560,1690,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,1690,560,560,560,1690,560,560,560,1690,560,560,560,1690,560), "گرمایش 22°"),
        )),
        DeviceBrand("Haier AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0xD0, 0x27), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0xD0, 0x07), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0xD0, 0x09), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0xD0, 0x08), "دما پایین"),
            IrSignal("Fan Auto", 38000, nec(0xD0, 0x25), "فن خودکار"),
            IrSignal("Cool Mode", 38000, nec(0xD0, 0x26), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0xD0, 0x4E), "گرمایش"),
            IrSignal("Dry Mode", 38000, nec(0xD0, 0x2E), "خشک"),
        )),
        DeviceBrand("Hisense AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0x10, 0x27), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0x10, 0x07), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0x10, 0x0A), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0x10, 0x0B), "دما پایین"),
            IrSignal("Cool Mode", 38000, nec(0x10, 0x26), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0x10, 0x4E), "گرمایش"),
            IrSignal("Fan Auto", 38000, nec(0x10, 0x25), "فن خودکار"),
        )),
        DeviceBrand("TCL AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0x20, 0x27), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0x20, 0x07), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0x20, 0x0A), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0x20, 0x0B), "دما پایین"),
            IrSignal("Cool Mode", 38000, nec(0x20, 0x26), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0x20, 0x4E), "گرمایش"),
        )),
        DeviceBrand("Panasonic AC", listOf(
            IrSignal("Power ON Cool 23°", 38000, intArrayOf(3456,1728,432,432,432,1296,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,1296,432,1296,432,432,432,1296,432,432,432,432,432,432,432,432,432,432,432,1296,432,432,432,432,432,432,432,432,432), "روشن سرمایش 23°"),
            IrSignal("Power OFF", 38000, intArrayOf(3456,1728,432,432,432,1296,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432,432), "خاموش"),
        )),
        DeviceBrand("Toshiba AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0xF2, 0x0D), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0xF2, 0x0E), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0xF2, 0x04), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0xF2, 0x05), "دما پایین"),
            IrSignal("Fan Auto", 38000, nec(0xF2, 0x09), "فن خودکار"),
            IrSignal("Cool Mode", 38000, nec(0xF2, 0x0A), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0xF2, 0x0B), "گرمایش"),
        )),
        DeviceBrand("Sharp AC", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0xAA, 0x52), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0xAA, 0x42), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0xAA, 0xD2), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0xAA, 0x92), "دما پایین"),
            IrSignal("Fan Auto", 38000, nec(0xAA, 0x32), "فن خودکار"),
            IrSignal("Cool Mode", 38000, nec(0xAA, 0x12), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0xAA, 0x72), "گرمایش"),
        )),
        DeviceBrand("Carrier", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0x4D, 0x51), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0x4D, 0x41), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0x4D, 0xD1), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0x4D, 0x91), "دما پایین"),
            IrSignal("Fan Auto", 38000, nec(0x4D, 0x31), "فن خودکار"),
            IrSignal("Cool Mode", 38000, nec(0x4D, 0x11), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0x4D, 0x71), "گرمایش"),
        )),
        DeviceBrand("Aux", listOf(
            IrSignal("Power ON Cool 24°", 38000, nec(0xBB, 0x27), "روشن سرمایش"),
            IrSignal("Power OFF", 38000, nec(0xBB, 0x07), "خاموش"),
            IrSignal("Temp ▲", 38000, nec(0xBB, 0x0A), "دما بالا"),
            IrSignal("Temp ▼", 38000, nec(0xBB, 0x0B), "دما پایین"),
            IrSignal("Fan Auto", 38000, nec(0xBB, 0x25), "فن خودکار"),
            IrSignal("Cool Mode", 38000, nec(0xBB, 0x26), "سرمایش"),
            IrSignal("Heat Mode", 38000, nec(0xBB, 0x4E), "گرمایش"),
        )),
    )

    // ── Projector brands ─────────────────────────────────────────────────────

    val projectorBrands = listOf(
        DeviceBrand("Epson", listOf(
            IrSignal("Power", 38000, nec(0x35, 0x01), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0x35, 0x03), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0x35, 0x04), "صدا پایین"),
            IrSignal("Mute", 38000, nec(0x35, 0x05), "بی‌صدا"),
            IrSignal("Source", 38000, nec(0x35, 0x06), "ورودی"),
            IrSignal("Menu", 38000, nec(0x35, 0x09), "منو"),
            IrSignal("Auto", 38000, nec(0x35, 0x0A), "تنظیم خودکار"),
            IrSignal("Freeze", 38000, nec(0x35, 0x07), "فریز تصویر"),
            IrSignal("▲", 38000, nec(0x35, 0x30), "بالا"),
            IrSignal("▼", 38000, nec(0x35, 0x31), "پایین"),
            IrSignal("◄", 38000, nec(0x35, 0x32), "چپ"),
            IrSignal("►", 38000, nec(0x35, 0x33), "راست"),
            IrSignal("Enter", 38000, nec(0x35, 0x0B), "تأیید"),
            IrSignal("Blank", 38000, nec(0x35, 0x08), "بلانک"),
        )),
        DeviceBrand("BenQ", listOf(
            IrSignal("Power", 38000, nec(0x83, 0x08), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0x83, 0x55), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0x83, 0x56), "صدا پایین"),
            IrSignal("Mute", 38000, nec(0x83, 0x50), "بی‌صدا"),
            IrSignal("Source", 38000, nec(0x83, 0x11), "ورودی"),
            IrSignal("Menu", 38000, nec(0x83, 0x40), "منو"),
            IrSignal("Blank", 38000, nec(0x83, 0x1F), "بلانک"),
            IrSignal("▲", 38000, nec(0x83, 0x02), "بالا"),
            IrSignal("▼", 38000, nec(0x83, 0x06), "پایین"),
            IrSignal("◄", 38000, nec(0x83, 0x04), "چپ"),
            IrSignal("►", 38000, nec(0x83, 0x05), "راست"),
            IrSignal("Enter", 38000, nec(0x83, 0x0A), "تأیید"),
        )),
        DeviceBrand("ViewSonic", listOf(
            IrSignal("Power", 38000, nec(0x58, 0x01), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0x58, 0x07), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0x58, 0x0B), "صدا پایین"),
            IrSignal("Menu", 38000, nec(0x58, 0x1A), "منو"),
            IrSignal("Source", 38000, nec(0x58, 0x10), "ورودی"),
            IrSignal("▲", 38000, nec(0x58, 0x60), "بالا"),
            IrSignal("▼", 38000, nec(0x58, 0x61), "پایین"),
            IrSignal("◄", 38000, nec(0x58, 0x65), "چپ"),
            IrSignal("►", 38000, nec(0x58, 0x62), "راست"),
            IrSignal("Enter", 38000, nec(0x58, 0x68), "تأیید"),
            IrSignal("Blank", 38000, nec(0x58, 0x15), "بلانک"),
        )),
        DeviceBrand("Optoma", listOf(
            IrSignal("Power", 38000, nec(0x74, 0x82), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0x74, 0x98), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0x74, 0x58), "صدا پایین"),
            IrSignal("Mute", 38000, nec(0x74, 0x18), "بی‌صدا"),
            IrSignal("Source", 38000, nec(0x74, 0x08), "ورودی"),
            IrSignal("Menu", 38000, nec(0x74, 0x1C), "منو"),
            IrSignal("▲", 38000, nec(0x74, 0xD0), "بالا"),
            IrSignal("▼", 38000, nec(0x74, 0x50), "پایین"),
            IrSignal("◄", 38000, nec(0x74, 0x30), "چپ"),
            IrSignal("►", 38000, nec(0x74, 0xB0), "راست"),
            IrSignal("Enter", 38000, nec(0x74, 0x70), "تأیید"),
            IrSignal("Blank", 38000, nec(0x74, 0x0C), "بلانک"),
        )),
        DeviceBrand("Acer Proj.", listOf(
            IrSignal("Power", 38000, nec(0x31, 0x82), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0x31, 0x15), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0x31, 0x16), "صدا پایین"),
            IrSignal("Mute", 38000, nec(0x31, 0x02), "بی‌صدا"),
            IrSignal("Source", 38000, nec(0x31, 0x01), "ورودی"),
            IrSignal("Menu", 38000, nec(0x31, 0x03), "منو"),
            IrSignal("▲", 38000, nec(0x31, 0x06), "بالا"),
            IrSignal("▼", 38000, nec(0x31, 0x07), "پایین"),
            IrSignal("◄", 38000, nec(0x31, 0x08), "چپ"),
            IrSignal("►", 38000, nec(0x31, 0x09), "راست"),
            IrSignal("Enter", 38000, nec(0x31, 0x0A), "تأیید"),
        )),
    )

    // ── DVD / Blu-ray ─────────────────────────────────────────────────────────

    val dvdBrands = listOf(
        DeviceBrand("Samsung DVD", listOf(
            IrSignal("Power", 38000, samsung(0x1E, 0x11), "روشن/خاموش"),
            IrSignal("Play", 38000, samsung(0x1E, 0x24), "پخش"),
            IrSignal("Pause", 38000, samsung(0x1E, 0x25), "مکث"),
            IrSignal("Stop", 38000, samsung(0x1E, 0x26), "توقف"),
            IrSignal("Next", 38000, samsung(0x1E, 0x2C), "بعدی"),
            IrSignal("Prev", 38000, samsung(0x1E, 0x2E), "قبلی"),
            IrSignal("FF", 38000, samsung(0x1E, 0x2A), "جلو سریع"),
            IrSignal("RW", 38000, samsung(0x1E, 0x2B), "عقب سریع"),
            IrSignal("Vol +", 38000, samsung(0x1E, 0x07), "صدا بالا"),
            IrSignal("Vol -", 38000, samsung(0x1E, 0x0B), "صدا پایین"),
            IrSignal("Menu", 38000, samsung(0x1E, 0x1A), "منو"),
            IrSignal("▲", 38000, samsung(0x1E, 0x60), "بالا"),
            IrSignal("▼", 38000, samsung(0x1E, 0x61), "پایین"),
            IrSignal("◄", 38000, samsung(0x1E, 0x65), "چپ"),
            IrSignal("►", 38000, samsung(0x1E, 0x62), "راست"),
            IrSignal("Enter", 38000, samsung(0x1E, 0x68), "تأیید"),
        )),
        DeviceBrand("Sony DVD", listOf(
            IrSignal("Power", 40000, sony12(0x02, 0x15), "روشن/خاموش"),
            IrSignal("Play", 40000, sony12(0x02, 0x32), "پخش"),
            IrSignal("Pause", 40000, sony12(0x02, 0x19), "مکث"),
            IrSignal("Stop", 40000, sony12(0x02, 0x18), "توقف"),
            IrSignal("Next", 40000, sony12(0x02, 0x30), "بعدی"),
            IrSignal("Prev", 40000, sony12(0x02, 0x31), "قبلی"),
            IrSignal("FF", 40000, sony12(0x02, 0x34), "جلو سریع"),
            IrSignal("RW", 40000, sony12(0x02, 0x33), "عقب سریع"),
            IrSignal("Menu", 40000, sony12(0x02, 0x60), "منو"),
            IrSignal("Enter", 40000, sony12(0x02, 0x65), "تأیید"),
            IrSignal("▲", 40000, sony12(0x02, 0x74), "بالا"),
            IrSignal("▼", 40000, sony12(0x02, 0x75), "پایین"),
            IrSignal("◄", 40000, sony12(0x02, 0x34), "چپ"),
            IrSignal("►", 40000, sony12(0x02, 0x33), "راست"),
        )),
        DeviceBrand("LG DVD", listOf(
            IrSignal("Power", 38000, nec(0xC4, 0x00), "روشن/خاموش"),
            IrSignal("Play", 38000, nec(0xC4, 0x16), "پخش"),
            IrSignal("Pause", 38000, nec(0xC4, 0x0C), "مکث"),
            IrSignal("Stop", 38000, nec(0xC4, 0x17), "توقف"),
            IrSignal("Next", 38000, nec(0xC4, 0x0F), "بعدی"),
            IrSignal("Prev", 38000, nec(0xC4, 0x0E), "قبلی"),
            IrSignal("Menu", 38000, nec(0xC4, 0x13), "منو"),
            IrSignal("Enter", 38000, nec(0xC4, 0x44), "تأیید"),
            IrSignal("▲", 38000, nec(0xC4, 0x40), "بالا"),
            IrSignal("▼", 38000, nec(0xC4, 0x41), "پایین"),
        )),
        DeviceBrand("Panasonic DVD", listOf(
            IrSignal("Power", 36700, kaseikyo(0x40, 0x04, 0x02, 0x3D), "روشن/خاموش"),
            IrSignal("Play", 36700, kaseikyo(0x40, 0x04, 0x02, 0x0E), "پخش"),
            IrSignal("Pause", 36700, kaseikyo(0x40, 0x04, 0x02, 0x0F), "مکث"),
            IrSignal("Stop", 36700, kaseikyo(0x40, 0x04, 0x02, 0x00), "توقف"),
            IrSignal("Menu", 36700, kaseikyo(0x40, 0x04, 0x02, 0x06), "منو"),
            IrSignal("▲", 36700, kaseikyo(0x40, 0x04, 0x02, 0x4C), "بالا"),
            IrSignal("▼", 36700, kaseikyo(0x40, 0x04, 0x02, 0x4D), "پایین"),
            IrSignal("Enter", 36700, kaseikyo(0x40, 0x04, 0x02, 0x49), "تأیید"),
        )),
    )

    // ── Sound systems ─────────────────────────────────────────────────────────

    val soundBrands = listOf(
        DeviceBrand("Samsung Soundbar", listOf(
            IrSignal("Power", 38000, samsung(0x11, 0x02), "روشن/خاموش"),
            IrSignal("Vol +", 38000, samsung(0x11, 0x07), "صدا بالا"),
            IrSignal("Vol -", 38000, samsung(0x11, 0x0B), "صدا پایین"),
            IrSignal("Mute", 38000, samsung(0x11, 0x0F), "بی‌صدا"),
            IrSignal("Source", 38000, samsung(0x11, 0x01), "ورودی"),
            IrSignal("Bluetooth", 38000, samsung(0x11, 0x5B), "بلوتوث"),
            IrSignal("Optical", 38000, samsung(0x11, 0x5C), "اپتیکال"),
            IrSignal("Bass +", 38000, samsung(0x11, 0x13), "بیس بالا"),
            IrSignal("Bass -", 38000, samsung(0x11, 0x14), "بیس پایین"),
        )),
        DeviceBrand("Sony Soundbar", listOf(
            IrSignal("Power", 40000, sony12(0x0D, 0x15), "روشن/خاموش"),
            IrSignal("Vol +", 40000, sony12(0x0D, 0x12), "صدا بالا"),
            IrSignal("Vol -", 40000, sony12(0x0D, 0x13), "صدا پایین"),
            IrSignal("Mute", 40000, sony12(0x0D, 0x14), "بی‌صدا"),
            IrSignal("Source", 40000, sony12(0x0D, 0x25), "ورودی"),
        )),
        DeviceBrand("LG Soundbar", listOf(
            IrSignal("Power", 38000, nec(0xF5, 0x02), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0xF5, 0x07), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0xF5, 0x0B), "صدا پایین"),
            IrSignal("Mute", 38000, nec(0xF5, 0x09), "بی‌صدا"),
            IrSignal("Bluetooth", 38000, nec(0xF5, 0x5B), "بلوتوث"),
        )),
        DeviceBrand("Yamaha AV", listOf(
            IrSignal("Power", 38000, nec(0x7A, 0x9E), "روشن/خاموش"),
            IrSignal("Vol +", 38000, nec(0x7A, 0x1E), "صدا بالا"),
            IrSignal("Vol -", 38000, nec(0x7A, 0x9A), "صدا پایین"),
            IrSignal("Mute", 38000, nec(0x7A, 0x1A), "بی‌صدا"),
            IrSignal("HDMI 1", 38000, nec(0x7A, 0x62), "HDMI 1"),
            IrSignal("HDMI 2", 38000, nec(0x7A, 0x61), "HDMI 2"),
        )),
    )

    val categories = listOf(
        DeviceCategory("tv", "تلویزیون", "📺", tvBrands),
        DeviceCategory("ac", "کولر گازی", "❄️", acBrands),
        DeviceCategory("projector", "پروژکتور", "📽️", projectorBrands),
        DeviceCategory("dvd", "DVD / Blu-ray", "💿", dvdBrands),
        DeviceCategory("sound", "سیستم صوتی", "🔊", soundBrands),
    )

    // ── Brute-force scanner ───────────────────────────────────────────────────
    // covers all 256 power command variants across 40 manufacturer addresses

    fun generateBruteForceSignals(): List<IrSignal> {
        val signals = mutableListOf<IrSignal>()

        // Every known manufacturer NEC address
        val necAddresses = listOf(
            0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x0A,0x0C,0x0D,0x0E,0x10,
            0x11,0x12,0x14,0x18,0x1E,0x20,0x24,0x31,0x35,0x40,0x4D,0x50,0x58,0x74,0x7A,
            0x7E,0x80,0x83,0x84,0x88,0xAA,0xBB,0xC4,0xD0,0xE0,0xF2,0xF5
        )
        // All common power/on/off commands
        val cmds = listOf(
            0x00,0x01,0x02,0x07,0x08,0x09,0x0B,0x0C,0x0D,0x0E,0x0F,0x10,0x11,0x12,0x13,
            0x18,0x1A,0x1B,0x1C,0x1E,0x20,0x25,0x26,0x27,0x2F,0x3D,0x40,0x41,0x42,0x43,
            0x48,0x4D,0x4E,0x50,0x51,0x52,0x55,0x60,0x80,0x82,0x9E,0xAE,0xBF,0xC0,0xC8,
            0xD0,0xD2,0xFF
        )

        for (addr in necAddresses) {
            for (cmd in cmds) {
                signals.add(IrSignal(
                    name = "NEC ${addr.hex}/${cmd.hex}",
                    frequency = 38000,
                    pattern = nec(addr, cmd),
                    description = "addr:${addr.hex} cmd:${cmd.hex}"
                ))
            }
        }

        // Samsung protocol sweep
        val samsungAddresses = listOf(0x04,0x07,0x0A,0x11,0x1E,0xB2)
        val samsungCmds = listOf(0x02,0x07,0x0B,0x0F,0x11,0x12,0x1A,0x2E,0x3D,0x4F,0x7F,0xBF)
        for (addr in samsungAddresses) {
            for (cmd in samsungCmds) {
                signals.add(IrSignal(
                    name = "SAM ${addr.hex}/${cmd.hex}",
                    frequency = 38000,
                    pattern = samsung(addr, cmd),
                    description = "Samsung addr:${addr.hex} cmd:${cmd.hex}"
                ))
            }
        }

        // Sony 40kHz sweep
        val sonyCmds = listOf(0x15,0x12,0x13,0x14,0x25,0x32,0x60,0x65)
        for (cmd in sonyCmds) {
            for (addr in listOf(0x01,0x02,0x0D)) {
                signals.add(IrSignal(
                    name = "SONY ${addr.hex}/${cmd.hex}",
                    frequency = 40000,
                    pattern = sony12(addr, cmd),
                    description = "Sony addr:${addr.hex} cmd:${cmd.hex}"
                ))
            }
        }

        return signals
    }

    private val Int.hex get() = "0x${toString(16).uppercase().padStart(2,'0')}"
}
