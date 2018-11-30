package com.leagueiq.app;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.HashMap;
import java.util.Map;

public final class CallFirebaseFunctions {

    private static final String TAG = 'firebase functions';

    private static Task<String> call(Map<String, Object> data, String function)
    {
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();
        return mFunctions
                .getHttpsCallable(function)
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                        // This continuation runs on either success or failure, but if the task
                        // has failed then getResult() will throw an Exception which will be
                        // propagated down.
                        String result = (String) task.getResult().getData();
                        return result;
                    }
                });
    }


    public static void call(String function, FirebaseFunctionListener callback)
    {
        call(function, null, null, callback);
    }

    public static void call(String function, String name, String val, final FirebaseFunctionListener callback)
    {
        Map<String, Object> args = new HashMap<>();
        args.put(name, val);
        call(args, function).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    if (e instanceof FirebaseFunctionsException) {
                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                        FirebaseFunctionsException.Code code = ffe.getCode();
                        Object details = ffe.getDetails();
                        Log.e(TAG, "did not obtain client token  There was an error");
                        Log.e(TAG, ffe.toString());
                        callback.onFailure(ffe.toString() + ffe.getMessage());
                    }
                    else
                    {
                        callback.onFailure(e.getMessage());
                        Log.e(TAG, e.getMessage());
                    }
                }
                else
                {
                    callback.onSuccess(task.getResult());
                }

                // ...
            }
        });
    }

}
