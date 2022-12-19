package com.example.plant_iot_tablet;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ForcedTerminationService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.e("Error", "onTaskRemoved - 강제 종료" + rootIntent);
        Intent intent = new Intent(getApplicationContext(), MyService.class);
        startService(intent);
        stopSelf();
    }
}
