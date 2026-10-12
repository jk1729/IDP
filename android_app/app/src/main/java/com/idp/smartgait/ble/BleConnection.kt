package com.idp.smartgait.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.annotation.SuppressLint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@SuppressLint("MissingPermission")
class BleConnection(
 private val device: BluetoothDevice
) {

 private var gatt: BluetoothGatt? = null

 private val _packets =
  MutableSharedFlow<Pair<java.util.UUID, ByteArray>>(
   extraBufferCapacity = 16
  )

 val packets: SharedFlow<Pair<java.util.UUID, ByteArray>> = _packets

 fun connect(callback: BluetoothGattCallback) {
  try {
   gatt = device.connectGatt(
    null,
    false,
    callback
   )
  } catch (_: SecurityException) {
   gatt = null
  }
 }

 fun close() {
  try {
   gatt?.close()
  } catch (_: SecurityException) {
   // The connection may be closed after the runtime permission is revoked.
  }
  gatt = null
 }

 fun onConnected(g: BluetoothGatt) {
  gatt = g
  try {
   g.discoverServices()
  } catch (_: SecurityException) {
   close()
  }
 }

 fun onCharacteristicChanged(
  uuid: java.util.UUID,
  value: ByteArray
 ) {
  _packets.tryEmit(uuid to value)
 }

 fun write(command: ByteArray): Boolean {

  val characteristic =
   gatt
    ?.getService(BleUuids.SERVICE)
    ?.getCharacteristic(BleUuids.COMMAND)
    ?: return false

  characteristic.value = command

  return try {
   gatt?.writeCharacteristic(characteristic) == true
  } catch (_: SecurityException) {
   false
  }
 }
}