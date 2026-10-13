package com.idp.smartgait

import com.idp.smartgait.protocol.PacketParser
import org.junit.Assert.*
import org.junit.Test

class PacketParserTest {
    @Test
    fun parsesRisk() {
        val p = PacketParser.risk("""{"type":"risk","overall":0.7,"alert":true}""".toByteArray())
        assertEquals(.7f, p?.overall)
        assertTrue(p!!.alert)
    }

    @Test
    fun rejectsWrongType() {
        assertNull(PacketParser.risk("""{"type":"live"}""".toByteArray()))
    }

    @Test
    fun parsesValidSampleMessage() {
        val sample = "FSR1: 1234 | V1: 0.994 V | R1: 23212 ohm || FSR2: 1456 | V2: 1.173 V | R2: 18133 ohm"
        val reading = PacketParser.parseEsp32Fsr(sample)
        assertNotNull(reading)
        assertEquals(1234, reading!!.raw1)
        assertEquals(0.994f, reading.v1, 0.001f)
        assertEquals(23212f, reading.r1!!, 0.01f)
        assertEquals(1456, reading.raw2)
        assertEquals(1.173f, reading.v2, 0.001f)
        assertEquals(18133f, reading.r2!!, 0.01f)

        val packet = PacketParser.parseEsp32BleString(sample, timestamp = 123456L)
        assertNotNull(packet)
        assertEquals(123456L, packet!!.ts)
        assertEquals(1234f, packet.heel)
        assertEquals(1456f, packet.forefoot)
        assertEquals("ESP32_BLE", packet.mode)
        assertEquals(0.994f, packet.v1!!, 0.001f)
        assertEquals(23212f, packet.r1!!, 0.01f)
        assertEquals(1.173f, packet.v2!!, 0.001f)
        assertEquals(18133f, packet.r2!!, 0.01f)
    }

    @Test
    fun parsesZeroReadings() {
        val sample = "FSR1: 0 | V1: 0.000 V | R1: 0.0 ohm || FSR2: 0 | V2: 0.000 V | R2: 0.0 ohm"
        val reading = PacketParser.parseEsp32Fsr(sample)
        assertNotNull(reading)
        assertEquals(0, reading!!.raw1)
        assertEquals(0.0f, reading.v1, 0.001f)
        assertNull("Zero resistance should be treated as null/open circuit", reading.r1)
        assertEquals(0, reading.raw2)
        assertEquals(0.0f, reading.v2, 0.001f)
        assertNull("Zero resistance should be treated as null/open circuit", reading.r2)

        val packet = PacketParser.parseEsp32BleString(sample)
        assertNotNull(packet)
        assertEquals(0f, packet!!.heel)
        assertEquals(0f, packet.forefoot)
    }

    @Test
    fun parsesAdcLimitReadingsAt4095() {
        val sample = "FSR1: 4095 | V1: 3.300 V | R1: -1 ohm || FSR2: 4095 | V2: 3.300 V | R2: -1 ohm"
        val reading = PacketParser.parseEsp32Fsr(sample)
        assertNotNull(reading)
        assertEquals(4095, reading!!.raw1)
        assertEquals(3.300f, reading.v1, 0.001f)
        assertNull("Negative resistance -1 should be treated as unavailable (null)", reading.r1)
        assertEquals(4095, reading.raw2)
        assertEquals(3.300f, reading.v2, 0.001f)
        assertNull("Negative resistance -1 should be treated as unavailable (null)", reading.r2)

        val packet = PacketParser.parseEsp32BleString(sample)
        assertNotNull(packet)
        assertEquals(4095f, packet!!.heel)
        assertEquals(4095f, packet.forefoot)
        assertNull(packet.r1)
        assertNull(packet.r2)
    }

    @Test
    fun handlesNegativeResistanceSafely() {
        val sample = "FSR1: 500 | V1: 0.400 V | R1: -1.00 ohm || FSR2: 800 | V2: 0.650 V | R2: -1 ohm"
        val reading = PacketParser.parseEsp32Fsr(sample)
        assertNotNull(reading)
        assertEquals(500, reading!!.raw1)
        assertNull(reading.r1)
        assertEquals(800, reading.raw2)
        assertNull(reading.r2)
    }

    @Test
    fun handlesExtraWhitespaceAndLineEndings() {
        val sample = "  \r\n\t  FSR1:   1000  |  V1:  0.810  V  |  R1:  25000  ohm  ||  FSR2:  2000  |  V2:  1.620  V  |  R2:  12000  ohm  \r\n"
        val reading = PacketParser.parseEsp32Fsr(sample)
        assertNotNull(reading)
        assertEquals(1000, reading!!.raw1)
        assertEquals(0.810f, reading.v1, 0.001f)
        assertEquals(25000f, reading.r1!!, 0.01f)
        assertEquals(2000, reading.raw2)
        assertEquals(1.620f, reading.v2, 0.001f)
        assertEquals(12000f, reading.r2!!, 0.01f)
    }

    @Test
    fun rejectsMalformedAndIncompleteMessages() {
        assertNull(PacketParser.parseEsp32Fsr(""))
        assertNull(PacketParser.parseEsp32Fsr("   "))
        assertNull(PacketParser.parseEsp32Fsr("FSR1: 1234 | V1: 0.994 V")) // Incomplete
        assertNull(PacketParser.parseEsp32Fsr("FSR1: abc | V1: 0.994 V | R1: 23212 ohm || FSR2: 1456 | V2: 1.173 V | R2: 18133 ohm")) // Non-numeric
        assertNull(PacketParser.parseEsp32Fsr("Some random garbage line from serial"))
    }

    @Test
    fun rejectsOutOfRangeAdcValues() {
        // Raw ADC > 4095
        assertNull(PacketParser.parseEsp32Fsr("FSR1: 4096 | V1: 3.300 V | R1: 1000 ohm || FSR2: 1000 | V2: 1.000 V | R2: 1000 ohm"))
        // Raw ADC < 0
        assertNull(PacketParser.parseEsp32Fsr("FSR1: -5 | V1: 0.000 V | R1: 1000 ohm || FSR2: 1000 | V2: 1.000 V | R2: 1000 ohm"))
    }

    @Test
    fun parseEsp32BleHandlesRawBytes() {
        val sample = "FSR1: 2048 | V1: 1.650 V | R1: 10000 ohm || FSR2: 1024 | V2: 0.825 V | R2: 20000 ohm"
        val bytes = sample.toByteArray(Charsets.UTF_8)
        val packet = PacketParser.parseEsp32Ble(bytes)
        assertNotNull(packet)
        assertEquals(2048f, packet!!.heel)
        assertEquals(1024f, packet.forefoot)
        assertEquals(1.650f, packet.v1!!, 0.001f)
        assertEquals(10000f, packet.r1!!, 0.01f)
    }
}

