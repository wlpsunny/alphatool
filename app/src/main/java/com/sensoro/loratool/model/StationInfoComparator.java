package com.sensoro.loratool.model;

import com.sensoro.lora.setting.server.bean.DeviceInfo;
import com.sensoro.station.communication.bean.StationInfo;

import java.util.Comparator;

/**
 * Created by sensoro on 17/2/14.
 */

public class StationInfoComparator implements Comparator<StationInfo> {


    @Override
    public int compare(StationInfo lhs, StationInfo rhs) {
        if (lhs.getSort() < rhs.getSort()) {
            return 1;
        } else if (lhs.getSort() == rhs.getSort()) {
            return 0;
        } else {
            return -1;
        }
    }
}
