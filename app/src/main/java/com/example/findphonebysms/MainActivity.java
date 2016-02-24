package com.example.findphonebysms;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    protected static final String TAG = "FIND-PHONE-SMS";

    // Pref
    public static final String keyTextSetting = "keyTextSetting";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLIS = 30 * 1000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLIS =
            UPDATE_INTERVAL_IN_MILLIS / 2;

    BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
                String keyText = getKeyText();
                Log.d(TAG, "key text:" + keyText);

                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    SmsMessage[] messages = new SmsMessage[pdus.length];
                    for (int i = 0; i < pdus.length; i++) {
                        String format = bundle.getString("format");
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                        } else {
                            messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }
                    }
                    for (SmsMessage message : messages) {
                        if (message.getMessageBody().toLowerCase().contains(keyText)) {
                            requestReceived(message.getOriginatingAddress());
                        }
                    }
                }
            }
        }
    };

    @NonNull
    private String getKeyText() {
        String keyTextDefault = getString(R.string.keyTextDefault);
        String keyText = getPrefs().getString(keyTextSetting, keyTextDefault);
        keyText = (keyText == null || keyText.trim().length() == 0) ? keyTextDefault : keyText;
        keyText = keyText.toLowerCase();
        return keyText;
    }

    private void requestReceived(String smsFrom) {
        Location loc = requestBestLocation();
        String text = createLocationMessage(loc);
        Log.d(TAG, "send SMS to:" + smsFrom + ", text:" + text);
        SmsManager.getDefault().sendTextMessage(smsFrom, null, text, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initLocationService();

        // sms receiver
        IntentFilter filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(smsReceiver, filter);

        wireUI();
    }

    private void initLocationService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{""}, 1);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void wireUI() {
        updateUIFromSettings();

        Button saveSettingsButton = (Button) findViewById(R.id.saveSettings);
        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        Button testLocationButton = (Button) findViewById(R.id.testLocation);
        //testLocationButton.setVisibility(View.GONE);
        testLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Location loc = requestBestLocation();
                String text = createLocationMessage(loc);
                Log.d(TAG, "test keyText:" + getKeyText() + ", loc:" + text);

                // Linkify the message
                SpannableString s = new SpannableString(text);
                Linkify.addLinks(s, Linkify.WEB_URLS);

                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.saveSettingDialogTitle)
                        .setPositiveButton("OK", null)
                        .setMessage(s)
                        .create();

                dialog.show();

                // Make the textview clickable. Must be called after show()
                ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            }
        });
    }

    private Location requestBestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    private String createLocationMessage(Location loc) {
        if (loc == null) {
            return getString(R.string.locationNotAvailable);
        }
        return createGmapUrl(loc);
    }

    @NonNull
    private String createGmapUrl(Location loc) {
        return "http://maps.google.com/maps?q=" + loc.getLatitude() + "," + loc.getLongitude();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIFromSettings();
    }

    private void updateUIFromSettings() {
        SharedPreferences prefs = getPrefs();
        // key text
        String keyText = prefs.getString(keyTextSetting, getString(R.string.keyTextDefault));
        ((EditText) findViewById(R.id.keyText)).setText(keyText);
    }

    private void saveSettings() {
        String keyText = ((EditText) findViewById(R.id.keyText)).getText().toString();

        // save to file
        SharedPreferences prefs = getPrefs();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(keyTextSetting, keyText);
        editor.commit();

        Toast.makeText(MainActivity.this, R.string.saveSettingsDialogMessage, Toast.LENGTH_SHORT).show();
    }

    private SharedPreferences getPrefs() {
        String settingsName = getString(R.string.user_settings);
        return getSharedPreferences(settingsName, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsReceiver);
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = requestBestLocation();
        Log.d(TAG, "onConnected loc:" + mLastLocation);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, createLocationRequest(), this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.d(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLIS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLIS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        Log.d(TAG, "fused updated:" + location);
        Toast.makeText(MainActivity.this, "Cur loc: " + location, Toast.LENGTH_SHORT).show();
    }
}
