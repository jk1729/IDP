package com.idp.smartgait.ble

import java.util.UUID

object BleUuids {
    const val DEVICE_NAME = "ESP32_FSR_BLE"
    val SERVICE: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    val FSR_CHARACTERISTIC: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    val CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // Aliases for compatibility
    val LIVE_DATA: UUID = FSR_CHARACTERISTIC
    val RISK: UUID = UUID.fromString("7e3a0003-5e8d-4f4a-9a9b-1a2b3c4d5e01")
    val STATUS: UUID = UUID.fromString("7e3a0004-5e8d-4f4a-9a9b-1a2b3c4d5e01")
    val COMMAND: UUID = UUID.fromString("7e3a0005-5e8d-4f4a-9a9b-1a2b3c4d5e01")
}
