package io.github.edufolly.flutterbluetoothserial.bg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val serviceIntent = Intent(context, BLEBackgroundConnection::class.java)
        context?.stopService(serviceIntent)
    }
}