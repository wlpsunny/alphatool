package com.sensoro.loratool;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.sensoro.libbleserver.ble.entity.BLEDevice;
import com.sensoro.libbleserver.ble.entity.SensoroDevice;
import com.sensoro.libbleserver.ble.scanner.BLEDeviceListener;
import com.sensoro.libbleserver.ble.scanner.BLEDeviceManager;
import com.sensoro.lora.setting.server.LoRaSettingServerImpl;
import com.sensoro.lora.setting.server.bean.DeviceInfo;
import com.sensoro.loratool.imainview.IScanDeviceAcView;
import com.sensoro.loratool.store.LoraDbHelper;
import com.sensoro.loratool.utils.IPUtil;
import com.sensoro.station.communication.BuildConfig;
import com.sensoro.station.communication.IStation;
import com.sensoro.station.communication.StationImpl;
import com.sensoro.station.communication.bean.StationInfo;
import com.squareup.leakcanary.RefWatcher;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.tencent.bugly.beta.UpgradeInfo;
import com.tencent.bugly.beta.ui.UILifecycleListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//
//import com.facebook.stetho.Stetho;
//import com.facebook.stetho.okhttp3.StethoInterceptor;


/**
 * Created by tangrisheng on 2016/5/9.
 * LoRaSetting Application
 */
public class LoRaSettingApplication extends Application implements BLEDeviceListener<BLEDevice> {



