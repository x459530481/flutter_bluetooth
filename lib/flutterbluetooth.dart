import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class Flutterbluetooth {
  static const MethodChannel _channel =
      const MethodChannel('flutterbluetooth');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  //发送命令 初始化（会触发设备是否有蓝牙、蓝牙是否开启）
  static Future<void> init() async {
    return _channel.invokeMethod('init');
  }
  //发送命令 连接指定地址蓝牙设备（会触发蓝牙连接成功、连接失败）
  static Future<void> connting(String macAddress) async {
    return _channel.invokeMethod('connting',{'macAddress':macAddress});
  }
  //发送命令 断开连接 （会触发蓝牙断开连接成功）
  static Future<void> disconnect() async {
    return _channel.invokeMethod('disconnect');
  }
  //发送命令 搜索蓝牙设备（会触发蓝牙连接失败）
  static Future<void> discovery() async {
    return _channel.invokeMethod('discovery');
  }
  //发送命令 停止搜索蓝牙设备（会触发蓝牙连接失败）
  static Future<void> cancelDiscovery() async {
    return _channel.invokeMethod('cancelDiscovery');
  }
  //发送命令 发送内容（会触发蓝牙连接失败）
  static Future<void> sendData(Uint8List byteArray) async {
    return _channel.invokeMethod('sendData',{'byteArray':byteArray});
  }
  //发送命令 转换hex2utf8（会触发转换成功、失败）
  static Future<void> hex2utf8(String hexStr) async {
    return _channel.invokeMethod('hex2utf8',{'hexStr':hexStr});
  }
}
