package com.orel.mih_service;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.ArrayList;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudService {
    public FirebaseFirestore _db;

    public CloudService(){
        _db = FirebaseFirestore.getInstance();
    }

    public void updateTripGeo(String docId, List<Map<String, Object>> geoHistory,String startTimeStamp){

        Log.e("CS-doc", docId);
        Map<String, Object> trip = new HashMap<>();
        Map<String, Object> tripDetails = new HashMap<>();
        Map<String, Object> timeStamps = new HashMap<>();
        trip.put("isTripEnd", true);

        tripDetails.put("amountDetails",0);
        tripDetails.put("bgService","");
        tripDetails.put("tripGeoHistory",geoHistory);
        tripDetails.put("endGeo",0);
        tripDetails.put("totalPrice",0);
        tripDetails.put("totalDistance",0);

        timeStamps.put("tripStartTimestamp",startTimeStamp);
        timeStamps.put("tripEndTimestamp",new Timestamp(new java.util.Date().getTime()));

        trip.put("tripDetails", tripDetails);
        trip.put("inActive", "");
        trip.put("timestamp",timeStamps);

        updateSelfTrip(docId,trip);
    };

    public void updateSelfTrip(String docId, Map<String, Object> map) {
        _db.collection("trips").document(docId).update(map)
                .addOnSuccessListener(new OnSuccessListener < Void > () {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("CS", "Location Updated");
                    }
                });
        //return ref.set(map, SetOptions(merge: true));
    }
    }


