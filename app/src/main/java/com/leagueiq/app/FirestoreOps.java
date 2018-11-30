package com.leagueiq.app;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

public final class FirestoreOps {

    private static final String TAG = "FirestoreOps";

    private static FirebaseFirestore db = null;

    static
    {
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);
    }

//    public static void uploadBraintreeUID(String braintreeUID, String userID, final PropertyChangeListener listener)
//    {
//
//        Map<String, String> payment_id = new HashMap<>();
//        payment_id.put("braintreeUID", braintreeUID);
//        Log.d(TAG, "Attempting to upload braintree uid to firestore: "+braintreeUID);
//        db.collection("users").document(userID)
//                .set(payment_id)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.d(TAG, "Payment ID  successfully added to firestore!");
//                        PropertyChangeEvent notify = new PropertyChangeEvent(this, "braintreeUIDSuccess", null, null);
//                        listener.propertyChange(notify);
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e(TAG, "Error Payment ID NOT successfully added to firestore", e);
//                        PropertyChangeEvent notify = new PropertyChangeEvent(this, "braintreeUIDError", null, null);
//                        listener.propertyChange(notify);
//                    }
//                });
//    }
//
//    public static void getBraintreeUID(String userID, final BraintreeIDListener pil)
//    {
//        Log.d(TAG, "Attempting getting braintree uid from firestore");
//        DocumentReference docRef = db.collection("users").document(userID);
//        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if (task.isSuccessful()) {
//                    DocumentSnapshot document = task.getResult();
//                    if (document.exists()) {
//                        Map<String, Object> data = document.getData();
//                        Object braintreeUID = data.get("braintreeUID");
//                        if(braintreeUID != null)
//                        {
//                            Log.d(TAG, "Found braintree UID: " + braintreeUID);
//                            pil.paymentIDObtained(braintreeUID.toString());
//                        }
//                        else {
//                            pil.paymentIDNonexistent();
//                            Log.d(TAG, "Didnt find braintree UID");
//                        }
//
//                    } else {
//                        pil.paymentIDNonexistent();
//                        Log.d(TAG, "Didnt find braintree UID");
//                    }
//                } else {
//                    Log.d(TAG, "get failed with ", task.getException());
//                    pil.paymentIDNonexistent();
//                }
//            }
//        });
//    }

    public static void uploadDeviceID(String deviceID, String uid)
    {

        Map<String, String> device_id = new HashMap<>();
        device_id.put("device_id", deviceID);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        db.collection("users").document(uid)
                .set(device_id)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });
    }
}
