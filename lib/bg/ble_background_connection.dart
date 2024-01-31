import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_bluetooth_serial_ble/bg/background_connection_handler.dart';
import 'package:flutter_bluetooth_serial_ble/bg/ble_android_setting.dart';
import 'package:flutter_bluetooth_serial_ble/flutter_bluetooth_serial_ble.dart';

class BLEBackgroundConnection {
  static final MethodChannel _channel =
      const MethodChannel('${FlutterBluetoothSerial.namespace}/methods');

  // Future<void> registerBackgroundCallback(Function callback) async {
  //   final callbackHandle =
  //       PluginUtilities.getCallbackHandle(callback)!.toRawHandle();
  //   _backgroundChannel.invokeMethod("registerBackgroundCallback", {
  //     "callbackHandle": callbackHandle,
  //   });
  // }

  Future<bool> isServiceRunning() async {
    return await _channel.invokeMethod("isServiceRunning");
  }

  Future<void> startService({
    required Function(BluetoothDevice device, Uint8List bytes) callback,
    bool autoConnect = true,
    BLEAndroidSettings androidSettings = const BLEAndroidSettings(),
  }) {
    final initCallbackHandle =
        PluginUtilities.getCallbackHandle(handleBackgroundConnection)!
            .toRawHandle();
    final readCallbackhandle =
        PluginUtilities.getCallbackHandle(callback)!.toRawHandle();
    return _channel.invokeMapMethod("startService", {
      "initCallbackHandle": initCallbackHandle,
      "readCallbackHandle": readCallbackhandle,
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

  Future<void> foo() {
    return _channel.invokeMethod("foo");
  }
}
