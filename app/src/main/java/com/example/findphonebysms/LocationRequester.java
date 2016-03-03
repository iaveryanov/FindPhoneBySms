package com.example.findphonebysms;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Timer;
import java.util.TimerTask;

public class LocationRequester implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final Context context;
    private final String address;

    private GoogleApiClient mGoogleApiClient;

    private Timer timerWaitUpdates;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLIS = 5000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLIS = 5000;

    public static final int TIMER_WAIT_UPDATES_MILLIS = 15000;

    public LocationRequester(Context context, String address) {
        this.context = context;
        this.address = address;
    }

    public void request() {
        timerWaitUpdates = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Location location = requestLastLocation();
                onReceiveLocation(location);
                stopLocationService();
            }
        };
        timerWaitUpdates.schedule(task, TIMER_WAIT_UPDATES_MILLIS);
        startLocationService();
    }

    private void stopLocationService() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    private void startLocationService() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (!checkPermission()) return;
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private Location requestLastLocation() {
        if (!checkPermission()) return null;
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLIS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLIS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(MainActivity.TAG, "Connection suspended");
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(MainActivity.TAG, "onConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(MainActivity.TAG, "location updated:" + location);
        if (location != null) {
            stopTimer();
            stopLocationService();
            onReceiveLocation(location);
        }
    }

    private void stopTimer() {
        timerWaitUpdates.cancel();
        timerWaitUpdates = null;
    }

    private void onReceiveLocation(final Location location) {
        final String text = createLocationMessage(location);
        if (address != null) {
            Log.d(MainActivity.TAG, "send SMS to:" + address + ", text:" + text);
            SmsManager.getDefault().sendTextMessage(address, null, text, null, null);
        } else {
            // for test-location button
            Log.d(MainActivity.TAG, text);
            showDialog(text);

        }
    }

    private void showDialog(final String text) {
        // running code in the UI thread
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                // Linkify the message
                SpannableString ss = new SpannableString(text);
                Linkify.addLinks(ss, Linkify.WEB_URLS);

                AlertDialog dialog = new AlertDialog.Builder(context)
                        .setTitle(R.string.saveSettingDialogTitle)
                        .setPositiveButton("OK", null)
                        .setMessage(ss)
                        .create();

                dialog.show();

                // Make the textview clickable. Must be called after show()
                ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }
        });
    }

    private String createLocationMessage(Location loc) {
        if (loc == null) {
            return context.getString(R.string.locationNotAvailable);
        }
        return createGmapUrl(loc);
    }

    @NonNull
    private String createGmapUrl(Location loc) {
        return "http://maps.google.com/maps?q=" + loc.getLatitude() + "," + loc.getLongitude();
    }
}