    public ConcurrentHashMap<String, SensoroDevice> mNearDeviceMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, DeviceInfo> mCacheDeviceMap;
    public IStation station;
    public LoRaSettingServerImpl loRaSettingServer;
    private List<StationInfo> stationInfoList = new ArrayList<>();
    private List<DeviceInfo> deviceInfoList = new ArrayList<>();
    private ConcurrentHashMap<String, SensoroDevice> sensoroDeviceMap = new ConcurrentHashMap<>();
    private BLEDeviceManager bleDeviceManager;
    private ArrayList<SensoroDeviceListener> sensoroDeviceListeners = new ArrayList<>();
    private RefWatcher refWatcher;
    private ArrayList<INearDeviceListener> nearDeviceListenerList = new ArrayList<INearDeviceListener>();
    private int count = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        init();

    }

    public static RefWatcher getRefWatcher(Context context) {
        LoRaSettingApplication application = (LoRaSettingApplication) context.getApplicationContext();
        return application.refWatcher;
    }


    private void init() {
//        refWatcher = LeakCanary.install(this);
        loRaSettingServer = LoRaSettingServerImpl.getInstance(getApplicationContext());
        //判断程序是否在后台
        registerActivityLifecycle();
        initStationHandler();
        LoraDbHelper.init(this);
        initSensoroSDK();
        initBuglySdk();
    }



    private void registerActivityLifecycle() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {
                if(count == 0 && bleDeviceManager != null){
                    bleDeviceManager.startScan();
                }
                count++;
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                count--;
                if(count == 0 && bleDeviceManager != null){
                    bleDeviceManager.stopScan();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    private void initBuglySdk() {
        Beta.largeIconId = R.mipmap.ic_launcher;
        Beta.smallIconId = R.mipmap.ic_launcher;
        Beta.upgradeCheckPeriod = 60000;
        Beta.upgradeDialogLayoutId = R.layout.layout_upgrade_dialog;
        Beta.enableHotfix = false;
        Bugly.init(getApplicationContext(),"326264e984",BuildConfig.DEBUG);

    }

    private void initSensoroSDK() {
        try {
            bleDeviceManager = BLEDeviceManager.getInstance(this);
            boolean isEnable = bleDeviceManager.startService();
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                bleDeviceManager.setForegroundScanPeriod(7000);
//                bleDeviceManager.setOutOfRangeDelay(15000);
//            }
////            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
////                bleDeviceManager.setForegroundScanPeriod(5000);
////                bleDeviceManager.setOutOfRangeDelay(15000);
////            }
//            else {
//                bleDeviceManager.setOutOfRangeDelay(10000);
//            }

            if (!isEnable) {
                Toast.makeText(this, R.string.tips_open_ble_service, Toast.LENGTH_SHORT).show();
            } else {
                bleDeviceManager.setBLEDeviceListener(this);
                bleDeviceManager.setBackgroundMode(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }//yangzhiqiang@sensoro.com 123456
    }

    @Override
    public void onTerminate() {
        stationInfoList.clear();
        deviceInfoList.clear();
        sensoroDeviceMap.clear();
        LoraDbHelper.instance.close();
        bleDeviceManager.stopService();
        super.onTerminate();
    }

    public void initStationHandler() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        String localIP = IPUtil.getWifiIP(wifiManager.getConnectionInfo());
        String gateWayIP = IPUtil.getGateWayIP(localIP);
        station = StationImpl.getInstance(getApplicationContext(), gateWayIP);
    }

    public List<StationInfo> getStationInfoList() {
        return stationInfoList;
    }

    public List<DeviceInfo> getDeviceInfoList() {
        return deviceInfoList;
    }

    public ConcurrentHashMap<String, SensoroDevice> getSensoroDeviceMap() {
        return sensoroDeviceMap;
    }
    public void registersSensoroDeviceListener(SensoroDeviceListener sensoroDeviceListener) {
        if (sensoroDeviceListeners != null) {
            synchronized (sensoroDeviceListeners) {
                if (!sensoroDeviceListeners.contains(sensoroDeviceListener)) {
                    sensoroDeviceListeners.add(sensoroDeviceListener);
                }
            }
        }
    }

    public void unRegistersSensoroDeviceListener(SensoroDeviceListener sensoroDeviceListener) {
        if (sensoroDeviceListener != null) {
            synchronized (sensoroDeviceListeners) {
                if (sensoroDeviceListeners.contains(sensoroDeviceListener)) {
                    sensoroDeviceListeners.remove(sensoroDeviceListener);
                }
            }
        }
    }

    @Override
    public void onNewDevice(BLEDevice bleDevice) {
        if (bleDevice instanceof SensoroDevice) {
            sensoroDeviceMap.put(bleDevice.getSn(), (SensoroDevice)bleDevice);
        }
        if (sensoroDeviceListeners != null) {
            synchronized (sensoroDeviceListeners) {
                if (sensoroDeviceListeners.size() != 0) {
                    for (SensoroDeviceListener listener : sensoroDeviceListeners) {
                        listener.onNewDevice(bleDevice);
                    }
                }
            }
        }
    }

    @Override
    public void onGoneDevice(BLEDevice bleDevice) {
        sensoroDeviceMap.remove(bleDevice.getSn());
        if (sensoroDeviceListeners != null) {
            synchronized (sensoroDeviceListeners) {
                if (sensoroDeviceListeners.size() != 0) {
                    for (SensoroDeviceListener listener : sensoroDeviceListeners) {
                        listener.onGoneDevice(bleDevice);
                    }
                }
            }
        }
    }

    @Override
    public void onUpdateDevices(ArrayList<BLEDevice> deviceList) {
        if (sensoroDeviceListeners != null) {
            synchronized (sensoroDeviceListeners) {
                if (sensoroDeviceListeners.size() != 0) {
                    for (SensoroDeviceListener listener : sensoroDeviceListeners) {
                        listener.onUpdateDevices(deviceList);
                    }
                }
            }
        }
    }

    public void updateNearDeviceMap() {
        for (INearDeviceListener iNearDeviceListener : nearDeviceListenerList) {
            iNearDeviceListener.updateListener();
        }
    }
    public void registerNearDeviceListener(INearDeviceListener listener){
        if (!nearDeviceListenerList.contains(listener)) {
            nearDeviceListenerList.add(listener);
        }


    }

    public void unregisterNearDeviceListener(INearDeviceListener listener){
        nearDeviceListenerList.remove(listener);
    }

    public void release() {
        //释放资源
        stationInfoList.clear();
        deviceInfoList.clear();
        sensoroDeviceMap.clear();
        LoraDbHelper.instance.close();
        bleDeviceManager.stopService();
    }

    public interface SensoroDeviceListener {
        void onNewDevice(BLEDevice bleDevice);
        void onGoneDevice(BLEDevice bleDevice);
        void onUpdateDevices(ArrayList<BLEDevice> deviceList);
    }

    public ConcurrentHashMap<String, SensoroDevice> getmNearDeviceMap() {
        return mNearDeviceMap;
    }

    public void setmNearDeviceMap(ConcurrentHashMap<String, SensoroDevice> mNearDeviceMap) {
        this.mNearDeviceMap = mNearDeviceMap;
    }

    public ConcurrentHashMap<String, DeviceInfo> getmCacheDeviceMap() {
        return mCacheDeviceMap;
    }

    public void setmCacheDeviceMap(ConcurrentHashMap<String, DeviceInfo> mCacheDeviceMap) {
        this.mCacheDeviceMap = mCacheDeviceMap;
    }

    public interface INearDeviceListener {
        void updateListener();
    }
}
