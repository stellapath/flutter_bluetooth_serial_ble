import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_bluetooth_serial_ble/bg/background_connection_handler.dart';
import 'package:flutter_bluetooth_serial_ble/bg/ble_android_setting.dart';
import 'package:flutter_bluetooth_serial_ble/flutter_bluetooth_serial_ble.dart';

class BLEBackgroundConnection {
  static final MethodChannel _channel =
      const MethodChannel('${FlutterBluetoothSerial.namespace}/methods');

  Future<bool> isServiceRunning() async {
    return await _channel.invokeMethod("isServiceRunning");
  }

  Future<void> startService({
    required Function() initCallback,
    required Function(BluetoothDevice device, Uint8List bytes) readCallback,
    Function(BluetoothDevice, bool connected)? stateChangeCallback,
    bool autoConnect = true,
    BLEAndroidSettings androidSettings = const BLEAndroidSettings(),
  }) {
    final serviceCallbackHandle =
        PluginUtilities.getCallbackHandle(handleBackgroundConnection)!
            .toRawHandle();
    final initCallbackHandle =
        PluginUtilities.getCallbackHandle(initCallback)!.toRawHandle();
    final readCallbackhandle =
        PluginUtilities.getCallbackHandle(readCallback)!.toRawHandle();
    final stateChangeCallbackHandle = stateChangeCallback != null
        ? PluginUtilities.getCallbackHandle(stateChangeCallback)!.toRawHandle()
        : null;
    return _channel.invokeMapMethod("startService", {
      "serviceCallbackHandle": serviceCallbackHandle,
      "initCallbackHandle": initCallbackHandle,
      "readCallbackHandle": readCallbackhandle,
      "stateChangeCallbackHandle": stateChangeCallbackHandle,
      "androidSettings": {
        "notificationTitle": androidSettings.notificationTitle,
        "notificationBody": androidSettings.notificationBody,
        "showConnections": androidSettings.showConnections,
        "scanInterval": androidSettings.scanInterval,
        "startAfterBoot": androidSettings.startAfterBoot,
      },
    });
  }

  Future<void> stopService() {
    return _channel.invokeMethod("stopService");
  }

  Future<void> connect({
    required String address,
  }) {
    return _channel.invokeMapMethod("connectOnBackground", {
      "address": address,
    });
  }

  Future<void> disconnect({required String address}) {
    return _channel.invokeMapMethod("disconnectOnBackground", {
      "address": address,
    });
  }

  Future<List<BluetoothDevice>> getConnectedDevices() {
    return _channel.invokeListMethod("getConnectedDevices").then(
        (value) => value!.map((e) => BluetoothDevice.fromMap(e)).toList());
  }

  Future<void> write(String address, Uint8List bytes) {
    return _channel.invokeMethod("writeOnBackground", {
      "address": address,
      "bytes": bytes,
    });
  }
}
