/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.leagueiq.app;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.app.DialogFragment;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements PropertyChangeListener{

    private static final String TAG = "MainActivity";
    private static final int SUB_SUCCESSFUL = 101;
    private static final int RC_SIGN_IN = 100;

    AsyncHttpClient client = new AsyncHttpClient();
    private String firebaseBackend = "https://us-central1-neuralleague.cloudfunctions.net/";
    String uid = null;
    boolean validSub = false;


    private JSONObject item_int2string = null;
    private BroadcastReceiver mReceiver = null;
    private ConstraintLayout layout = null;
    private ArrayList<Integer> itemImgs = new ArrayList<>();
    private ArrayList<String> itemNames = new ArrayList<>();

    private boolean itemsInvisible = true;
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView buildText;
    private TextView sortText;
    private TextView hotkeyText;
    private TextView remainingText;
    private ImageView splashImg;
    private View div1;
    private View div2;
    private Button subscribeButton;

    public void signIn() {
        AuthUI.SignInIntentBuilder builder =  AuthUI.getInstance().createSignInIntentBuilder()
                .setTheme(R.style.AppTheme).setIsSmartLockEnabled(false)
                .setLogo(R.mipmap.ic_launcher_foreground)
                .setAvailableProviders(Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                        new AuthUI.IdpConfig.FacebookBuilder().build(),
                        new AuthUI.IdpConfig.TwitterBuilder().build(),

                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build()));

        startActivityForResult(builder.build(), RC_SIGN_IN);
    }


    public void logout() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        // user is now signed out
                        signIn();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            handleSignInResponse(resultCode, data);
        }
        else if (requestCode == SUB_SUCCESSFUL) {
            if(resultCode == Activity.RESULT_OK)
            {
                String result = data.getStringExtra("result");
                if(result.equals("SUBBED"))
                {
                    validSub = true;
                    remainingText.setVisibility(View.GONE);
                    subscribeButton.setVisibility(View.GONE);

                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            signIn();
        }
    }

    private void onSigninSuccess() throws Exception
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
        }
        else
            throw new Exception("Unable to authenticate user");
        fetchAndUploadDeviceID();
        checkSubscriptionStatus();
    }

    private void handleSignInResponse(int resultCode, @Nullable Intent data) {
        IdpResponse response = IdpResponse.fromResultIntent(data);
        Log.d(TAG, "LUL");
        // Successfully signed in
        if (resultCode == RESULT_OK) {
            try {
                onSigninSuccess();
            }
            catch(Exception ex) {
                Log.e(TAG, "Unable to log in user. Continuing in offline mode. lol.");
            }
            Log.d(TAG, "Successful signin: ");
//            startSignedInActivity(response);
//            finish();
        } else {
            Log.e(TAG, "Sign-in error: ", response.getError());
            // Sign in failed
            if (response == null) {
                // User pressed back button
                showSnackbar(R.string.sign_in_cancelled);
//                signIn();
            }

            if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                showSnackbar(R.string.no_internet_connection);
//                signIn();
            }

            showSnackbar(R.string.unknown_error);
            Log.e(TAG, "Sign-in error: ", response.getError());

        }
    }

    private void checkSubscriptionStatus()
    {
        RequestParams params = new RequestParams();
        params.put("customer_id", uid);
        Log.d(TAG, "Attempting to find subs for "+uid);
        client.post(firebaseBackend + "isValid", params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String braintreeUID) {

                Log.d(TAG, "This user has a valid subscription");
                remainingText.setVisibility(View.GONE);
                subscribeButton.setVisibility(View.GONE);
                validSub = true;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                Log.d(TAG, "This user does not have a valid subscription");
                remainingText.setText(String.format(getResources().getString(R.string.remaining_predictions), errorResponse));
            }
        });
    }

    private void checkRemainingPreds(String uid)
    {
        RequestParams params = new RequestParams();
        params.put("customer_id", uid);
        Log.d(TAG, "Attempting to get remaining preds for "+uid);
        client.post(firebaseBackend + "getRemainingPreds", params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String numberLeft) {
                Log.d(TAG, "Number left: "+numberLeft);
                remainingText.setText(String.format(getResources().getString(R.string.remaining_predictions), numberLeft));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String errorResponse, Throwable e) {
                Log.d(TAG, "Couldn't get number left: "+errorResponse);
            }
        });
    }

    private String getUID()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = null;
        if (user != null) {
            uid = user.getUid();
        }
        Log.d(TAG, "This is our UID: " + uid);
        return uid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

                FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            signIn();
        }
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycleView);
        buildText = (TextView) findViewById(R.id.buildText);
//        sortText = (TextView) findViewById(R.id.sortTextView);
        hotkeyText = (TextView) findViewById(R.id.hotkeyTextView);
        remainingText = (TextView) findViewById(R.id.remainingText);
        splashImg = (ImageView) findViewById(R.id.splashImg);
        div1 = (View) findViewById(R.id.divider1);
        div2 = (View) findViewById(R.id.divider2);
        subscribeButton = (Button) findViewById(R.id.subscribeButton);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // specify an adapter (see also next example)
        mAdapter = new MyAdapter(itemImgs, itemNames);
        mRecyclerView.setAdapter(mAdapter);

        layout = (ConstraintLayout)findViewById(R.id.linearLayout);

