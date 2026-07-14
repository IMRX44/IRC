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

    // NEC Protocol helper: encode address + command to pulse pattern
    private fun necProtocol(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()
        // Leader
        pattern.add(9000); pattern.add(4500)
        // Address (8 bits LSB first)
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((address shr i) and 1 == 1) 1690 else 560)
        }
        // Address inverted
        val addrInv = address.inv() and 0xFF
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((addrInv shr i) and 1 == 1) 1690 else 560)
        }
        // Command (8 bits)
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((command shr i) and 1 == 1) 1690 else 560)
        }
        // Command inverted
        val cmdInv = command.inv() and 0xFF
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((cmdInv shr i) and 1 == 1) 1690 else 560)
        }
        // Stop bit
        pattern.add(560)
        return pattern.toIntArray()
    }

    // Samsung protocol
    private fun samsungProtocol(address: Int, command: Int): IntArray {
        val pattern = mutableListOf<Int>()
        pattern.add(4500); pattern.add(4500)
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((address shr i) and 1 == 1) 1690 else 560)
        }
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((address shr i) and 1 == 1) 1690 else 560)
        }
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((command shr i) and 1 == 1) 1690 else 560)
        }
        val cmdInv = command.inv() and 0xFF
        for (i in 0..7) {
            pattern.add(560)
            pattern.add(if ((cmdInv shr i) and 1 == 1) 1690 else 560)
        }
        pattern.add(560)
        return pattern.toIntArray()
    }

    val tvBrands = listOf(
        DeviceBrand("Samsung", listOf(
            IrSignal("Power", 38000, samsungProtocol(0x07, 0x02), "Power Toggle"),
            IrSignal("Vol+", 38000, samsungProtocol(0x07, 0x07), "Volume Up"),
            IrSignal("Vol-", 38000, samsungProtocol(0x07, 0x0B), "Volume Down"),
            IrSignal("Mute", 38000, samsungProtocol(0x07, 0x0F), "Mute"),
            IrSignal("Ch+", 38000, samsungProtocol(0x07, 0x12), "Channel Up"),
            IrSignal("Ch-", 38000, samsungProtocol(0x07, 0x10), "Channel Down"),
            IrSignal("Menu", 38000, samsungProtocol(0x07, 0x1A), "Menu"),
            IrSignal("Source", 38000, samsungProtocol(0x07, 0x01), "Source/Input"),
            IrSignal("Home", 38000, samsungProtocol(0x07, 0x79), "Home"),
            IrSignal("Back", 38000, samsungProtocol(0x07, 0x58), "Back"),
            IrSignal("OK", 38000, samsungProtocol(0x07, 0x68), "OK/Enter"),
            IrSignal("Up", 38000, samsungProtocol(0x07, 0x60), "Up"),
            IrSignal("Down", 38000, samsungProtocol(0x07, 0x61), "Down"),
            IrSignal("Left", 38000, samsungProtocol(0x07, 0x65), "Left"),
            IrSignal("Right", 38000, samsungProtocol(0x07, 0x62), "Right"),
        )),
        DeviceBrand("LG", listOf(
            IrSignal("Power", 38000, necProtocol(0x04, 0x08), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x04, 0x02), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x04, 0x03), "Volume Down"),
            IrSignal("Mute", 38000, necProtocol(0x04, 0x09), "Mute"),
            IrSignal("Ch+", 38000, necProtocol(0x04, 0x00), "Channel Up"),
            IrSignal("Ch-", 38000, necProtocol(0x04, 0x01), "Channel Down"),
            IrSignal("Menu", 38000, necProtocol(0x04, 0x43), "Menu"),
            IrSignal("OK", 38000, necProtocol(0x04, 0x44), "OK"),
            IrSignal("Back", 38000, necProtocol(0x04, 0x28), "Back"),
            IrSignal("Home", 38000, necProtocol(0x04, 0xAE), "Home"),
            IrSignal("Up", 38000, necProtocol(0x04, 0x40), "Up"),
            IrSignal("Down", 38000, necProtocol(0x04, 0x41), "Down"),
            IrSignal("Left", 38000, necProtocol(0x04, 0x07), "Left"),
            IrSignal("Right", 38000, necProtocol(0x04, 0x06), "Right"),
        )),
        DeviceBrand("Sony", listOf(
            IrSignal("Power", 40000, intArrayOf(2400,600,600,600,1200,600,600,600,600,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Power Toggle"),
            IrSignal("Vol+", 40000, intArrayOf(2400,600,1200,600,600,600,600,600,600,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Volume Up"),
            IrSignal("Vol-", 40000, intArrayOf(2400,600,600,600,1200,600,600,600,1200,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Volume Down"),
            IrSignal("Mute", 40000, intArrayOf(2400,600,1200,600,1200,600,600,600,600,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Mute"),
            IrSignal("Ch+", 40000, intArrayOf(2400,600,600,600,600,600,1200,600,600,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Channel Up"),
            IrSignal("Ch-", 40000, intArrayOf(2400,600,1200,600,600,600,1200,600,600,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Channel Down"),
            IrSignal("Input", 40000, intArrayOf(2400,600,600,600,1200,600,1200,600,1200,600,600,600,600,600,600,600,1200,600,600,600,600,600,600,600,600), "Input Select"),
        )),
        DeviceBrand("Philips", listOf(
            IrSignal("Power", 36000, necProtocol(0x04, 0x0C), "Power Toggle"),
            IrSignal("Vol+", 36000, necProtocol(0x04, 0x10), "Volume Up"),
            IrSignal("Vol-", 36000, necProtocol(0x04, 0x11), "Volume Down"),
            IrSignal("Mute", 36000, necProtocol(0x04, 0x0D), "Mute"),
            IrSignal("Ch+", 36000, necProtocol(0x04, 0x20), "Channel Up"),
            IrSignal("Ch-", 36000, necProtocol(0x04, 0x21), "Channel Down"),
        )),
        DeviceBrand("TCL", listOf(
            IrSignal("Power", 38000, necProtocol(0x84, 0x08), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x84, 0x02), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x84, 0x03), "Volume Down"),
            IrSignal("Mute", 38000, necProtocol(0x84, 0x09), "Mute"),
            IrSignal("Ch+", 38000, necProtocol(0x84, 0x00), "Channel Up"),
            IrSignal("Ch-", 38000, necProtocol(0x84, 0x01), "Channel Down"),
        )),
        DeviceBrand("Haier", listOf(
            IrSignal("Power", 38000, necProtocol(0x40, 0x12), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x40, 0x02), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x40, 0x03), "Volume Down"),
        )),
        DeviceBrand("Hisense", listOf(
            IrSignal("Power", 38000, necProtocol(0x00, 0x08), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x00, 0x02), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x00, 0x03), "Volume Down"),
            IrSignal("Mute", 38000, necProtocol(0x00, 0x09), "Mute"),
            IrSignal("Ch+", 38000, necProtocol(0x00, 0x00), "Channel Up"),
            IrSignal("Ch-", 38000, necProtocol(0x00, 0x01), "Channel Down"),
        )),
        DeviceBrand("Sharp", listOf(
            IrSignal("Power", 38000, necProtocol(0xAA, 0xA8), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0xAA, 0xB8), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0xAA, 0xB0), "Volume Down"),
            IrSignal("Mute", 38000, necProtocol(0xAA, 0xA0), "Mute"),
        )),
    )

    // AC signals - real Daikin/Midea/etc protocols simplified for demo
    val acBrands = listOf(
        DeviceBrand("Daikin", listOf(
            IrSignal("Power On (Cool 24°)", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,
                420,420,420,420,420,1310,420,420,420,1310,420,1310,420,1310,420,420,
                420,1310,420,420,420,420,420,420,420,420,420,1310,420,1310,420,420,
                420,420,420,1310,420,420,420,1310,420,420,420,1310,420,1310,420,1310,
                420,29000
            ), "Power On - Cool Mode 24°C"),
            IrSignal("Power Off", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,
                420,420,420,420,420,420,420,1310,420,420,420,420,420,420,420,1310,
                420,1310,420,420,420,420,420,420,420,1310,420,420,420,420,420,420,
                420,420,420,1310,420,1310,420,1310,420,1310,420,1310,420,1310,420,
                29000
            ), "Power Off"),
            IrSignal("Temp+", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,1310,420,1310,420,420,420,420,
                420,420,420,420,420,420,420,1310,420,420,420,1310,420,420,420,1310,
                420,29000
            ), "Temperature Up"),
            IrSignal("Temp-", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,1310,420,420,420,1310,420,420,
                420,420,420,420,420,420,420,1310,420,420,420,1310,420,420,420,1310,
                420,29000
            ), "Temperature Down"),
            IrSignal("Mode: Cool", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,1310,420,1310,420,1310,420,420,
                420,420,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,
                420,29000
            ), "Cool Mode"),
            IrSignal("Mode: Heat", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,1310,420,1310,420,1310,420,1310,
                420,420,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,
                420,29000
            ), "Heat Mode"),
            IrSignal("Fan Auto", 38000, intArrayOf(
                3500,1750,420,1310,420,420,420,420,420,420,420,420,420,420,420,420,
                420,1310,420,420,420,420,420,1310,420,420,420,420,420,1310,420,1310,
                420,29000
            ), "Fan Auto Speed"),
        )),
        DeviceBrand("Midea", listOf(
            IrSignal("Power On (Cool 25°)", 38000, intArrayOf(
                4500,4500,560,1690,560,560,560,560,560,560,560,1690,560,1690,560,1690,
                560,560,560,1690,560,560,560,560,560,1690,560,560,560,560,560,560,
                560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,1690,
                560,1690,560,560,560,1690,560,1690,560,1690,560,560,560,560,560,560,
                560
            ), "Power On Cool 25°C"),
            IrSignal("Power Off", 38000, intArrayOf(
                4500,4500,560,1690,560,560,560,560,560,560,560,560,560,1690,560,1690,
                560,560,560,1690,560,1690,560,1690,560,560,560,1690,560,1690,560,1690,
                560
            ), "Power Off"),
            IrSignal("Temp 18°C Cool", 38000, intArrayOf(
                4500,4500,560,1690,560,560,560,560,560,560,560,1690,560,1690,560,1690,
                560,560,560,1690,560,560,560,1690,560,560,560,1690,560,560,560,560,
                560
            ), "18°C Cool Mode"),
            IrSignal("Temp 20°C Cool", 38000, intArrayOf(
                4500,4500,560,1690,560,560,560,560,560,1690,560,560,560,1690,560,1690,
                560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,560,
                560
            ), "20°C Cool Mode"),
            IrSignal("Temp 22°C Cool", 38000, intArrayOf(
                4500,4500,560,1690,560,560,560,560,560,1690,560,1690,560,560,560,1690,
                560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,560,560,
                560
            ), "22°C Cool Mode"),
        )),
        DeviceBrand("LG AC", listOf(
            IrSignal("Power On Cool 24°", 38000, necProtocol(0x88, 0xC8), "Power On Cool"),
            IrSignal("Power Off", 38000, necProtocol(0x88, 0x08), "Power Off"),
            IrSignal("Temp+", 38000, necProtocol(0x88, 0x00), "Temperature Up"),
            IrSignal("Temp-", 38000, necProtocol(0x88, 0x80), "Temperature Down"),
            IrSignal("Fan Low", 38000, necProtocol(0x88, 0x40), "Fan Low Speed"),
            IrSignal("Fan Med", 38000, necProtocol(0x88, 0x20), "Fan Medium Speed"),
            IrSignal("Fan High", 38000, necProtocol(0x88, 0x10), "Fan High Speed"),
        )),
        DeviceBrand("Samsung AC", listOf(
            IrSignal("Power On Cool 24°", 38000, samsungProtocol(0xB2, 0xBF), "Power On Cool"),
            IrSignal("Power Off", 38000, samsungProtocol(0xB2, 0x7F), "Power Off"),
            IrSignal("Temp+", 38000, samsungProtocol(0xB2, 0x9E), "Temperature Up"),
            IrSignal("Temp-", 38000, samsungProtocol(0xB2, 0x9F), "Temperature Down"),
        )),
        DeviceBrand("Gree", listOf(
            IrSignal("Power On Cool 24°", 38000, intArrayOf(
                9000,4500,560,1690,560,560,560,560,560,1690,560,560,560,1690,560,1690,
                560,560,560,1690,560,560,560,560,560,1690,560,1690,560,560,560,1690,560
            ), "Power On Cool"),
            IrSignal("Power Off", 38000, intArrayOf(
                9000,4500,560,560,560,1690,560,1690,560,560,560,1690,560,560,560,560,
                560,1690,560,560,560,1690,560,1690,560,560,560,560,560,1690,560,560,560
            ), "Power Off"),
        )),
    )

    val projectorBrands = listOf(
        DeviceBrand("Epson", listOf(
            IrSignal("Power", 38000, necProtocol(0x35, 0x01), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x35, 0x03), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x35, 0x04), "Volume Down"),
            IrSignal("Mute", 38000, necProtocol(0x35, 0x05), "Mute/Freeze"),
            IrSignal("Source", 38000, necProtocol(0x35, 0x06), "Source"),
            IrSignal("Menu", 38000, necProtocol(0x35, 0x09), "Menu"),
            IrSignal("Auto", 38000, necProtocol(0x35, 0x0A), "Auto Adjustment"),
        )),
        DeviceBrand("BenQ", listOf(
            IrSignal("Power", 38000, necProtocol(0x83, 0x08), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x83, 0x55), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x83, 0x56), "Volume Down"),
            IrSignal("Mute", 38000, necProtocol(0x83, 0x50), "Mute"),
            IrSignal("Source", 38000, necProtocol(0x83, 0x11), "Source"),
            IrSignal("Menu", 38000, necProtocol(0x83, 0x40), "Menu"),
        )),
        DeviceBrand("ViewSonic", listOf(
            IrSignal("Power", 38000, necProtocol(0x58, 0x01), "Power Toggle"),
            IrSignal("Vol+", 38000, necProtocol(0x58, 0x07), "Volume Up"),
            IrSignal("Vol-", 38000, necProtocol(0x58, 0x0B), "Volume Down"),
            IrSignal("Menu", 38000, necProtocol(0x58, 0x1A), "Menu"),
        )),
    )

    val dvdBrands = listOf(
        DeviceBrand("Samsung DVD", listOf(
            IrSignal("Power", 38000, samsungProtocol(0x1E, 0x11), "Power Toggle"),
            IrSignal("Play", 38000, samsungProtocol(0x1E, 0x24), "Play"),
            IrSignal("Pause", 38000, samsungProtocol(0x1E, 0x25), "Pause"),
            IrSignal("Stop", 38000, samsungProtocol(0x1E, 0x26), "Stop"),
            IrSignal("Next", 38000, samsungProtocol(0x1E, 0x2C), "Next Chapter"),
            IrSignal("Prev", 38000, samsungProtocol(0x1E, 0x2E), "Previous Chapter"),
            IrSignal("Vol+", 38000, samsungProtocol(0x1E, 0x07), "Volume Up"),
            IrSignal("Vol-", 38000, samsungProtocol(0x1E, 0x0B), "Volume Down"),
        )),
        DeviceBrand("Sony DVD", listOf(
            IrSignal("Power", 40000, intArrayOf(2400,600,600,600,1200,600,1200,600,600,600,600,600,1200,600,600,600,600), "Power Toggle"),
            IrSignal("Play", 40000, intArrayOf(2400,600,1200,600,1200,600,1200,600,600,600,600,600,1200,600,600,600,600), "Play"),
            IrSignal("Pause", 40000, intArrayOf(2400,600,600,600,600,600,600,600,1200,600,600,600,1200,600,600,600,600), "Pause"),
            IrSignal("Stop", 40000, intArrayOf(2400,600,1200,600,600,600,600,600,1200,600,600,600,1200,600,600,600,600), "Stop"),
        )),
    )

    val categories = listOf(
        DeviceCategory("tv", "تلویزیون", "📺", tvBrands),
        DeviceCategory("ac", "کولر گازی", "❄️", acBrands),
        DeviceCategory("projector", "پروژکتور", "📽️", projectorBrands),
        DeviceCategory("dvd", "DVD / Blu-ray", "💿", dvdBrands),
    )

    // Generate all possible NEC codes for brute-force scanning
    fun generateBruteForceSignals(frequency: Int = 38000): List<IrSignal> {
        val signals = mutableListOf<IrSignal>()
        // Common power-on addresses used by major manufacturers
        val commonAddresses = listOf(
            0x00, 0x01, 0x02, 0x04, 0x07, 0x08, 0x10, 0x20,
            0x40, 0x7F, 0x80, 0x84, 0x88, 0xA8, 0xAA, 0xBF,
            0x35, 0x58, 0x83, 0x1E, 0x04, 0x06, 0xFF
        )
        val powerCommands = listOf(0x08, 0x02, 0x0C, 0x12, 0x01, 0x00, 0x10, 0x09, 0x18, 0x48)

        for (addr in commonAddresses) {
            for (cmd in powerCommands) {
                signals.add(IrSignal(
                    name = "NEC 0x${addr.toString(16).uppercase()}/0x${cmd.toString(16).uppercase()}",
                    frequency = frequency,
                    pattern = necProtocol(addr, cmd),
                    description = "Address: 0x${addr.toString(16).uppercase()}, Command: 0x${cmd.toString(16).uppercase()}"
                ))
            }
        }
        return signals
    }
}
