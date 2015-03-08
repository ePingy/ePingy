package com.ePingy.app;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.ePingy.DevicesStateChangedListener;
import com.ePingy.service.EPingyService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jhansi on 06/03/15.
 */
public class EPingyApplication extends Application {

    private List<String> devices = new ArrayList<>();
    private EPingyService.DeviceConnectBinder connectBinder;
    private boolean mBound;
    private Module mModule = Module.NONE;
    private String mErrorString = "ER_OK";
    private EPingyService.HostChannelState mHostChannelState = EPingyService.HostChannelState.IDLE;
    private final static String TAG = EPingyApplication.class.toString();
    private DevicesStateChangedListener devicesStateChangedListener;

    /**
     * Enumeration of the high-level moudules in the system.  There is one
     * value per module.
     */
    public static enum Module {
        NONE,
        GENERAL,
        USE,
        HOST
    }

    @Override
    public void onCreate() {
        super.onCreate();
        checkInDevice();
    }

    public void setDevicesStateChangedListener(DevicesStateChangedListener devicesStateChangedListener) {
        this.devicesStateChangedListener = devicesStateChangedListener;
    }

    public boolean isDeviceNameSet() {
        String deviceName = getDeviceName();
        return !deviceName.equals("");
    }

    public List<String> getDevices() {
        return devices;
    }

    public void addDevice(final String device) {
        String deviceFriendlyName = device.substring(device.lastIndexOf('.') + 1, device.length());
        if (!deviceFriendlyName.equals(getDeviceName())) {
            remove(device);
            devices.add(device);
        }
    }

    private void remove(String _device) {
        for (Iterator<String> i = devices.iterator(); i.hasNext(); ) {
            String device = i.next();
            if (device.equals(_device)) {
                Log.i(TAG, "removeDevice(): removed " + _device);
                i.remove();
            }
        }
    }

    public void pingDevice(String targetDevice, int messageId, String senderDevice) {
        connectBinder.pingDevice(targetDevice, messageId, senderDevice);
    }

    public void leaveFromNetwork() {
        connectBinder.leaveFromNetwork(getDeviceName());
    }

    public void removeDevice(final String _device) {
        remove(_device);
    }

    public void setDeviceName(String deviceName) {
        saveToSharedPreference(AppConstants.DEVICE_NAME, deviceName);
    }

    public String getDeviceName() {
        return getFromSharedPreference(AppConstants.DEVICE_NAME);
    }

    public void joinChannel(String deviceName) {
        connectBinder.joinNetwork(deviceName);
    }

    public void checkInDevice() {
        Intent easyReachService = new Intent(this, EPingyService.class);
        bindService(easyReachService, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void saveToSharedPreference(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreference();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    private String getFromSharedPreference(String key) {
        SharedPreferences sharedPreferences = getSharedPreference();
        String value = sharedPreferences.getString(key, "");
        return value;
    }

    private SharedPreferences getSharedPreference() {
        SharedPreferences sharedPreferences = getSharedPreferences(EPingyApplication.class.toString(), Context.MODE_PRIVATE);
        return sharedPreferences;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            connectBinder = (EPingyService.DeviceConnectBinder) service;
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connectBinder = null;
            mBound = false;
        }
    };

    public synchronized void alljoynError(Module m, String s) {
        mModule = m;
        mErrorString = s;
        Log.e(TAG, s);
    }

    public synchronized void hostSetChannelState(EPingyService.HostChannelState state) {
        mHostChannelState = state;
    }

}