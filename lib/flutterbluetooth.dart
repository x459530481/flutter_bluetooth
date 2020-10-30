import 'dart:async';

import 'package:flutter/services.dart';

class Flutterbluetooth {
  static const MethodChannel _channel =
      const MethodChannel('flutterbluetooth');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<void> init() async {
    return _channel.invokeMethod('init');
  }
  static Future<void> connting(String macAddress) async {
    return _channel.invokeMethod('connting',{'macAddress':macAddress});
  }
  static Future<void> disconnect() async {
    return _channel.invokeMethod('disconnect');
  }
  static Future<void> discovery() async {
    return _channel.invokeMethod('discovery');
  }
  static Future<void> cancelDiscovery() async {
    return _channel.invokeMethod('cancelDiscovery');
  }
  static Future<void> sendData(List byteArray) async {
    return _channel.invokeMethod('sendData',{'byteArray':byteArray});
  }
}
