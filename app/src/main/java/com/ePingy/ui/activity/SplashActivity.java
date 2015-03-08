package com.ePingy.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import com.ePingy.R;

/**
 * Created by jhansi on 06/03/15.
 */
public class SplashActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.layout_splash;
    }

    private void init() {
        findViewById(R.id.splashLayout).postDelayed(new Runnable() {
            @Override
            public void run() {
                startApplication();
            }
        }, 2000);
    }

    protected void startApplication() {
        if (application.isDeviceNameSet()) {
            startDeviceListActivity();
        } else {
            startSetupActivity();
        }
    }

    protected void startDeviceListActivity() {
        Intent deviceListActivity = new Intent(this, DeviceListActivity.class);
        startActivity(deviceListActivity);
    }

    protected void startSetupActivity() {
        Intent setUpActivity = new Intent(this, DeviceSetupActivity.class);
        startActivity(setUpActivity);
    }
}