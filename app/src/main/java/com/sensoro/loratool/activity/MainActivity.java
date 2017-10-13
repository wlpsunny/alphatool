package com.sensoro.loratool.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sensoro.loratool.LoRaSettingApplication;
import com.sensoro.loratool.LocationService;
import com.sensoro.loratool.R;
import com.sensoro.loratool.activity.fragment.DeviceFragment;
import com.sensoro.loratool.activity.fragment.StationFragment;
import com.sensoro.loratool.adapter.MenuInfoAdapter;
import com.sensoro.loratool.ble.BLEDevice;
import com.sensoro.loratool.ble.SensoroDevice;
import com.sensoro.loratool.ble.SensoroSensor;
import com.sensoro.loratool.ble.SensoroStation;
import com.sensoro.loratool.ble.scanner.BLEDeviceManager;
import com.sensoro.loratool.constant.Constants;
import com.sensoro.loratool.service.PollingService;
import com.sensoro.loratool.widget.MainPager;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.Position;

import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.sensoro.loratool.constant.Constants.PREFERENCE_KEY_SESSION_ID;
import static com.sensoro.loratool.constant.Constants.PREFERENCE_LOGIN;

public class MainActivity extends BaseActivity
        implements View.OnClickListener, LoRaSettingApplication.SensoroDeviceListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private MenuDrawer mMenuDrawer = null;
    private DeviceFragment deviceFragment = null;
    private StationFragment stationFragment = null;
    private ListView mListView = null;
    private MenuInfoAdapter mMenuInfoAdapter = null;
    private MainPager mainPager = null;
    private LoRaSettingApplication loRaSettingApplication;
    private LinearLayout exitLinearLayout;
    private LinearLayout mainExitLinearLayout;
    private ProgressDialog progressDialog = null;
    private ImageView menuImageView = null;
    private ImageView multiIv = null;
    private TextView titleTextView = null;
    private TextView nameTextView = null;
    private TextView exitTextView = null;
    private TextView versionTextView = null;

    private long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();

    }

    private void init() {


        loRaSettingApplication = (LoRaSettingApplication) getApplication();
        verifyStoragePermissions(this);
        startLocation();
        initWidget();
        initSensoroSDK();
        Intent intent = new Intent(this, PollingService.class);
        startService(intent);
    }




    @NeedsPermission({Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    public void startLocation() {
        if (PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                || PackageManager.PERMISSION_DENIED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                ) {
            Toast.makeText(this, R.string.tips_open_location_service, Toast.LENGTH_SHORT).show();
        } else {
            LocationService locationService = loRaSettingApplication.locationService;
            locationService.init();
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.content_main;
    }

    private void initWidget() {
        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.Type.BEHIND, Position.LEFT, MenuDrawer.MENU_DRAG_WINDOW);
        mMenuDrawer.setContentView(R.layout.content_main);
        mMenuDrawer.setDropShadowEnabled(true);
        mMenuDrawer.setMenuView(R.layout.main_left_menu);
        mMenuDrawer.setMenuSize((int) getResources().getDimension(R.dimen.x720));
        mListView = (ListView) mMenuDrawer.findViewById(R.id.left_menu_list);
        mMenuInfoAdapter = new MenuInfoAdapter(this);
        mListView.setAdapter(mMenuInfoAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                switch (position) {
                    case 0:
                        multiIv.setVisibility(View.VISIBLE);
                        mainPager.setCurrentItem(0);
                        deviceFragment.request();
                        deviceFragment.isCurrent = true;
                        titleTextView.setText(R.string.title_sensor);
                        break;
                    case 1:
                        multiIv.setVisibility(View.GONE);
                        mainPager.setCurrentItem(1);
                        stationFragment.request();
                        deviceFragment.isCurrent = false;
                        titleTextView.setText(R.string.title_station);
                        break;
                }
                mMenuInfoAdapter.setSelectedIndex(position);
                mMenuInfoAdapter.notifyDataSetChanged();
                mMenuDrawer.closeMenu();
            }
        });
        resetRootLayout();
        menuImageView = (ImageView) findViewById(R.id.content_main_menu);
        menuImageView.setOnClickListener(this);
        titleTextView = (TextView) findViewById(R.id.content_main_title);
        nameTextView = (TextView) findViewById(R.id.left_menu_name);
        nameTextView.setText(getIntent().getStringExtra("name"));
        exitTextView = (TextView) findViewById(R.id.main_exit);
        exitTextView.setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        mainPager = (MainPager) findViewById(R.id.main_container);
        List<Fragment> fragmentList = new ArrayList<>();
        deviceFragment = DeviceFragment.newInstance("");
        stationFragment = StationFragment.newInstance("");
        fragmentList.add(deviceFragment);
        fragmentList.add(stationFragment);
        mainPager.setAdapter(new PagerAdapter(getSupportFragmentManager(), fragmentList));
        exitLinearLayout = (LinearLayout) findViewById(R.id.main_ll_exit);
        exitLinearLayout.setOnClickListener(this);
        mainExitLinearLayout = (LinearLayout) findViewById(R.id.main_left_exit);
        versionTextView = (TextView) findViewById(R.id.app_version);
        ImageView filterIv = (ImageView) findViewById(R.id.content_filter);
        filterIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                if (mainPager.getCurrentItem() == 0) {
                    intent.setClass(MainActivity.this, FilterDeviceActivity.class);
                } else {
                    intent.setClass(MainActivity.this, FilterStationActivity.class);
                }
                startActivityForResult(intent, Constants.REQUEST_FILTER);
            }
        });
        multiIv = (ImageView) findViewById(R.id.content_multi);
        multiIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deviceFragment.doModelChoose();
            }
        });
        getAPPVersionCode();
        if (checkDeviceHasNavigationBar()) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(MATCH_PARENT,WRAP_CONTENT);
            params.setMargins(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.y200));
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, -1);
            mainExitLinearLayout.setLayoutParams(params);
        }
    }


    public void getAPPVersionCode() {
        PackageManager manager = this.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            String appVersionName = info.versionName; // 版本名
            int currentVersionCode = info.versionCode; // 版本号
            System.out.println(currentVersionCode + " " + appVersionName);
            versionTextView.setText("α Tool " + appVersionName + " (" + currentVersionCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void initSensoroSDK() {
        try {
            loRaSettingApplication.registersSensoroDeviceListener(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mainPager.getCurrentItem() == 0 && deviceFragment != null) {
            deviceFragment.isCurrent = true;
        } else {
            deviceFragment.isCurrent = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        deviceFragment.isCurrent = false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.main_exit:
            case R.id.main_ll_exit:
                SharedPreferences sp = getSharedPreferences(PREFERENCE_LOGIN, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.remove(PREFERENCE_KEY_SESSION_ID);
                editor.commit();
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.content_main_menu:
                mMenuDrawer.openMenu();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), R.string.exit_main,
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loRaSettingApplication.unRegistersSensoroDeviceListener(this);
        BLEDeviceManager.getInstance(this).stopService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Constants.RESULT_FILTER) {
            if (mainPager.getCurrentItem() == 0) {
                deviceFragment.filterDevice();
            } else {
                stationFragment.filterStation();
            }

        }
    }

    @Override
    public void onNewDevice(final BLEDevice bleDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (bleDevice.getType()) {
                    case BLEDevice.TYPE_DEVICE:
                        SensoroDevice sensoroDevice = (SensoroDevice) bleDevice;
                        if (sensoroDevice.getSensoroSensor() != null) {
                            deviceFragment.refreshSensorNew(sensoroDevice.getSensoroSensor());
                        }
                        deviceFragment.refreshNew(sensoroDevice);
                        break;
                    case BLEDevice.TYPE_SENSOR:
                        deviceFragment.refreshSensorNew((SensoroSensor) bleDevice);
                        break;
                    case BLEDevice.TYPE_STATION:
                        stationFragment.refreshNew((SensoroStation) bleDevice);
                        break;
                }
            }
        });

    }

    @Override
    public void onGoneDevice(final BLEDevice bleDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (bleDevice.getType()) {
                    case BLEDevice.TYPE_DEVICE:
                        deviceFragment.refreshGone((SensoroDevice) bleDevice);
                        break;
                    case BLEDevice.TYPE_SENSOR:
                        deviceFragment.refreshSensorGone((SensoroSensor) bleDevice);
                        break;
                    case BLEDevice.TYPE_STATION:
                        stationFragment.refreshGone((SensoroStation) bleDevice);
                        break;
                }
            }
        });

    }

    @Override
    public void onUpdateDevices(ArrayList<BLEDevice> deviceList) {

        for (int i = 0; i < deviceList.size(); i++) {
            BLEDevice bleDevice = deviceList.get(i);
            switch (bleDevice.getType()) {
                case BLEDevice.TYPE_SENSOR:
                    SensoroSensor sensoroSensor = (SensoroSensor) bleDevice;
                    deviceFragment.refreshSensor(sensoroSensor);
                    break;
            }
        }
    }

    private static class PagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragments;

        public PagerAdapter(android.support.v4.app.FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

    }
}
