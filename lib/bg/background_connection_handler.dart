import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_bluetooth_serial_ble/flutter_bluetooth_serial_ble.dart';

const initChannelId = "BLEBackgroundChannel/init";
const readChannelId = "BLEBackgroundChannel/read";
const stateChangeChannelId = "BLEBackgroundChannel/stateChange";

@pragma('vm:entry-point')
void handleBackgroundConnection() {
  WidgetsFlutterBinding.ensureInitialized();

  const initChannel = EventChannel(initChannelId);
  initChannel.receiveBroadcastStream().listen((event) {
    final callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(event['initCallbackHandle']));
    callback?.call();
  });

  const readChannel = EventChannel(readChannelId);
  readChannel.receiveBroadcastStream().listen((event) {
    final callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(event['readCallbackHandle']));
    callback?.call(BluetoothDevice.fromMap(event['device']), event['bytes']);
  });

  const stateChangeChannel = EventChannel(stateChangeChannelId);
  stateChangeChannel.receiveBroadcastStream().listen((event) {
    final callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(event['stateChangeCallbackHandle']));
    callback?.call(
        BluetoothDevice.fromMap(event['device']), event['connected']);
  });
}
