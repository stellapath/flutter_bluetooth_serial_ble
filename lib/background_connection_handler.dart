import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_bluetooth_serial_ble/ble_background_connection.dart';

const readChannelId = "${BLEBackgroundConnection.channelId}/read";

@pragma('vm:entry-point')
void handleBackgroundConnection() {
  WidgetsFlutterBinding.ensureInitialized();

  const _readChannel = EventChannel(readChannelId);
  _readChannel.receiveBroadcastStream().listen((event) {
    final callback = PluginUtilities.getCallbackFromHandle(
        CallbackHandle.fromRawHandle(event['callbackHandle']));
    callback?.call(event['bytes']);
  });
}
