package com.orel.mih_service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

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

  //gps based -start
  LocationListener locationListener=null;
  LocationManager locationManager= null;
  //  end
  //  Messenger for communicating with the service.
  Messenger mMessenger = null;
  // Flag indicating whether we have called bind on the service.
  boolean mBound;
  final Messenger rMessenger = new Messenger(new IncomingHandler());


  class IncomingHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case LocationService.MSG_SET_INT_VALUE:
          //textIntValue.setText("Int Message: " + msg.arg1);
          break;
        case LocationService.MSG_LOCATION_LISTENING:
          String location = msg.getData().getString("location");
          Toast.makeText(context,location,Toast.LENGTH_SHORT).show();
          //textStrValue.setText("Str Message: " + str1);
          break;
        default:
          super.handleMessage(msg);
      }
    }
  }

  // Class for interacting with the main interface of the service.
  private ServiceConnection mConnection = new ServiceConnection() {
    public void onServiceConnected(ComponentName className, IBinder iBinder) {
      // This is called when the connection with the iBinder has been established, giving us the object we can use
      // to interact with the iBinder.  We are communicating with the iBinder using a Messenger, so here we get a
      // client-side representation of that from the raw IBinder object.
      mMessenger = new Messenger(iBinder);
      mBound = true;
      try {
        Message msg = Message.obtain(null, LocationService.MSG_LOCATION_LISTENING);
        msg.replyTo = rMessenger;
        mMessenger.send(msg);
      }
      catch (RemoteException e) {
        Log.e("TAG", "Exception "+e.getMessage());
        // In this case the service has crashed before we could even do anything with it
      }
      Log.e("TAG -M BOUND", "service"+mBound);
    }

    public void onServiceDisconnected(ComponentName className) {
      // This is called when the connection with the service has been unexpectedly disconnected -- that is,
      // its process crashed.
      mMessenger = null;
      mBound = false;

    }
  };

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    this.context = flutterPluginBinding.getApplicationContext();
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mih_service");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.e("TAG", "method called");
    if (call.method.equals("startService")) {

      /*FirebaseFirestore db = FirebaseFirestore.getInstance();
      // Create a new user with a first and last name
      Map<String, Object> user = new HashMap<>();
      user.put("first", "Ada");
      user.put("last", "Lovelace");
      user.put("born", 1815);
*/
// Add a new document with a generated ID
    /*  db.collection("users")
              .add(user)
              .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                  Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                }
              })
              .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                  Log.w("TAG", "Error adding document", e);
                }
              });
*/


      final Boolean holdWakeLock = call.argument("holdWakeLock");
      final String icon = call.argument("icon");
      final int color = call.argument("color");
      final String tripId = call.argument("tripId");
      final String startTimestamp = call.argument("startTimestamp");
      final String title = call.argument("title");
      final String content = call.argument("content");
      final String subtext = call.argument("subtext");
      final Boolean chronometer = call.argument("chronometer");
      final Boolean stopAction = call.argument("stop_action");
      final String stopIcon = call.argument("stop_icon");
      final String stopText = call.argument("stop_text");

      launchForegroundService(startTimestamp,tripId,icon, color, title, content, subtext, chronometer, stopAction, stopIcon, stopText);
      result.success("startForegroundService");

    } else if (call.method.equals("stopService")) {

      Log.e("TAG", "method called stopService");
      Intent intent = new Intent(context, LocationService.class);
      intent.setAction(STOP_FOREGROUND_ACTION);
      //
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent);
      } else {
        context.startService(intent);
      }
      context.unbindService(mConnection);
      result.success("stopForegroundService");
    }
    else if (call.method.equals("bindService")) {
      Intent intent = new Intent(context, LocationService.class);
      context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }
    else if (call.method.equals("currentLocation")) {
        // Acquire a reference to the system Location Manager
       locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
        @Override
        public void onLocationChanged(Location location) {
          Log.e("success", ""+location.getLatitude()+","+location.getLongitude());
          locationManager.removeUpdates(locationListener);
          result.success(""+location.getLatitude()+","+location.getLongitude());

        }
        public void onProviderEnabled(String provider) {}

        public void onProviderDisabled(String provider) {}
      };

// Register the listener with the Location Manager to receive location updates
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }
    else
    {
      result.notImplemented();
    }
  }

  private void launchForegroundService(String startTimestamp,String tripId,String icon, int color, String title, String content, String subtext,
                                       Boolean chronometer, Boolean stopAction, String stopIcon,
                                       String stopText) {
    Log.e("TAG", "launch service called");
    Intent intent = new Intent(context, LocationService.class);
    intent.setAction(START_FOREGROUND_ACTION);
    intent.putExtra("icon", icon);
    intent.putExtra("color", color);
    intent.putExtra("tripId", tripId);
    intent.putExtra("startTimestamp", startTimestamp);
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
    context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    serviceStarted = true;

    //start locatioin listening
    //startServiceLoop();
    //callbackChannel.invokeMethod("onStarted", null);
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    Log.e("TAG DEATCHED", "DEATACHED FROM ENGINE");
    context.unbindService(mConnection);
  }
}
