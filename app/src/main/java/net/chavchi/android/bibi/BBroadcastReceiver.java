package net.chavchi.android.bibi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            ((Bibi)context.getApplicationContext()).onMediaButtonPress(intent);
        }
    }
}