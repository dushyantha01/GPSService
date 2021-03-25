
import 'dart:async';

import 'package:flutter/services.dart';

class MihService {
  static const MethodChannel _channel =
      const MethodChannel('mih_service');


  static Future<String> get platformVersion async {
    //final String version = await _channel.invokeMethod('getPlatformVersion');
    print("onStartCommandwwwwwwww111");
    final String version =await _channel.invokeMethod("getPlatformVersion", <String, dynamic>{
      'holdWakeLock': false,
      'icon': "ic_stat_hot_tub",
      'color': 0,
      'title': "title",
      'content': "content",
      'subtext': "subtext",
      'chronometer': false,
      'stop_action': false,
      'stop_icon': "ic_stat_hot_tub",
      'stop_text': "stopText",
    });
    //_channel.setMethodCallHandler(_handleMethod);
    return version;
  }

  Future<dynamic> _handleMethod(MethodCall call) async {
    switch (call.method) {
      case "getPlatformVersion":
      //debugPrint(call.arguments);
        return new Future.value("");
    }
  }


}
