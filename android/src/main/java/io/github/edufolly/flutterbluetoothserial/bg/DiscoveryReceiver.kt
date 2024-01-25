package io.github.edufolly.flutterbluetoothserial.bg

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class DiscoveryReceiver(
    val onDiscovery: (BluetoothDevice) -> Unit,
    val onFinish: () -> Unit,
) : BroadcastReceiver() {
    companion object {
        const val TAG = "DISCOVERY_RECEIVER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_FOUND -> {
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)?.let {
                    onDiscovery(it)
                }
                //final BluetoothClass deviceClass = intent.getParcelableExtra(BluetoothDevice.EXTRA_CLASS); // @TODO . !BluetoothClass!
                //final String extraName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME); // @TODO ? !EXTRA_NAME!
//                val deviceRSSI =
//                    intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
//                        .toInt()
//                val discoveryResult: MutableMap<String, Any> = HashMap()
//                discoveryResult["address"] = device!!.address
//                discoveryResult["name"] = device.name
//                discoveryResult["type"] = device.type
//                //discoveryResult.put("class", deviceClass); // @TODO . it isn't my priority for now !BluetoothClass!
//                discoveryResult["isConnected"] =
//                    FlutterBluetoothSerialPlugin.checkIsDeviceConnected(device)
//                discoveryResult["bondState"] = device.bondState
//                discoveryResult["rssi"] = deviceRSSI
//                Log.d(TAG, "Discovered " + device.address)
//                if (discoverySink != null) {
//                    discoverySink.success(discoveryResult)
//                }
            }

            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.d(TAG, "Discovery finished")
                try {
                    context.unregisterReceiver(this)
                } catch (ex: IllegalArgumentException) {
                    // Ignore `Receiver not registered` exception
                }

                onFinish()
//                bluetoothAdapter.cancelDiscovery()
//                if (discoverySink != null) {
//                    discoverySink.endOfStream()
//                    discoverySink = null
//                }
            }

            else -> {}
        }
    }
}