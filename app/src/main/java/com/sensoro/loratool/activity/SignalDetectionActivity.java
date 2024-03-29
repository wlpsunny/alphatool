package com.sensoro.loratool.activity;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sensoro.libbleserver.ble.callback.SensoroConnectionCallback;
import com.sensoro.libbleserver.ble.callback.SensoroWriteCallback;
import com.sensoro.libbleserver.ble.connection.SensoroDeviceConnection;
import com.sensoro.libbleserver.ble.constants.CmdType;
import com.sensoro.libbleserver.ble.entity.BLEDevice;
import com.sensoro.libbleserver.ble.entity.SensoroDevice;
import com.sensoro.loratool.R;
import com.sensoro.loratool.fragment.SettingsSingleChoiceItemsFragment;
import com.sensoro.loratool.adapter.SignalAdapter;
import com.sensoro.loratool.adapter.SignalArrayAdapter;
import com.sensoro.loratool.constant.Constants;
import com.sensoro.loratool.event.OnPositiveButtonClickListener;
import com.sensoro.loratool.model.SignalData;
import com.sensoro.libbleserver.ble.proto.ProtoMsgTest1U1;
import com.sensoro.loratool.utils.DateUtil;
import com.sensoro.loratool.utils.ParamUtil;
import com.sensoro.loratool.widget.RecycleViewDivider;
import com.sensoro.loratool.widget.RecycleViewItemClickListener;
import com.umeng.analytics.MobclickAgent;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by sensoro on 16/9/26.
 */

public class SignalDetectionActivity extends BaseActivity implements OnPositiveButtonClickListener,
        SensoroConnectionCallback, SensoroWriteCallback {

    @BindView(R.id.signal_freq)
    TextView freqTextView;
    @BindView(R.id.signal_send)
    TextView sendTextView;
    @BindView(R.id.signal_receive)
    TextView receiveTextView;
    @BindView(R.id.signal_rate)
    TextView rateTextView;
    @BindView(R.id.signal_play)
    ImageButton signalPlayButton;
    RecyclerView mRecyclerView;
    private static final int START = 0;
    private static final int STOP = 1;
    private ProgressDialog progressDialog;
    private SignalArrayAdapter mArrayAdapter;
    private SensoroDeviceConnection sensoroDeviceConnection;
    private SensoroDevice sensoroDevice;
    private SignalAdapter mSignalAdapter;
    private String band;
    private int sendCount;
    private int receiveCount;
    private int selectedFreq;
    private int buttonStatus = STOP;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensoroDevice = this.getIntent().getParcelableExtra(Constants.EXTRA_NAME_DEVICE);
        band = this.getIntent().getStringExtra(Constants.EXTRA_NAME_BAND);
        init();
        MobclickAgent.onPageStart("设备信号检测");
    }

    private void setFreqTextViewVisible(boolean isVisible) {
        //频点选择功能去掉了
        freqTextView.setVisibility(View.GONE);
//        freqTextView.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.settings_v4_signal_back)
    public void back() {
        this.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_signal;
    }

    private void init() {
        setContentView(R.layout.activity_signal);
        ButterKnife.bind(this);
        resetRootLayout();
//        signal_spinner = (Spinner) findViewById(R.id.signal_spinner);
        freqTextView.setText(getString(R.string.random));
        //适配器
        String[] signal_array = getResources().getStringArray(R.array.signal_spinner_array);
        mArrayAdapter = new SignalArrayAdapter(this, signal_array);
        //设置样式
        mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        //加载适配器
//        signal_spinner.setAdapter(mArrayAdapter);
//        signal_spinner.setDropDownHorizontalOffset(5);
//        signal_spinner.setDropDownVerticalOffset(5);

        mRecyclerView = (RecyclerView) findViewById(R.id.signal_rv);
        mSignalAdapter = new SignalAdapter(this, new RecycleViewItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }
        });
