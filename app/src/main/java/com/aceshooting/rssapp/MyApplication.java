package com.aceshooting.rssapp;

/**
 * Created by Ace Shooting on 7/4/2014.
 */

import android.app.Application;
import android.content.Intent;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "", // will not be used
        mailTo = "info@aceshooting.com",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        super.onCreate();
        ACRA.init(this);
        Intent intent = new Intent(this, MainTabActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}