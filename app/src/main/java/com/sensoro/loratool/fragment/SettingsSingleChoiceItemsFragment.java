package com.sensoro.loratool.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.sensoro.loratool.R;
import com.sensoro.loratool.activity.SettingDeviceActivity;
import com.sensoro.loratool.utils.DialogFragmentUtils;


/**
 * Created by Sensoro on 15/8/5.
 */
public class SettingsSingleChoiceItemsFragment extends SettingsBaseDialogFragment {

    public static final String ITEMS = "ITEMS";
    public static final String ITEM = "ITEM";
    public static final String INDEX = "INDEX";

    private View dialogView;
    private TextView levelTipsTextView;
    private TextView tipsDetailTextView;

    private int selectedIndex;

    public static SettingsSingleChoiceItemsFragment newInstance(String[] items, int index) {
        SettingsSingleChoiceItemsFragment settingsSingleChoiceItemsFragment = new SettingsSingleChoiceItemsFragment();
        Bundle args = new Bundle();
        args.putStringArray(ITEMS, items);
        args.putInt(INDEX, index);
        settingsSingleChoiceItemsFragment.setArguments(args);
        return settingsSingleChoiceItemsFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        try {
            String[] items = getArguments().getStringArray(ITEMS);
            int index = getArguments().getInt(INDEX);
            selectedIndex = index;
            setViewVisible(index);
            int title = getTitleId();
            if (title != R.string.prevent_squatters) {
                AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setView(dialogView)
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Bundle bundle = new Bundle();
                                bundle.putInt(INDEX, selectedIndex);
                                bundle.putString(ITEM, getArguments().getStringArray(ITEMS)[selectedIndex]);
                                onPositiveButtonClickListener.onPositiveButtonClick(SettingsSingleChoiceItemsFragment.this.getTag(), bundle);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                builder.setSingleChoiceItems(items, index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedIndex = which;
                        if (SettingDeviceActivity.SETTINGS_BLE_POWER.equals(getTag())) {
                            setPowerView();
                        }
                    }
                });

                return builder.create();
            } else {
                Dialog dialog = new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setView(dialogView)
                        .setSingleChoiceItems(items, index, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selectedIndex = which;
                                setPowerView();
                                setBroadcastKeyView(which);
                            }
                        })
                        .setNeutralButton(R.string.management, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Bundle bundle = new Bundle();
                                bundle.putInt(INDEX, selectedIndex);
                                onPositiveButtonClickListener.onPositiveButtonClick(SettingsSingleChoiceItemsFragment.this.getTag(), bundle);
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .create();
                return dialog;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setViewVisible(int index) {
        if (getTag().equals(SettingDeviceActivity.SETTINGS_BLE_POWER)|| getTag().equals(SettingDeviceActivity.SETTINGS_LORA_TXP) ||getTag().equals(SettingDeviceActivity.SETTINGS_LORA_EIRP)) {
            levelTipsTextView.setVisibility(View.GONE);
            tipsDetailTextView.setVisibility(View.VISIBLE);
            setPowerView();
        } else if (getTag().equals(SettingDeviceActivity.SETTINGS_ADV_INTERVAL)) {
            tipsDetailTextView.setVisibility(View.VISIBLE);
            setAdvIntervalView(index);
        } else if (getTag().equals(SettingDeviceActivity.SETTINGS_BROADCAST_KEY)) {
            tipsDetailTextView.setVisibility(View.VISIBLE);
            setBroadcastKeyView(index);
        }

    }

    private void setPowerView() {
        tipsDetailTextView.setText(R.string.setting_power_tips);
    }

    private void setBroadcastKeyView(int index) {
        tipsDetailTextView.setText(R.string.setting_key_reminder);
    }

    private void setAdvIntervalView(int index) {
        tipsDetailTextView.setText(R.string.setting_interval_tips);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        dialogView = inflater.inflate(R.layout.dialog_settings_tips_v4, null);
        levelTipsTextView = (TextView) dialogView.findViewById(R.id.settings_tips_v4_tv_level_tips);
        tipsDetailTextView = (TextView) dialogView.findViewById(R.id.settings_tips_v4_tv_tips_detail);
    }

    @Override
    public void onStart() {
        super.onStart();
        DialogFragmentUtils.fitListView(getDialog(),getActivity());
    }

    public int getTitleId() {
        String tag = getTag();
        if (tag.equals(SettingDeviceActivity.SETTINGS_BLE_POWER)) {
            return R.string.setting_power;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_ADV_INTERVAL)) {
            return R.string.setting_rate;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_BROADCAST_KEY)) {
            return R.string.prevent_squatters;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_TEMP_INTERVAL)) {
            return R.string.setting_temp;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_LIGHT_INTERVAL)) {
            return R.string.setting_brightness;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_ACCELER)) {
            return R.string.setting_speed;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_TLM_INTERVAL)) {
            return R.string.settings_tlm_interval;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_HUMIDITY_INTERVAL)) {
            return R.string.setting_humidity;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_BLE_TURNON_TIME)) {
            return R.string.setting_ble_turnon_time;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_BLE_TURNOFF_TIME)) {
            return R.string.setting_ble_turnoff_time;
        } else if (tag.equals(SettingDeviceActivity.SETTINGS_LORA_TXP) || tag.equals(SettingDeviceActivity.SETTINGS_LORA_EIRP)) {
            return R.string.setting_lora_txp;
        }
        return R.string.settings;
    }
}
