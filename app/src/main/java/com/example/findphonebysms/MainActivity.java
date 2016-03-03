package com.example.findphonebysms;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {


    protected static final String TAG = "FIND-PHONE-SMS";

    // Pref
    public static final String keyTextSetting = "keyTextSetting";
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 10;

    @NonNull
    private String getKeyText() {
        String keyTextDefault = getString(R.string.keyTextDefault);
        String keyText = getPrefs().getString(keyTextSetting, keyTextDefault);
        keyText = (keyText == null || keyText.trim().length() == 0) ? keyTextDefault : keyText;
        keyText = keyText.toLowerCase();
        return keyText;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wireUI();
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
                new LocationRequester(MainActivity.this, null).request();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIFromSettings();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "requestCode:" + requestCode
                + ", permissions:" + Arrays.toString(permissions)
                + ", grantResults:" + Arrays.toString(grantResults));
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
}
