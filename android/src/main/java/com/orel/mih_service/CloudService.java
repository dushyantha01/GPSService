package com.orel.mih_service;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudService {
    public FirebaseFirestore _db;
    private boolean status=false;

    public CloudService(){
        _db = FirebaseFirestore.getInstance();
    }

    public boolean updateTripGeo(String docId, List<Map<String, Object>> geoHistory,String startTimeStamp,double distance)  {

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Log.e("CS-doc", docId);
        Map<String, Object> trip = new HashMap<>();
        Map<String, Object> tripDetails = new HashMap<>();
        Map<String, Object> tripTimestamps = new HashMap<>();
        trip.put("isTripEnd", true);

        tripDetails.put("amountDetails","");
        tripDetails.put("bgService","");
        tripDetails.put("tripGeoHistory",geoHistory);
        //tripDetails.put("endGeo","");
        tripDetails.put("totalPrice","");
        tripDetails.put("totalDistance",distance);

        try {
            tripTimestamps.put("tripStartTimestamp",new Timestamp(dateFormat.parse(startTimeStamp)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        tripTimestamps.put("tripEndTimestamp", new Timestamp(new Date()));
        //dateFormat.format(new Date())
        trip.put("tripDetails", tripDetails);
        trip.put("inActive", "");
        trip.put("timestamp",tripTimestamps);

       return updateSelfTrip(docId,trip);
    };

    public boolean updateSelfTrip(String docId, Map<String, Object> map) {

        _db.collection("trips").document(docId).update(map)
                .addOnSuccessListener(new OnSuccessListener < Void > () {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("CS", "Location Updated");
                        status= true;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e)
                    {
                        //Toast error using method -> e.getMessage()
                        status= false;
                    }
                });
        return status;
        //return ref.set(map, SetOptions(merge: true));
    }
    }


