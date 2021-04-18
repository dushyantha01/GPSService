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

import com.google.type.LatLng;
import java.text.DecimalFormat;
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
    long notify_interval = 4000;
    public static String str_receiver = "servicetutorial.service.receiver";
    Intent intent;
    private double totalDistance = 0;
    // end //

    // messenger service used for communicate with dart -start//
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static final String HELLO = "hello!";
    public static final int MSG_SAY_HELLO = 1;
    public static final int MSG_LOCATION_LISTENING = 101;
    public static final int MSG_DISTENCE_LISTENING = 102;
    private Messenger messengerListner = null;

    private List<Map<String, Object>> geoHistory;
    // end //

    // firestore functions -start
    private CloudService cService;
    private int dataOfflineSyncCount = 0;
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
                mTimer.schedule(new TimerTaskToGetLocation(),4,notify_interval);
                break;
            case MihServicePlugin.STOP_FOREGROUND_ACTION:
                Log.e("TAG", "service called stopService");
                //if(dataOfflineSyncCount>1){
                  //  Toast.makeText(getApplicationContext(),"Please Check th e internet connection",Toast.LENGTH_SHORT).show();
                    //}else {
                    stopFlutterForegroundService();
                //}
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

                        //Log.e("latitude",location.getLatitude()+"");
                        //Log.e("longitude",location.getLongitude()+"");

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
                        //Log.e("latitude",location.getLatitude()+"");
                        //Log.e("longitude",location.getLongitude()+"");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        if(messengerListner != null)
                        {
                            Bundle stringdata = new Bundle();
                            stringdata.putString("location", latitude+","+longitude);
                            Message msg = Message.obtain(null, MSG_LOCATION_LISTENING);
                            msg.setData(stringdata);
                            //try {
                                //messengerListner.send(msg);
                            //} catch (RemoteException e) {
                            //    e.printStackTrace();
                            //}
                        }
                        fn_update(location);
                    }
                }
            }


        }

    }

    private void fn_update(Location location){
        //dataSync=false;
        Map<String, Object> geoPoint = new HashMap<>();
        geoPoint.put("lt",location.getLatitude());
        geoPoint.put("ln",location.getLongitude());
        geoHistory.add(geoPoint);
        if(geoHistory.size()>1){
            //CalculationByDistance(geoHistory.get(geoHistory.size()-2),geoHistory.get(geoHistory.size()-1));
            meterDistanceBetweenPoints( ((Double)geoHistory.get(geoHistory.size()-2).get("lt")).floatValue(),
                    ((Double)geoHistory.get(geoHistory.size()-2).get("ln")).floatValue(),
                    ((Double)geoHistory.get(geoHistory.size()-1).get("lt")).floatValue(),
                    ((Double)geoHistory.get(geoHistory.size()-1).get("ln")).floatValue());

        }
        boolean dataSync=cService.updateTripGeo(tripId,geoHistory,startTimestamp,totalDistance);

        if(dataSync){
            dataOfflineSyncCount=0;
        }else{
            dataOfflineSyncCount+=1;
        }
    }

    public double CalculationByDistance(Map<String, Object> StartP, Map<String, Object> EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = (Double)StartP.get("lt");
        Log.i("Radius Value", "st lat1 = " + lat1);
        double lat2 = (Double)EndP.get("lt");
        Log.i("Radius Value", "end lat2 = " + lat2);
        double lon1 = (Double)StartP.get("ln");
        Log.i("Radius Value", "st ln1 = " + lon1);
        double lon2 = (Double)EndP.get("ln");
        Log.i("Radius Value", "end ln2 = " + lon2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        Log.i("Radius Value", "a = " + a);
        double c = 2 * Math.asin(Math.sqrt(a));
        Log.i("Radius Value", "c = " + c);
        double valueResult = Radius * c;
        double km = valueResult / 1;
        Log.i("Radius Value", "km = " + km);
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        Log.i("Radius Value", "kmD = " + kmInDec);
        double meter = valueResult % 1000;
        totalDistance=totalDistance+meter;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);
        if(messengerListner != null)
        {
            Bundle stringdata = new Bundle();
            stringdata.putString("distance", "   KM  " + kmInDec+ " Meter   " + meterInDec);
            stringdata.putString("total_distance", " Meter   " + Integer.valueOf(newFormat.format(totalDistance)));
            Message msg = Message.obtain(null, MSG_DISTENCE_LISTENING);
            msg.setData(stringdata);
            try {
                messengerListner.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return Radius * c;
    }

    private void meterDistanceBetweenPoints(float lat_a, float lng_a, float lat_b, float lng_b) {
        float pk = (float) (180.f/Math.PI);

        float a1 = lat_a / pk;
        float a2 = lng_a / pk;
        float b1 = lat_b / pk;
        float b2 = lng_b / pk;

        double t1 = Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math.cos(b2);
        double t2 = Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math.sin(b2);
        double t3 = Math.sin(a1) * Math.sin(b1);
        double tt = Math.acos(t1 + t2 + t3);
        Log.i("Radius Value", "meters = " + (6366000 * tt));
        if((6366000 * tt)>1){
        totalDistance=totalDistance+(6366000 * tt);}
        if(messengerListner != null)
        {
            Bundle stringdata = new Bundle();
            //stringdata.putString("distance", "   KM  " + kmInDec+ " Meter   " + meterInDec);
            stringdata.putString("total_distance", " Meter   " + totalDistance);
            Message msg = Message.obtain(null, MSG_DISTENCE_LISTENING);
            msg.setData(stringdata);
            try {
                messengerListner.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        //return 6366000 * tt;
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
