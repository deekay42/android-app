package com.leagueiq.app;

public interface BraintreeIDListener {
    void paymentIDObtained(String id);
    void paymentIDNonexistent();
}

public interface FirebaseFunctionListener {
    void onSuccess();
    void onFailure();
}

