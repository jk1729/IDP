package com.idp.smartgait.protocol

import kotlinx.serialization.json.Json

object PacketParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // Pattern matching: FSR1: 1234 | V1: 0.994 V | R1: 23212 ohm || FSR2: 1456 | V2: 1.173 V | R2: 18133 ohm
    // Handles whitespace variations, case insensitivity, optional units, and negative resistance values.
    private val ESP32_FSR_REGEX = Regex(
        """(?i)FSR1\s*:\s*(-?\d+)\s*\|\s*V1\s*:\s*([0-9.]+)\s*V?\s*\|\s*R1\s*:\s*([0-9.-]+)\s*(?:ohm)?\s*\|\|\s*FSR2\s*:\s*(-?\d+)\s*\|\s*V2\s*:\s*([0-9.]+)\s*V?\s*\|\s*R2\s*:\s*([0-9.-]+)\s*(?:ohm)?"""
    )

    fun risk(bytes: ByteArray): RiskPacket? = runCatching {
        json.decodeFromString<RiskPacket>(bytes.decodeToString()).takeIf { it.type == "risk" }
    }.getOrNull()

    fun live(bytes: ByteArray): LiveDataPacket? = runCatching {
        json.decodeFromString<LiveDataPacket>(bytes.decodeToString()).takeIf { it.type == "live" }
    }.getOrNull()

    fun status(bytes: ByteArray): StatusPacket? = runCatching {
        json.decodeFromString<StatusPacket>(bytes.decodeToString()).takeIf { it.type == "status" }
    }.getOrNull()

    /**
     * Parses the raw sensor reading string from ESP32 BLE into an [Esp32FsrReading].
     * Returns null if the format is invalid or if raw ADC values fall outside the 0..4095 range.
     */
    fun parseEsp32Fsr(text: String): Esp32FsrReading? {
        val trimmed = text.trim()
        val match = ESP32_FSR_REGEX.find(trimmed) ?: return null
        return try {
            val raw1 = match.groupValues[1].toInt()
            val v1 = match.groupValues[2].toFloat()
            val r1Val = match.groupValues[3].toFloat()
            val raw2 = match.groupValues[4].toInt()
            val v2 = match.groupValues[5].toFloat()
            val r2Val = match.groupValues[6].toFloat()

            // Validate 12-bit ADC range
            if (raw1 !in 0..4095 || raw2 !in 0..4095) {
                return null
            }

            // Treat -1 or non-positive/unreliable resistance as null (unavailable)
            val r1 = if (r1Val > 0f) r1Val else null
            val r2 = if (r2Val > 0f) r2Val else null

            Esp32FsrReading(
                raw1 = raw1,
                v1 = v1,
                r1 = r1,
                raw2 = raw2,
                v2 = v2,
                r2 = r2
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses an ESP32 BLE text message into an application [LiveDataPacket].
     */
    fun parseEsp32BleString(text: String, timestamp: Long = System.currentTimeMillis()): LiveDataPacket? {
        val reading = parseEsp32Fsr(text) ?: return null
        return LiveDataPacket(
            type = "live",
            ts = timestamp,
            heel = reading.raw1.toFloat(),
            forefoot = reading.raw2.toFloat(),
            mode = "ESP32_BLE",
            v1 = reading.v1,
            r1 = reading.r1,
            v2 = reading.v2,
            r2 = reading.r2
        )
    }

    /**
     * Parses raw UTF-8 bytes from BLE characteristic notification into a [LiveDataPacket].
     */
    fun parseEsp32Ble(bytes: ByteArray, timestamp: Long = System.currentTimeMillis()): LiveDataPacket? {
        return try {
            val text = bytes.toString(Charsets.UTF_8)
            parseEsp32BleString(text, timestamp)
        } catch (_: Exception) {
            null
        }
    }
}

