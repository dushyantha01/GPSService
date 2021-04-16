import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:mih_service/mih_service.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _location = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion="a";
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      //platformVersion = await MihService.startService("13","2021-04-07 13:22:57.980313");
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

  //stop GPS service
  Future<void> onButtonPress() async{
    await MihService.stopService;
  }

  //GPS service bind to the activity when reopen the app
  Future<void> onButtonPressBind() async{
    await MihService.bindService;
  }

  //sart service with sample data
  Future<void> onButtonPressStart() async{
    await MihService.startService("13","2021-04-07 13:22:57.980313");
  }

  //get latest current location
  Future<void> onButtonPressLocation() async{
    _location=await MihService.currentLocation;
    setState(() {
      _location = _location;
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
          child:
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: <Widget>[
              Text('Running on: $_platformVersion\n'),
              RaisedButton(
                child: Text('Stop Service'),
                onPressed: onButtonPress,
              ),

              RaisedButton(
                child: Text('Bind Service'),
                onPressed: onButtonPressBind,
              ),
              RaisedButton(
                child: Text('Start Service'),
                onPressed: onButtonPressStart,
              ),
              Text('Current Location: $_location\n'),
              RaisedButton(
                child: Text('get Current Location'),
                onPressed: onButtonPressLocation,
              ),
            ],
          ),
        ),
      ),
    );
  }


}
