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
import androidx.core.content.ContextCompat
import com.idp.smartgait.protocol.CommandCodec
import com.idp.smartgait.repository.BleCommand
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@SuppressLint("MissingPermission")
class GattDataSource(
    private val context: Context,
    private val device: BluetoothDevice
) {

    private val _errors =
        MutableSharedFlow<String>(
            extraBufferCapacity = 4
        )

    val errors = _errors.asSharedFlow()

    private val _packets =
        MutableSharedFlow<Pair<java.util.UUID, ByteArray>>(
            extraBufferCapacity = 16
        )

    val packets = _packets.asSharedFlow()

    private var gatt: BluetoothGatt? = null

    private fun allowed(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun connect() {

        if (
            !allowed(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) {
            _errors.tryEmit(
                "Bluetooth Connect permission is required"
            )
            return
        }

        gatt = device.connectGatt(
            context,
            false,
            callback
        )
    }

    fun close() {

        if (
            allowed(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) {
            gatt?.close()
        }

        gatt = null
    }

    fun read(uuid: java.util.UUID): Boolean {

        if (
            !allowed(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) {
            _errors.tryEmit(
                "Bluetooth Connect permission is required"
            )
            return false
        }

        val characteristic =
            gatt
                ?.getService(BleUuids.SERVICE)
                ?.getCharacteristic(uuid)
                ?: return false

        return gatt?.readCharacteristic(
            characteristic
        ) == true
    }

    fun writeCommand(
        command: BleCommand
    ): Boolean {

        if (
            !allowed(
                Manifest.permission.BLUETOOTH_CONNECT
            )
        ) {
            _errors.tryEmit(
                "Bluetooth Connect permission is required"
            )
            return false
        }

        val characteristic =
            gatt
                ?.getService(BleUuids.SERVICE)
                ?.getCharacteristic(
                    BleUuids.COMMAND
                )
                ?: return false

        characteristic.value =
            CommandCodec
                .encode(command.wire)
                .toByteArray()

        return gatt?.writeCharacteristic(
            characteristic
        ) == true
    }

    private val callback =
        object : BluetoothGattCallback() {

            override fun onConnectionStateChange(
                g: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                if (
                    newState ==
                    BluetoothProfile.STATE_CONNECTED
                ) {

                    gatt = g

                    if (
                        allowed(
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    ) {
                        g.discoverServices()
                    }

                } else if (
                    newState ==
                    BluetoothProfile.STATE_DISCONNECTED
                ) {

                    _errors.tryEmit(
                        "BLE disconnected"
                    )
                }
            }

            override fun onServicesDiscovered(
                g: BluetoothGatt,
                status: Int
            ) {

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {
                    _errors.tryEmit(
                        "BLE service discovery failed"
                    )
                    return
                }

                val characteristics =
                    listOf(
                        BleUuids.LIVE_DATA,
                        BleUuids.RISK,
                        BleUuids.STATUS
                    )

                characteristics.forEach { uuid ->

                    val characteristic =
                        g.getService(
                            BleUuids.SERVICE
                        )?.getCharacteristic(uuid)

                    if (
                        characteristic != null &&
                        allowed(
                            Manifest.permission.BLUETOOTH_CONNECT
                        )
                    ) {

                        g.setCharacteristicNotification(
                            characteristic,
                            true
                        )

                        val descriptor =
                            characteristic.getDescriptor(
                                java.util.UUID.fromString(
                                    "00002902-0000-1000-8000-00805f9b34fb"
                                )
                            )

                        descriptor?.let {
                            it.value =
                                BluetoothGattDescriptor
                                    .ENABLE_NOTIFICATION_VALUE

                            g.writeDescriptor(it)
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {

                _packets.tryEmit(
                    characteristic.uuid to characteristic.value
                )
            }

            override fun onCharacteristicRead(
                g: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    _packets.tryEmit(
                        characteristic.uuid to characteristic.value
                    )
                }
            }
        }
}