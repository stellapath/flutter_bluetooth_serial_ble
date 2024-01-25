package io.github.edufolly.flutterbluetoothserial.bg

data class BLEBackgroundConnectionParam(
    val address: String,
    val retryInterval: Long,
)