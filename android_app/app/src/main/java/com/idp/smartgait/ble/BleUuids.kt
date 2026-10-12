package com.idp.smartgait.ble
import java.util.UUID
object BleUuids {
 const val DEVICE_NAME = "SMART_GAIT_001"
 val SERVICE = UUID.fromString("7e3a0001-5e8d-4f4a-9a9b-1a2b3c4d5e01")
 val LIVE_DATA = UUID.fromString("7e3a0002-5e8d-4f4a-9a9b-1a2b3c4d5e01")
 val RISK = UUID.fromString("7e3a0003-5e8d-4f4a-9a9b-1a2b3c4d5e01")
 val STATUS = UUID.fromString("7e3a0004-5e8d-4f4a-9a9b-1a2b3c4d5e01")
 val COMMAND = UUID.fromString("7e3a0005-5e8d-4f4a-9a9b-1a2b3c4d5e01")
}