//        signal_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                spinnerSelectedIndex = i;
//                if (i == 0) {
//
//                } else {
//                    showChoiceDialog();
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> adapterView) {
//
//            }
//        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mSignalAdapter);
        int spacingInPixels = getResources().getDimensionPixelSize(R.dimen.y100);
        RecycleViewDivider mDivider = new RecycleViewDivider(this, LinearLayoutManager.HORIZONTAL, spacingInPixels,
                android.R.color.black, false);
        mRecyclerView.addItemDecoration(mDivider);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.connecting));
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
//                    SignalDetectionActivity.this.finish();
                if (sensoroDeviceConnection != null) {
                    sensoroDeviceConnection.disconnect();
                }
            }
        });
        progressDialog.setCanceledOnTouchOutside(false);
        sensoroDeviceConnection = new SensoroDeviceConnection(this, sensoroDevice.getMacAddress(), true);
    }

    private void showChoiceDialog() {
        DialogFragment dialogFragment = SettingsSingleChoiceItemsFragment.newInstance(ParamUtil.getLoraBandText(this,
                band), 0);
        dialogFragment.show(getFragmentManager(), "SETTINGS_SIGNAL");
    }

    private void refresh(ProtoMsgTest1U1.MsgTest msgTest) {
        SignalData signalData = new SignalData();
        signalData.setUplinkDR(msgTest.getUplinkDR());
        signalData.setUplinkFreq(msgTest.getUplinkFreq());
        signalData.setUplinkInterval(msgTest.getUplinkInterval());
        signalData.setUplinkRSSI(msgTest.getUplinkRSSI());
        signalData.setUplinkSNR(msgTest.getUplinkSNR());
        signalData.setUplinkTxPower(msgTest.getUplinkTxPower());
        signalData.setDownlinkDR(msgTest.getDownlinkDR());
        signalData.setDownlinkFreq(msgTest.getDownlinkFreq());
        signalData.setDownlinkRSSI(msgTest.getDownlinkRSSI());
        signalData.setDownlinkSNR(msgTest.getDownlinkSNR());
        signalData.setDownlinkTxPower(msgTest.getDownlinkTxPower());
        Calendar calendar = Calendar.getInstance();
        signalData.setDate(DateUtil.getFullDate(calendar.getTimeInMillis()));

        if (msgTest.getDownlinkSNR() != 0 && msgTest.getDownlinkFreq() != 0 && msgTest.getDownlinkRSSI() != 0 &&
                msgTest.getDownlinkSNR() != 0 && msgTest.getDownlinkTxPower() != 0) {
            receiveCount++;
        }
        sendCount++;
        float rate = (float) (receiveCount) / sendCount * 100;
        String rateString = String.format("%.1f", rate);
        //

        sendTextView.setText(getString(R.string.send) + " " + sendCount);
        receiveTextView.setText(getString(R.string.receive) + " " + receiveCount);
        rateTextView.setText(getString(R.string.success_rate) + " " + rateString + "%");
        mSignalAdapter.appendData(signalData);
        Log.e("hcs",":加入数据::");
        mSignalAdapter.notifyDataSetChanged();
    }

    private void connect() {
        try {
            progressDialog.show();
            sensoroDeviceConnection.connect(sensoroDevice.getPassword(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDetectionCmd() {

        sensoroDeviceConnection.writeSignalData(selectedFreq, sensoroDevice.getLoraDr(), sensoroDevice.getLoraTxp(),
                5, this);

    }

    @OnClick(R.id.signal_freq)
    public void freq() {
        showChoiceDialog();
    }

    @OnClick(R.id.signal_play)
    public void play() {
        if (buttonStatus == STOP) {
            connect();
        } else {
            buttonStatus = STOP;
            signalPlayButton.setImageResource(R.mipmap.ic_play);
            sensoroDeviceConnection.disconnect();
            setFreqTextViewVisible(true);
        }

    }

    @Override
    public void onConnectedSuccess(BLEDevice bleDevice, int cmd) {
        String sn = sensoroDevice.getSn();
        String firmwareVersion = sensoroDevice.getFirmwareVersion();
//        sensoroDevice = (SensoroDevice) bleDevice;
        sensoroDevice.setFirmwareVersion(firmwareVersion);
        sensoroDevice.setSn(sn);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
                sendCount = 0;
                receiveCount = 0;
                buttonStatus = STOP;
                setFreqTextViewVisible(false);
                sendDetectionCmd();
                Log.e("hcs","连接成功:::");
                Toast.makeText(SignalDetectionActivity.this, R.string.connect_success, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onConnectedFailure(int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                sendCount = 0;
                receiveCount = 0;
                buttonStatus = STOP;
                if (sensoroDeviceConnection != null) {
                    sensoroDeviceConnection.disconnect();
                }
                setFreqTextViewVisible(true);
                Toast.makeText(SignalDetectionActivity.this, R.string.connect_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDisconnected() {
        setFreqTextViewVisible(true);
    }

    @Override
    public void onWriteSuccess(final Object o, final int cmd) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e("hcs",":写入返回::"+cmd);
                if (cmd == CmdType.CMD_SIGNAL) {
                    if (o == null) {
                        Log.e("hcs",":写入返回第一次::");
                        buttonStatus = START;
                        signalPlayButton.setImageResource(R.mipmap.ic_stop);
                    } else {
                        ProtoMsgTest1U1.MsgTest msgTest = (ProtoMsgTest1U1.MsgTest) o;
                        Log.e("hcs",":写入返回::刷新");
                        refresh(msgTest);
                    }

                }

            }
        });
    }

    @Override
    public void onWriteFailure(int errorCode, final int cmd) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setFreqTextViewVisible(true);
        if (sensoroDeviceConnection != null) {
            sensoroDeviceConnection.disconnect();
        }
    }

    @Override
    public void onPositiveButtonClick(String tag, Bundle bundle) {
        if (tag.equals("SETTINGS_SIGNAL")) {
            int index = bundle.getInt(SettingsSingleChoiceItemsFragment.INDEX);
            if (index == 0) {
                selectedFreq = 0;
                freqTextView.setText(getString(R.string.random));
            } else {
                selectedFreq = ParamUtil.getLoraBandIntArray(band)[index];
                freqTextView.setText((float) selectedFreq / 1000000 + " MHz");
            }
        }
    }

}
