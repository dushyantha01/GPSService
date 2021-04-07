package com.orel.mih_service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.ServiceState;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service implements LocationListener {
    private static String TAG = "FlutterForegroundService";
    public static int ONGOING_NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_CHANNEL_ID = "CHANNEL_ID";
    public static final String ACTION_STOP_SERVICE = "STOP";
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;

    //location attributes -start//
    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    double latitude,longitude;
    LocationManager locationManager;
    Location location;
    private Handler mHandler = new Handler();
    private Timer mTimer = null;
    long notify_interval = 5000;
    public static String str_receiver = "servicetutorial.service.receiver";
    Intent intent;
    // end //

    // messenger service used for communicate with dart -start//
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static final String HELLO = "hello!";
    public static final int MSG_SAY_HELLO = 1;
    public static final int MSG_LOCATION_LISTENING = 101;
    private Messenger messengerListner = null;

    private List<Map<String, Object>> geoHistory;
    // end //

    // firestore functions -start
    private CloudService cService;
    // end

    private String tripId= null;
    private String startTimestamp= null;

    private boolean userStopForegroundService = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStartCommand: Intent is null");
            return START_NOT_STICKY;
        }
        if (intent.getAction() == null) {
            Log.d(TAG, "onStartCommand: Intent action is null");
            return START_NOT_STICKY;
        }
        cService=new CloudService();
        geoHistory = new ArrayList<>();
        final String action = intent.getAction();
        Log.d(TAG, String.format("onStartCommand: %s", action));
        Log.e("ACTION", ""+action);
        switch (action) {
            case MihServicePlugin.START_FOREGROUND_ACTION:

                Log.e(TAG, "service started.");
                PackageManager pm = getApplicationContext().getPackageManager();
                Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, 0);

                Bundle bundle = intent.getExtras();
                tripId = bundle.getString("tripId");
                startTimestamp = bundle.getString("startTimestamp");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                            "flutter_foreground_service_channel",
                            NotificationManager.IMPORTANCE_DEFAULT);

                    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                            .createNotificationChannel(channel);
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(getNotificationIcon(bundle.getString("icon")))
                        .setColor(bundle.getInt("color"))
                        .setContentTitle(bundle.getString("title"))
                        .setContentText(bundle.getString("content"))
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setContentIntent(pendingIntent)
                        .setUsesChronometer(bundle.getBoolean("chronometer"))
                        .setOngoing(true);

                if (bundle.getBoolean("stop_action")) {
                    Intent stopSelf = new Intent(this, LocationService.class);
                    stopSelf.setAction(ACTION_STOP_SERVICE);

                    PendingIntent pStopSelf = PendingIntent
                            .getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.addAction(getNotificationIcon(bundle.getString("stop_icon")),
                            bundle.getString("stop_text"),
                            pStopSelf);
                }

                if (bundle.getString("subtext") != null && !bundle.getString("subtext").isEmpty()) {
                    builder.setSubText(bundle.getString("subtext"));
                }

                startForeground(ONGOING_NOTIFICATION_ID, builder.build());
                //setServiceState(this, ServiceState.STARTED);
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "LocationService::lock");
                wakeLock.acquire();
                mTimer = new Timer();
                mTimer.schedule(new TimerTaskToGetLocation(),5,notify_interval);
                break;
            case MihServicePlugin.STOP_FOREGROUND_ACTION:
                Log.e("TAG", "service called stopService");
                stopFlutterForegroundService();
                break;
            case ACTION_STOP_SERVICE:
                stopFlutterForegroundService();
                break;
            default:
                break;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mTimer != null) {mTimer.cancel();}
        if (!userStopForegroundService) {
            Log.d(TAG, "User close app, kill current process to avoid memory leak in other plugin.");
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    /*@Override
    public void onRebind(Intent intent) {
        return mMessenger.getBinder();
    }
*/
    @Override
    public boolean onUnbind(Intent intent) {
        Log.e("unbind","provider eeee");
        messengerListner=null;
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Log.e("status","status changed");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e("provider","provider eeee");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e("provider","provider ccccc");
    }

    private int getNotificationIcon(String iconName) {
        int resourceId = getApplicationContext().getResources().getIdentifier(iconName, "drawable", getApplicationContext().getPackageName());
        return resourceId;
    }

    private void stopFlutterForegroundService() {
        userStopForegroundService = true;
        stopForeground(ONGOING_NOTIFICATION_ID);
        stopSelf();
        Log.e("STOP", "service Stoped");

    }

    private void fn_getlocation(){
        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnable && !isNetworkEnable){

        }else {

            if (isNetworkEnable){
                location = null;
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,2000,0,this);
                if (locationManager!=null){
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (location!=null){

                        Log.e("latitude",location.getLatitude()+"");
                        Log.e("longitude",location.getLongitude()+"");

                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        //fn_update(location);
                    }
                }

            }


            if (isGPSEnable){
                location = null;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,2000,0,this);
                if (locationManager!=null){
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location!=null){
                        Log.e("latitude",location.getLatitude()+"");
                        Log.e("longitude",location.getLongitude()+"");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        if(messengerListner != null)
                        {
                            Bundle stringdata = new Bundle();
                            stringdata.putString("location", latitude+","+longitude);
                            Message msg = Message.obtain(null, MSG_LOCATION_LISTENING);
                            msg.setData(stringdata);
                            try {
                                messengerListner.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        fn_update(location);
                    }
                }
            }


        }

    }

    private void fn_update(Location location){
        Map<String, Object> geoPoint = new HashMap<>();
        geoPoint.put("lt",location.getLatitude());
        geoPoint.put("ln",location.getLongitude());
        geoHistory.add(geoPoint);
        cService.updateTripGeo(tripId,geoHistory,startTimestamp);
    }

    private class TimerTaskToGetLocation extends TimerTask {
        @Override
        public void run() {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    fn_getlocation();
                }
            });

        }
    }

    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOCATION_LISTENING:
                    messengerListner=msg.replyTo;
                    Toast.makeText(getApplicationContext(),"Location listening",Toast.LENGTH_SHORT).show();
                   // mMessenger.send
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