//        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
//        setSupportActionBar(myToolbar);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }
        try {
            item_int2string = new JSONObject(loadJSONFromAsset());
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            return;
        }

        mReceiver = new BroadcastReceiver() {

            @Override

            public void onReceive(Context context, Intent intent)
            {

                if(!validSub)
                    checkRemainingPreds(getUID());
                String items_str = intent.getStringExtra("items");
                Log.d(TAG, "Received message: " + items_str);
                String[] items_arr = items_str.split(",");
                updateBuildPath(items_arr);
                if(mRecyclerView.getVisibility() != View.VISIBLE)
                {
                    mRecyclerView.setVisibility(View.VISIBLE);
                    buildText.setVisibility(View.VISIBLE);
//                    sortText.setVisibility(View.GONE);
                    hotkeyText.setTextColor(Color.GRAY);
                    buildText.setTextColor(Color.GRAY);
                    div1.setVisibility(View.VISIBLE);
                    div2.setVisibility(View.VISIBLE);
                    splashImg.setVisibility(View.GONE);

                    //Created Constraint Set.
                    ConstraintSet set = new ConstraintSet();
                    set.clone(layout);
                    set.connect(hotkeyText.getId(), ConstraintSet.BOTTOM, subscribeButton.getId(), ConstraintSet.TOP, 16);
                    set.applyTo(layout);

                }
                mAdapter.notifyDataSetChanged();
                runLayoutAnimation();
            }
        };

        IntentFilter intentFilter = new IntentFilter("NEW_ITEMS");
        this.registerReceiver(mReceiver, intentFilter);

        // If a notification message is tapped, any data accompanying the notification
        // message is available in the intent extras. In this sample the launcher
        // intent is fired when the notification is tapped, so any accompanying data would
        // be handled here. If you want a different intent fired, set the click_action
        // field of the notification message to the desired intent. The launcher intent
        // is used when no click_action is specified.
        //
        // Handle possible data accompanying notification message.
        // [START handle_data_extras]
        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }
        // [END handle_data_extras]

//        Button subscribeButton = findViewById(R.id.subscribeButton);
//        subscribeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d(TAG, "Subscribing to news topic");
//                // [START subscribe_topics]
//                FirebaseMessaging.getInstance().subscribeToTopic("news")
//                        .addOnCompleteListener(new OnCompleteListener<Void>() {
//                            @Override
//                            public void onComplete(@NonNull Task<Void> task) {
//                                String msg = getString(R.string.msg_subscribed);
//                                if (!task.isSuccessful()) {
//                                    msg = getString(R.string.msg_subscribe_failed);
//                                }
//                                Log.d(TAG, msg);
//                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                // [END subscribe_topics]
//            }
//        });

//        Button logTokenButton = findViewById(R.id.subscribeButton);
//        logTokenButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Get token
//                FirebaseInstanceId.getInstance().getInstanceId()
//                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
//                            @Override
//                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
//                                if (!task.isSuccessful()) {
//                                    Log.w(TAG, "getInstanceId failed", task.getException());
//                                    return;
//                                }
//
//                                // Get new Instance ID token
//                                String token = task.getResult().getToken();
//
//                                // Log and toast
//                                String msg = getString(R.string.msg_token_fmt, token);
//                                Log.d(TAG, msg);
//                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
//                                logout();
//                            }
//                        });
//
//
//            }
//        });

        Button logTokenButton = findViewById(R.id.subscribeButton);
        logTokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, SubscribeActivity.class);
                startActivityForResult(intent, SUB_SUCCESSFUL);
            }
        });


    }

    private String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = this.getAssets().open("item_int2string.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private void fetchAndUploadDeviceID()
    {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();
                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d("fetchAndUploadDeviceID", msg);
                        FirestoreOps.uploadDeviceID(token, uid);


                    }
                });
    }



    private void updateBuildPath(String[] itemIds)
    {
        ArrayList<Integer> itemImgs = new ArrayList<>();
        ArrayList<String> itemNames = new ArrayList<>();

        for(String itemStr : itemIds)
        {
            Integer item_int = Integer.parseInt(itemStr);
            String item_filename = "item_"+item_int;

//            Log.d(TAG, "item_int: "+item_int+" filename: "+item_filename+" resID: "+resID);
            try {
                itemNames.add((String) item_int2string.get(itemStr));
                itemImgs.add(getResources().getIdentifier(item_filename, "drawable", getPackageName()));
            }
            catch (JSONException ex) {
                ex.printStackTrace();
                return;
            }

        }
        this.itemImgs.clear();
        this.itemNames.clear();
        this.itemImgs.addAll(itemImgs);
        this.itemNames.addAll(itemNames);


    }

    private void runLayoutAnimation() {
        final Context context = mRecyclerView.getContext();
        final LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom);

        mRecyclerView.setLayoutAnimation(controller);
        mRecyclerView.scheduleLayoutAnimation();
    }


    public void onDestroy() {

        unregisterReceiver(mReceiver);

        super.onDestroy();

    }

    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(layout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.unsub:
                PropertyChangeEvent confirmEvent = new PropertyChangeEvent(this, "confirm_unsub", null, null);
                new ConfirmDialog(getString(R.string.confirm_unsub), confirmEvent, this, this)
                .show(getSupportFragmentManager(), "confirmCancel");
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.logout:
                logout();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    public void propertyChange​(PropertyChangeEvent evt)
    {
        String event = (String) evt.getPropertyName();
        if(event.equals("confirmCancel"))
        {
            Log.d(TAG, "User wants to cancel sub");
            unsubscribe();
        }
        else if(event.equals("confirmLogout"))
        {
            Log.d(TAG, "User wants to logout");

        }
    }

    private void unsubscribe()
    {
        validSub = false;
        subscribeButton.setVisibility(View.VISIBLE);
        remainingText.setVisibility(View.VISIBLE);
        checkRemainingPreds(uid);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow, menu);

        return super.onCreateOptionsMenu(menu);
    }


}
