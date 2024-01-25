import 'dart:ui';

import 'package:flutter/services.dart';

class BLEBackgroundConnection {
  static const channelId = "BLEBackgroundChannel";

  static final MethodChannel _backgroundChannel = MethodChannel(channelId);

  // Future<void> registerBackgroundCallback(Function callback) async {
  //   final callbackHandle =
  //       PluginUtilities.getCallbackHandle(callback)!.toRawHandle();
  //   _backgroundChannel.invokeMethod("registerBackgroundCallback", {
  //     "callbackHandle": callbackHandle,
  //   });
  // }

  Future<void> connect({
    required String address,
    required Function(Uint8List bytes) callback,
  }) {
    final callbackHandle =
        PluginUtilities.getCallbackHandle(callback)!.toRawHandle();
    return _backgroundChannel.invokeMethod("connect", {
      "address": address,
      "callbackHandle": callbackHandle,
    });
  }
}
