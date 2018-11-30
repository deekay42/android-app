package com.leagueiq.app;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.dropin.utils.PaymentMethodType;
import com.braintreepayments.api.models.GooglePaymentRequest;
import com.braintreepayments.api.models.PayPalRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.*;

import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;


public class SubscribeActivity extends AppCompatActivity {

    private final static String TAG = "SUBSCRIBEACTIVITY";
    private final static int REQUEST_CODE = 42;
    private final static int CONFIRM_REQUEST = 43;


    private TextView features;
    private TextView fineprint;
    private Button sub;

    private String clientToken = null;
    private String firebaseBackend = "https://us-central1-neuralleague.cloudfunctions.net/";
    AsyncHttpClient client = new AsyncHttpClient();
    String nonce = null;
    String icon;
    String desc;
    String braintreeUID = null;

    boolean subButtonClicked = false;
    FirebaseUser user = null;


//    @Override
//    public void paymentIDObtained(String braintreeUID)
//    {
//        this.braintreeUID = braintreeUID;
//        getCustomerPaymentToken(braintreeUID);
//    }
//
//    public void paymentIDNonexistent()
//    {
//        getCustomerPaymentToken(null);
//    }



//    private void getCustomerPaymentToken(String braintreeUID)
//    {
//        Log.d(TAG, "Trying to obtain client token now");
//
//        RequestParams params = new RequestParams();
//        params.put("client_id", braintreeUID);
//        Log.d(TAG, "Posting request now");
//        client.post(firebaseBackend + "client_token", params, new TextHttpResponseHandler() {
//            @Override
//            public void onSuccess(int statusCode, Header[] headers, String clientToken) {
//                Log.d(TAG, "Obtained client token!!");
//                Log.d(TAG, "This is it: "+clientToken);
//                SubscribeActivity.this.clientToken = clientToken;
//                if(subButtonClicked)
//                {
//                    Log.d(TAG, "Sub button already clicked");
//                    subButtonClicked = false;
//                    startPaymentSelection();
//                }
//            }
//
//            @Override
//            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
//                Log.e(TAG, "client token  There was an error");
//                Log.e(TAG, errorResponse);
//            }
//        });
//
//    }

    private void getCustomerPaymentToken()
    {
        CallFirebaseFunctions.call("client_token", new FirebaseFunctionListener() {
            @Override
            public void onSuccess(String result) {
                clientToken = result;
                Log.d(TAG, "Obtained client token!!");
                Log.d(TAG, "This is it: "+clientToken);
                SubscribeActivity.this.clientToken = clientToken;
                if(subButtonClicked)
                {
                    Log.d(TAG, "Sub button already clicked");
                    subButtonClicked = false;
                    startPaymentSelection();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscribe);
        features = findViewById(R.id.features);
        fineprint = findViewById(R.id.fineprint);
        sub = findViewById(R.id.sub);

        user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = null;
        if (user != null) {
            uid = user.getUid();
        }

//        FirestoreOps.getBraintreeUID(uid, this);

        getCustomerPaymentToken();

        sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Subscribing!!");

                onPurchaseButtonClicked(v);
//                Intent returnIntent = new Intent();
//                returnIntent.putExtra("result","SUBBED");
//                setResult(Activity.RESULT_OK,returnIntent);
//                finish();

            }
        });
    }

    private void startPaymentSelection()
    {
        DropInRequest dropInRequest = new DropInRequest()
                .clientToken(clientToken)
//                .amount("1.00")
//                .requestThreeDSecureVerification(true)
                .collectDeviceData(true);
        PayPalRequest paypalRequest = new PayPalRequest();
        paypalRequest.userAction("BUYYYY");
        dropInRequest.paypalRequest(paypalRequest);
        enableGooglePay(dropInRequest);

        startActivityForResult(dropInRequest.getIntent(SubscribeActivity.this), REQUEST_CODE);
    }

    private void startConfirmActivity()
    {
        Intent intent = new Intent(this, ConfirmActivity.class);
        intent.putExtra("nonce", nonce);
        intent.putExtra("desc", desc);
        intent.putExtra("icon", icon);
        intent.putExtra("braintreeUID", braintreeUID);
        startActivityForResult(intent, CONFIRM_REQUEST);
    }

