package com.mcrepeau.ventracheck;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class UpdateCardInfoService extends Service {

    private static final String TAG = "UpdateCardInfoService";

    public UpdateCardInfoService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
