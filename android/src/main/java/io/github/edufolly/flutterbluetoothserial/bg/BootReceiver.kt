package io.github.edufolly.flutterbluetoothserial.bg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
//            BackgroundLocatorPlugin.registerAfterBoot(context) if enabled
        }
    }
}