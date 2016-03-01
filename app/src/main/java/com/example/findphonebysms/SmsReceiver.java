package com.example.findphonebysms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String keyText = getKeyText(context);
        Log.d(MainActivity.TAG, "key text:" + keyText);

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
                String num = message.getOriginatingAddress();
                // not alpha numeric and with key text
                if (num.matches("\\+?[0-9]+")
                    && message.getMessageBody().toLowerCase().contains(keyText)) {
                    Log.d(MainActivity.TAG, "key text:" + keyText + ", identified!");
                    requestReceived(context, num);
                }
            }
        }
    }

    private void requestReceived(Context context, String address) {
        new LocationRequester(context, address).request();
    }

    @NonNull
    private String getKeyText(Context context) {
        String keyTextDefault = context.getString(R.string.keyTextDefault);
        String settingsName = context.getString(R.string.user_settings);
        String keyText = context.getSharedPreferences(settingsName, 0).getString(MainActivity.keyTextSetting, keyTextDefault);
        keyText = (keyText == null || keyText.trim().length() == 0) ? keyTextDefault : keyText;
        keyText = keyText.toLowerCase();
        return keyText;
    }
}
