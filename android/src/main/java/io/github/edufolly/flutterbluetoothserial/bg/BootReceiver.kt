package io.github.edufolly.flutterbluetoothserial.bg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val store = BLEBackgroundStore(context)
            val param = store.getServiceParam() ?: return
            val serviceIntent = Intent(context, BLEBackgroundConnection::class.java).apply {
                putExtra(BLEBackgroundConnection.startServiceParamKey, param)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}