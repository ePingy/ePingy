package com.ePingy.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ePingy.DevicesStateChangedListener;
import com.ePingy.R;
import com.ePingy.app.AppConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jhansi on 06/03/15.
 */
public class DeviceListActivity extends BaseActivity implements DevicesStateChangedListener {

    private ListView deviceListView;
    private ArrayAdapter<String> deviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.layout_device_list;
    }

    private void init() {
        application.setDevicesStateChangedListener(this);
        application.joinChannel(application.getDeviceName());
        deviceListView = (ListView) findViewById(android.R.id.list);
        TextView emptyTextView = (TextView) findViewById(android.R.id.empty);
        deviceListView.setEmptyView(emptyTextView);
        bindData();
        bindListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                reloadDevices();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void reloadDevices() {
        List<String> devices = getDevices();
        deviceListAdapter.clear();
        deviceListAdapter.addAll(devices);
        deviceListAdapter.notifyDataSetChanged();
    }

    private void bindData() {
        List<String> devices = getDevices();
        deviceListAdapter = new ArrayAdapter<String>(this, R.layout.simple_list_item, devices);
        deviceListView.setAdapter(deviceListAdapter);
    }

    private void bindListeners() {
        deviceListView.setOnItemClickListener(new DeviceItemClickListener());
    }

    private List<String> getDevices() {
        List<String> channels = application.getDevices();
        List<String> devices = new ArrayList<String>();
        for (String channel : channels) {
            int lastDot = channel.lastIndexOf('.');
            if (lastDot < 0) {
                continue;
            }
            devices.add(channel.substring(lastDot + 1));
        }
        return devices;
    }


    private class DeviceItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String deviceName = deviceListAdapter.getItem(position);
            startDevicePingActivity(deviceName);
        }
    }

    private void startDevicePingActivity(String targetDeviceName) {
        Intent intent = new Intent(this, DevicePingActivity.class);
        intent.putExtra(AppConstants.DEVICE_NAME_EXTRA, targetDeviceName);
        startActivity(intent);
    }

    @Override
    public void deviceStateChanged() {
        reloadDevices();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }


}