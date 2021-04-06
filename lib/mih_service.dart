
import 'dart:async';

import 'package:flutter/services.dart';

class MihService {
  static const MethodChannel _channel =
      const MethodChannel('mih_service');


  static Future<String>  startService (var tripId) async {
    final String version =await _channel.invokeMethod("startService", <String, dynamic>{
      'tripId':tripId,
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

  static Future<String> get stopService async {
    print("onStartStopService");
    final String version =await _channel.invokeMethod("stopService", <String, dynamic>{});
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
