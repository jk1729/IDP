package com.idp.smartgait.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.idp.smartgait.data.ConnectionState

/** Owns the scanner and one GATT connection; packet decoding stays in the repository. */
class BleManager(private val adapter: BluetoothAdapter) {
    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state
    private var scanner: BleScanner? = null
    private var connection: BleConnection? = null

    fun scan(onDevice: (android.bluetooth.le.ScanResult) -> Unit) {
        _state.value = ConnectionState.SCANNING
        scanner = BleScanner(adapter, onDevice) { _state.value = ConnectionState.ERROR }.also { it.start() }
    }
    fun stopScan() { scanner?.stop(); scanner = null; if (_state.value == ConnectionState.SCANNING) _state.value = ConnectionState.DISCONNECTED }
    fun connect(device: android.bluetooth.BluetoothDevice) {
        stopScan(); _state.value = ConnectionState.CONNECTING
        connection = BleConnection(device).also { c ->
            c.connect(object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) { _state.value = ConnectionState.CONNECTED; c.onConnected(gatt) }
                    else if (newState == BluetoothProfile.STATE_DISCONNECTED) _state.value = ConnectionState.DISCONNECTED
                }
                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: android.bluetooth.BluetoothGattCharacteristic) {
                    c.onCharacteristicChanged(characteristic.uuid, characteristic.value)
                }
            })
        }
    }
    fun close() { connection?.close(); connection = null; _state.value = ConnectionState.DISCONNECTED }
}
