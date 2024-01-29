import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter_bluetooth_serial_ble/bg/background_connection_handler.dart';
import 'package:flutter_bluetooth_serial_ble/bg/ble_android_setting.dart';
import 'package:flutter_bluetooth_serial_ble/flutter_bluetooth_serial_ble.dart';

class BLEBackgroundConnection {
  static const channelId = "BLEBackgroundChannel";

  static final MethodChannel _foregroundChannel =
      const MethodChannel('${FlutterBluetoothSerial.namespace}/methods');
  static final MethodChannel _backgroundChannel = MethodChannel(channelId);

  // Future<void> registerBackgroundCallback(Function callback) async {
  //   final callbackHandle =
  //       PluginUtilities.getCallbackHandle(callback)!.toRawHandle();
  //   _backgroundChannel.invokeMethod("registerBackgroundCallback", {
  //     "callbackHandle": callbackHandle,
  //   });
  // }

  Future<bool> isServiceRunning() async {
    return await _foregroundChannel.invokeMethod("isServiceRunning");
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
    return _foregroundChannel.invokeMapMethod("startService", {
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
    return _foregroundChannel.invokeMethod("stopService");
  }

  Future<void> connect({
    required String address,
  }) {
    return _backgroundChannel.invokeMethod("connect", {
      "address": address,
    });
  }

  Future<void> disconnect({required String address}) {
    return _backgroundChannel.invokeMethod("disconnect", {
      "address": address,
    });
  }
}
