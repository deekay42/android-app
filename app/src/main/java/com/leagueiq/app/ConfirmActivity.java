package com.leagueiq.app;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ConfirmActivity extends AppCompatActivity implements PropertyChangeListener {

    AsyncHttpClient client = new AsyncHttpClient();
    private String firebaseBackend = "https://us-central1-neuralleague.cloudfunctions.net/";
    private final static String TAG = "Checkout";
    private String nonce = null;
    private TextView payDesc = null;
    private ImageView payImg = null;

    private String braintreeUID = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        payDesc = findViewById(R.id.paymentMethodTxt);
        payImg = findViewById(R.id.paymentMethodImg);

        Intent intent = getIntent();
        braintreeUID = intent.getStringExtra("braintreeUID");
        nonce = intent.getStringExtra("nonce");
        payDesc.setText(intent.getStringExtra("desc"));
        payImg.setImageResource(Integer.parseInt(intent.getStringExtra("icon")));
    }

    public void propertyChange​(PropertyChangeEvent evt)
    {
        String event = (String)evt.getPropertyName();
        if(event.equals("braintreeUIDSuccess"))
        {
            Log.d(TAG, "Successfully uploaded customer id");
            checkout();
        }
        else if(event.equals("braintreeUIDError"))
        {
            Log.d(TAG, "Error obtaining customer id");
        }
    }

    private void createBraintreeUserThenCheckout()
    {
        RequestParams params = new RequestParams();
        params.put("payment_method_nonce", nonce);
        Log.d(TAG, "Attempting to create new braintree user");
        client.post(firebaseBackend + "createUserWithNonce", params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String braintreeUID) {
                ConfirmActivity.this.braintreeUID = braintreeUID;
                Log.d(TAG, "Create UID That worked!");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String firebaseUID = null;
                if (user != null) {
                    firebaseUID = user.getUid();
                }
                FirestoreOps.uploadBraintreeUID(braintreeUID, firebaseUID, ConfirmActivity.this);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                Log.e(TAG, "Create UID There was an error");
                Log.e(TAG, errorResponse);
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                Toast.makeText(ConfirmActivity.this, "An error occurred during checkout", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onSubscribeButtonClick(View v) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        if(braintreeUID == null) {
            Log.d(TAG, "There is no braintree UID ");
            createBraintreeUserThenCheckout();
        }
        else
        {
            Log.d(TAG, "There is a braintree UID ");
            checkout();
        }
    }

    public void onChangeClick(View v) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result","CHANGING");
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

//    private void checkout()
//    {
//        RequestParams params = new RequestParams();
//        params.put("customer_id", braintreeUID);
//        Log.d(TAG, "Attempting checkout now with braintreeUID: "+braintreeUID);
//        client.post(firebaseBackend + "subscribe", params, new TextHttpResponseHandler() {
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, String clientToken) {
//                Log.d(TAG, "CHECKOUT That worked!");
//                Intent returnIntent = new Intent();
//                returnIntent.putExtra("result", "CONFIRMED");
//                setResult(Activity.RESULT_OK, returnIntent);
//                finish();
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
//                Log.e(TAG, "CHECKOUT There was an error");
//                Log.e(TAG, errorResponse);
//                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
//                Toast.makeText(ConfirmActivity.this, "An error occurred during checkout", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }

    private void checkout()
    {
        CallFirebaseFunctions.call("client_token", new FirebaseFunctionListener() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "CHECKOUT That worked!");
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result", "CONFIRMED");
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }

            @Override
            public void onFailure(String result) {
                Log.e(TAG, "CHECKOUT There was an error");
                Log.e(TAG, result);
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                Toast.makeText(ConfirmActivity.this, "An error occurred during checkout", Toast.LENGTH_SHORT).show();
            }
        });

        RequestParams params = new RequestParams();
        params.put("customer_id", braintreeUID);
        Log.d(TAG, "Attempting checkout now with braintreeUID: "+braintreeUID);
        client.post(firebaseBackend + "subscribe", params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String clientToken) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {

            }
        });
    }
}
