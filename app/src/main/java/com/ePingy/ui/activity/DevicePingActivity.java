package com.ePingy.ui.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.ePingy.R;
import com.ePingy.app.AppConstants;

/**
 * Created by jhansi on 07/03/15.
 */
public class DevicePingActivity extends BaseActivity {

    private Button coffeeButton;
    private Button lunchButton;
    private Button waitingButton;
    private Button comeToMyDeskButton;
    private Button beerButton;
    private Button shoppingButton;
    private Button movieTonightButton;
    private String targetDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.layout_ping_options;
    }

    private void init() {
        targetDeviceName = getIntent().getStringExtra(AppConstants.DEVICE_NAME_EXTRA);
        if (toolbar != null) {
            toolbar.setTitle(targetDeviceName);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        coffeeButton = (Button) findViewById(R.id.coffee);
        lunchButton = (Button) findViewById(R.id.lunch);
        waitingButton = (Button) findViewById(R.id.waiting);
        comeToMyDeskButton = (Button) findViewById(R.id.comeToMyDesk);

        beerButton = (Button) findViewById(R.id.beer);
        shoppingButton = (Button) findViewById(R.id.shopping);
        movieTonightButton = (Button) findViewById(R.id.movieTonight);

        coffeeButton.setOnClickListener(new PingListener(AppConstants.COFFEE_MSG_ID));
        lunchButton.setOnClickListener(new PingListener(AppConstants.LUNCH_MSG_ID));
        waitingButton.setOnClickListener(new PingListener(AppConstants.WAITING_MSG_ID));
        comeToMyDeskButton.setOnClickListener(new PingListener(AppConstants.COMETOMYDESK_MSG_ID));

        beerButton.setOnClickListener(new PingListener(AppConstants.BEER_MSG_ID));
        shoppingButton.setOnClickListener(new PingListener(AppConstants.SHOPPING_MSG_ID));
        movieTonightButton.setOnClickListener(new PingListener(AppConstants.MOVIE_TONIGHT_MSG_ID));
    }

    private class PingListener implements View.OnClickListener {
        private int messageId;

        PingListener(int messageId) {
            this.messageId = messageId;
        }

        @Override
        public void onClick(View v) {
            String deviceName = application.getDeviceName();
            application.pingDevice(targetDeviceName, messageId, deviceName);
        }
    }
}