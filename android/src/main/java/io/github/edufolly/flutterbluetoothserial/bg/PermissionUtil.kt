package io.github.edufolly.flutterbluetoothserial.bg

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.edufolly.flutterbluetoothserial.FlutterBluetoothSerialPlugin

object PermissionUtil {
    @JvmStatic
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }

    @JvmStatic
    fun hasAllPermissions(context: Context): Boolean {
        return permissions.all { context.hasPermission(it) }
    }

    @JvmStatic
    fun requestAllPermissions(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), requestCode)
    }
}

private fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
