package com.orel.mih_service;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** MihServicePlugin */
public class MihServicePlugin implements FlutterPlugin, MethodCallHandler {
  /// The MethodChannel that will the communication between Flutter and native Android
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  public final static String START_FOREGROUND_ACTION = "com.orel.bg_service.action.startforeground";
  public final static String STOP_FOREGROUND_ACTION = "com.orel.bg_service.action.stopforeground";
  private Context context;
  private int methodInterval = -1;
  private long dartServiceMethodHandle = -1;
  private boolean serviceStarted = false;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.context = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mih_service");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.e("TAG", "onStwwwwwwwwwwww4444444444444");
    if (call.method.equals("getPlatformVersion")) {
      final Boolean holdWakeLock = call.argument("holdWakeLock");
      final String icon = call.argument("icon");
      final int color = call.argument("color");
      final String title = call.argument("title");
      final String content = call.argument("content");
      final String subtext = call.argument("subtext");
      final Boolean chronometer = call.argument("chronometer");
      final Boolean stopAction = call.argument("stop_action");
      final String stopIcon = call.argument("stop_icon");
      final String stopText = call.argument("stop_text");

      launchForegroundService(icon, color, title, content, subtext, chronometer, stopAction, stopIcon, stopText);
      result.success("startForegroundService");
      //result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else {
      result.notImplemented();
    }
  }

  private void launchForegroundService(String icon, int color, String title, String content, String subtext,
                                       Boolean chronometer, Boolean stopAction, String stopIcon,
                                       String stopText) {
    Log.e("TAG", "onStwwwwwwwwwwww55555555555");
    Intent intent = new Intent(context, LocationService.class);
    intent.setAction(START_FOREGROUND_ACTION);
    intent.putExtra("icon", icon);
    intent.putExtra("color", color);
    intent.putExtra("title", title);
    intent.putExtra("content", content);
    intent.putExtra("subtext", subtext);
    intent.putExtra("chronometer", chronometer);
    intent.putExtra("stop_action", stopAction);
    intent.putExtra("stop_icon", stopIcon);
    intent.putExtra("stop_text", stopText);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.startForegroundService(intent);
    } else {
      context.startService(intent);
    }

    serviceStarted = true;
    //startServiceLoop();
    //callbackChannel.invokeMethod("onStarted", null);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}