    public void onPurchaseButtonClicked(final View arg0) {
        Log.d(TAG, "Purchase button clicked.");

        if(clientToken != null)
            if(nonce != null)
                startConfirmActivity();
            else
                startPaymentSelection();
        else
            subButtonClicked = true;

//        DropInResult.fetchDropInResult(this, clientToken, new DropInResult.DropInResultListener() {
//            @Override
//            public void onError(Exception exception) {
//                Log.e(TAG, "There was an error obtaining existing payment info");
//                Log.e(TAG, exception.getMessage());
//            }
//
//            @Override
//            public void onResult(DropInResult result) {
//                Log.d(TAG, "Existing Purchase method result");
//                if (result.getPaymentMethodType() != null) {
//                    // use the icon and name to show in your UI
//                    int icon = result.getPaymentMethodType().getDrawable();
//                    int name = result.getPaymentMethodType().getLocalizedName();
//
//                    if (result.getPaymentMethodType() == PaymentMethodType.ANDROID_PAY) {
//                        Log.d(TAG, "Last one was android pay");
//                        // The last payment method the user used was Android Pay. The Android Pay
//                        // flow will need to be performed by the user again at the time of checkout
//                        // using AndroidPay#requestAndroidPay(...). No PaymentMethodNonce will be
//                        // present in result.getPaymentMethodNonce(), this is only an indication that
//                        // the user last used Android Pay. Note: if you have enabled preauthorization
//                        // and the user checked the "Use selected info for future purchases from this app"
//                        // last time Android Pay was used you may need to call
//                        // AndroidPay#requestAndroidPay(...) and then offer the user the option to change
//                        // the default payment method using AndroidPay#changePaymentMethod(...)
//                    } else {
//                        Log.d(TAG, "Last one was somethine else");
//                        // show the payment method in your UI and charge the user at the
//                        // time of checkout using the nonce: paymentMethod.getNonce()
//                        PaymentMethodNonce paymentMethod = result.getPaymentMethodNonce();
//                    }
//                } else {
//                    Log.d(TAG, "No existing payment method found.");
//
//
//                }
//            }
//        });


    }



    private void enableGooglePay(DropInRequest dropInRequest) {
        GooglePaymentRequest googlePaymentRequest = new GooglePaymentRequest()
                .transactionInfo(TransactionInfo.newBuilder()
                        .setTotalPrice("1.00")
                        .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                        .setCurrencyCode("USD")
                        .build())
                .billingAddressRequired(true); // We recommend collecting and passing billing address information with all Google Pay transactions as a best practice.


        dropInRequest.googlePaymentRequest(googlePaymentRequest);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Activity finished");
        if (requestCode == REQUEST_CODE) {
            Log.d(TAG, "It was the payment activity");
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Everything worked!");
                // use the result to update your UI and send the payment method nonce to your server
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                String deviceData = result.getDeviceData();

                if (result.getPaymentMethodType() == PaymentMethodType.PAY_WITH_VENMO) {
                    VenmoAccountNonce venmoAccountNonce = (VenmoAccountNonce) result.getPaymentMethodNonce();
                    String venmoUsername = venmoAccountNonce.getUsername();
                }


                nonce = result.getPaymentMethodNonce().getNonce();
                icon = Integer.toString(result.getPaymentMethodType().getDrawable());
                desc = result.getPaymentMethodNonce().getDescription();

                startConfirmActivity();

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "It was canceled");
                // the user canceled
            } else {
                Log.e(TAG, "There was an error");
                // Handle errors here, an exception may be available in
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.e(TAG, error.toString());
            }
        }
        else if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                String message = data.getStringExtra("result");

                if(message.equals("CONFIRMED")) {
                    Log.d(TAG, "CONFIRM REQUEST Everything worked!");
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", "SUBBED");
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
                else if(message.equals("CHANGING"))
                {
                    startPaymentSelection();
                }

            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.d(TAG, "confirmation It was canceled");
                // the user canceled
            } else {
                Log.e(TAG, "confirmation There was an error");
                // Handle errors here, an exception may be available in
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
                Log.e(TAG, error.toString());
            }



        }
    }





//    protected void onResume() {
//        super.onResume();
//        // Note: We query purchases in onResume() to handle purchases completed while the activity
//        // is inactive. For example, this can happen if the activity is destroyed during the
//        // purchase flow. This ensures that when the activity is resumed it reflects the user's
//        // current purchases.
//        if (mBillingManager != null
//                && mBillingManager.getBillingClientResponseCode() == BillingResponse.OK) {
//            mBillingManager.queryPurchases();
//        }
//    }


}
