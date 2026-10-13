package com.idp.smartgait.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.idp.smartgait.data.ConnectionState
import com.idp.smartgait.protocol.CommandCodec
import com.idp.smartgait.repository.BleCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class GattDataSource(
    private val context: Context? = null,
    private val device: BluetoothDevice
) {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val _packets = MutableSharedFlow<Pair<UUID, ByteArray>>(extraBufferCapacity = 32)
    val packets: SharedFlow<Pair<UUID, ByteArray>> = _packets.asSharedFlow()

    private var gatt: BluetoothGatt? = null

    private fun allowed(permission: String): Boolean {
        val ctx = context ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun connect() {
        if (!allowed(Manifest.permission.BLUETOOTH_CONNECT)) {
            _errors.tryEmit("Bluetooth Connect permission is required")
            _connectionState.value = ConnectionState.ERROR
            return
        }

        try {
            _connectionState.value = ConnectionState.CONNECTING
            gatt = device.connectGatt(context, false, callback)
        } catch (_: SecurityException) {
            _errors.tryEmit("Bluetooth Connect permission is required")
            _connectionState.value = ConnectionState.ERROR
        } catch (e: Exception) {
            _errors.tryEmit("Failed to connect: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun close() {
        try {
            if (allowed(Manifest.permission.BLUETOOTH_CONNECT)) {
                gatt?.disconnect()
                gatt?.close()
            }
        } catch (_: SecurityException) {
            // Ignored on close
        } catch (_: Exception) {
            // Ignored on close
        } finally {
            gatt = null
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun read(uuid: UUID): Boolean {
        if (!allowed(Manifest.permission.BLUETOOTH_CONNECT)) {
            _errors.tryEmit("Bluetooth Connect permission is required")
            return false
        }

        val characteristic = gatt
            ?.getService(BleUuids.SERVICE)
            ?.getCharacteristic(uuid)
            ?: return false

        return try {
            gatt?.readCharacteristic(characteristic) == true
        } catch (_: SecurityException) {
            false
        }
    }

    fun writeCommand(command: BleCommand): Boolean {
        if (!allowed(Manifest.permission.BLUETOOTH_CONNECT)) {
            _errors.tryEmit("Bluetooth Connect permission is required")
            return false
        }

        val characteristic = gatt
            ?.getService(BleUuids.SERVICE)
            ?.getCharacteristic(BleUuids.COMMAND)
            ?: return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val data = CommandCodec.encode(command.wire).toByteArray()
                gatt?.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = CommandCodec.encode(command.wire).toByteArray()
                @Suppress("DEPRECATION")
                gatt?.writeCharacteristic(characteristic) == true
            }
        } catch (_: SecurityException) {
            false
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = g
                if (allowed(Manifest.permission.BLUETOOTH_CONNECT)) {
                    try {
                        g.discoverServices()
                    } catch (_: SecurityException) {
                        _errors.tryEmit("Permission denied during service discovery")
                        close()
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _errors.tryEmit("BLE disconnected")
                close()
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _errors.tryEmit("BLE service discovery failed (status: $status)")
                close()
                return
            }

            val service = g.getService(BleUuids.SERVICE)
            if (service == null) {
                _errors.tryEmit("ESP32 Service not found (${BleUuids.SERVICE})")
                close()
                return
            }

            val characteristic = service.getCharacteristic(BleUuids.FSR_CHARACTERISTIC)
            if (characteristic == null) {
                _errors.tryEmit("ESP32 FSR Characteristic not found (${BleUuids.FSR_CHARACTERISTIC})")
                close()
                return
            }

            if (!allowed(Manifest.permission.BLUETOOTH_CONNECT)) {
                _errors.tryEmit("Permission denied enabling notifications")
                close()
                return
            }

            try {
                // Enable local notification listener
                val notifSet = g.setCharacteristicNotification(characteristic, true)
                if (!notifSet) {
                    _errors.tryEmit("Failed to set characteristic notification locally")
                }

                // Write CCCD (0x2902) to peripheral to enable live notifications from ESP32
                val descriptor = characteristic.getDescriptor(BleUuids.CCCD)
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        g.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        g.writeDescriptor(descriptor)
                    }
                } else {
                    _errors.tryEmit("CCCD descriptor (0x2902) not found")
                    // If no CCCD descriptor is present, transition to CONNECTED directly
                    _connectionState.value = ConnectionState.CONNECTED
                }
            } catch (e: Exception) {
                _errors.tryEmit("Error configuring notifications: ${e.message}")
                close()
            }
        }

        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == BleUuids.CCCD) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Subscription confirmed! Connection is officially ready for live data
                    _connectionState.value = ConnectionState.CONNECTED
                } else {
                    _errors.tryEmit("Failed to subscribe to FSR notifications (status: $status)")
                    close()
                }
            }
        }

        // Android 13+ (API 33+) notification callback
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            _packets.tryEmit(characteristic.uuid to value)
        }

        // Backward-compatible notification callback for Android < 13
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value ?: return
            _packets.tryEmit(characteristic.uuid to value)
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                _packets.tryEmit(characteristic.uuid to value)
            }
        }
    }
}