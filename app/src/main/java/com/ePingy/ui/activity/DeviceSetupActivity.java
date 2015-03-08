package com.ePingy.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.ePingy.R;

/**
 * Created by jhansi on 06/03/15.
 */
public class DeviceSetupActivity extends BaseActivity {

    private EditText deviceNameEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.layout_device_setup;
    }

    private void init() {
        deviceNameEditText = (EditText) findViewById(R.id.deviceName);
        deviceNameEditText.setOnEditorActionListener(new DeviceNameOnEditActionListener());
    }

    private class DeviceNameOnEditActionListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                String deviceName = v.getText().toString();
                application.setDeviceName(deviceName);
                startDeviceListActivity();
                return true;
            }
            return false;
        }
    }

    private void startDeviceListActivity() {
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivity(intent);
    }

}