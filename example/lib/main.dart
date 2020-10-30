import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_bluetooth/flutterbluetooth.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String macAddress = '';
  MethodChannel _channel;
  void log(String str){
    print('_FlutterBluetoothView---'+str);
  }

  @override
  void initState() {
    super.initState();
    this._channel = new MethodChannel('flutterbluetooth');
    this._channel.setMethodCallHandler((handler) {
      switch (handler.method) {
        case "connection_successful":
          log(handler.arguments.toString());
          break;
        case "connection_failed":
          log(handler.arguments.toString());
          break;
        case "found_result":
          log(handler.arguments.toString());
          break;
        case "found_finish":
          log(handler.arguments.toString());
          break;
      }
    });

    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await Flutterbluetooth.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(

          child: Column(
            children: <Widget>[
              Text('Running on: $_platformVersion\n'),
              GestureDetector(
                onTap: (){
                  print('初始化蓝牙设备');
                  Flutterbluetooth.init();
                },
                child: Container(
                    alignment: Alignment.center,
                    height: 40,
                    child: Text(
                        '初始化蓝牙设备'
                    )
                ),
              ),
              GestureDetector(
                onTap: (){
                  print('搜索蓝牙设备');
                  Flutterbluetooth.discovery();
                },
                child: Container(
                    alignment: Alignment.center,
                    height: 40,
                    child: Text(
                        '搜索蓝牙设备'
                    )),
              ),
              GestureDetector(
                onTap: (){
                  Flutterbluetooth.cancelDiscovery();
                },
                child: Container(
                    alignment: Alignment.center,
                    height: 40,
                    child: Text(
                        '停止搜索蓝牙设备'
                    )),
              ),
              GestureDetector(
                onTap: (){
                  Flutterbluetooth.connting(macAddress);
                },
                child: Container(
                  alignment: Alignment.center,
                  height: 40,
                  child: Text(
                      '连接蓝牙设备'
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
