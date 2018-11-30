package com.leagueiq.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ConfirmDialog extends DialogFragment
{
    String message;
    PropertyChangeEvent confirmEvent;
    PropertyChangeListener listener;
    AppCompatActivity activity;

    ConfirmDialog(String message, PropertyChangeEvent confirmEvent, PropertyChangeListener listener, AppCompatActivity activity)
    {
        this.message = message;
        this.confirmEvent = confirmEvent;
        this.listener = listener;
        this.activity = activity;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        listener.propertyChange(confirmEvent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}