package com.sensoro.loratool.presenter;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sensoro.lora.setting.server.bean.DeviceInfo;
import com.sensoro.lora.setting.server.bean.DeviceInfoListRsp;
import com.sensoro.loratool.LoRaSettingApplication;
import com.sensoro.loratool.R;
import com.sensoro.loratool.activity.DeviceDetailActivity;
import com.sensoro.loratool.base.BasePresenter;
import com.sensoro.loratool.imainview.IScanDeviceAcView;
import com.sensoro.loratool.utils.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.VIBRATOR_SERVICE;


public class ScanDeviceAcPresenter extends BasePresenter<IScanDeviceAcView> implements MediaPlayer.OnErrorListener
{
    private Context mContext;
    private MediaPlayer mediaPlayer;

    @Override
    public void initData(Context context) {
        mContext = context;
        mediaPlayer = buildMediaPlayer(mContext);

    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public void processResult(String result) {
        playVoice();
        if (TextUtils.isEmpty(result)) {
            getView().startScan();
            return;
        }
        String scanSerialNumber = parseResultMac(result);
        if (scanSerialNumber == null) {
            getView().startScan();
        } else {
            if (scanSerialNumber.length() == 16) {
                scanFinish(scanSerialNumber);
            } else {
                getView().startScan();
            }
        }
    }

    private void playVoice() {
        vibrate();
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(200);
        }
    }

    private MediaPlayer buildMediaPlayer(Context activity) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try (AssetFileDescriptor file = activity.getResources().openRawResourceFd(R.raw.beep)) {
            mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setLooping(false);
            mediaPlayer.setVolume(0.1f, 0.1f);
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException ioe) {
            LogUtils.loge(this, ioe.getMessage());
            mediaPlayer.release();
            return null;
        }
    }

    private String parseResultMac(String result) {

        String serialNumber = null;
        if (result != null) {
            String[] data;
            String type;
            data = result.split("\\|");
            type = data[0];
            serialNumber = type;
        }
        return serialNumber;
    }
    private void scanFinish(final String scanSerialNumber) {
        getView().showProgressDialog();
        final Intent intent = new Intent(mContext,DeviceDetailActivity.class);
        LoRaSettingApplication application = (LoRaSettingApplication) mContext.getApplicationContext();
        final boolean[] isCache = {false};
        final List<DeviceInfo> deviceInfoList = application.getDeviceInfoList();
        if(deviceInfoList.size()>0){
            for (DeviceInfo deviceInfo : deviceInfoList) {
                if(deviceInfo.getSn().equals(scanSerialNumber)){
                    intent.putExtra("deviceInfo",deviceInfo);
                    addTags(intent, deviceInfo);
                    isCache[0] = true;
                    break;
                }
            }
        }

        if(!isCache[0]){
            application.loRaSettingServer.deviceAll(scanSerialNumber, new Response.Listener<DeviceInfoListRsp>() {
                @Override
                public void onResponse(DeviceInfoListRsp response) {
                    ArrayList<DeviceInfo> infoArrayList = (ArrayList) response.getData().getItems();
                    if (infoArrayList.size() != 0) {
                        for (DeviceInfo  deviceInfo: infoArrayList) {
                            if(deviceInfo.getSn().equals(scanSerialNumber)){
                                intent.putExtra("deviceInfo",deviceInfo);
                                addTags(intent,deviceInfo);
                                isCache[0] = true;
                                break;
                            }
                        }
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            });
        }

        if(!isCache[0]){
            getView().shortToast(R.string.ac_scan_obtain_fail);
            getView().startScan();
            getView().dismissProgressDialog();

        }else{
            getView().dismissProgressDialog();
            getView().startAC(intent);
        }
    }

    private void addTags(Intent intent, DeviceInfo deviceInfo) {
        List<String> tags = deviceInfo.getTags();
        StringBuilder sb = new StringBuilder();
        if (tags!= null) {
            for (String tag : tags) {
                sb.append(tag);
            }
        }
        if(sb.length()>0){
            intent.putExtra("tags",sb.toString());
        }
    }


}
