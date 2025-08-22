package com.checkmate.android.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.wmspanel.libstream.Streamer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CameraInfo {
    public static final int LENS_FACING_FRONT = 0;
    public static final int LENS_FACING_BACK = 1;
    public static final int LENS_FACING_EXTERNAL = 2;

    public static final String ID = "id";
    public static final String PHYSICAL_ID = "physical_id";

    public String cameraId;
    public Streamer.Size[] recordSizes;
    public int lensFacing;
    public Streamer.FpsRange[] fpsRanges;
    public int minExposure;
    public int maxExposure;
    public float exposureStep;
    public List<CameraInfo> physicalCameras = new ArrayList<>();
    public float fov;

    @RequiresApi(Build.VERSION_CODES.Q)
    public static class CamId {
        public String id;
        public String physicalId;

        CamId(String id, String physicalId) {
            this.id = id;
            this.physicalId = physicalId;
        }

        @Override
        public String toString() {
            return String.format("id=%s, physicalId=%s", id, physicalId);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || (this.getClass() != other.getClass())) {
                return false;
            }
            final CameraInfo.CamId guest = (CameraInfo.CamId) other;
            return Objects.equals(this.id, guest.id)
                    && Objects.equals(this.physicalId, guest.physicalId);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024)
                .append("cameraId=").append(cameraId);

        switch (lensFacing) {
            case LENS_FACING_FRONT:
                sb.append("(FRONT)");
                break;
            case LENS_FACING_BACK:
                sb.append("(BACK)");
                break;
            case LENS_FACING_EXTERNAL:
                sb.append("(EXTERNAL)");
                break;
            default:
                break;
        }

        sb.append(", isMultiCamera=").append(physicalCameras.size() > 0).append(";");

        if (recordSizes != null) {
            sb.append("\nrecordSizes=");
            for (Streamer.Size size : recordSizes) {
                sb.append(size).append(";");
            }
        }

        if (fpsRanges != null) {
            sb.append("\nfpsRanges=");
            for (Streamer.FpsRange range : fpsRanges) {
                sb.append(range).append(";");
            }
        }

        sb.append("\nexposure=(").append(minExposure).append("..").append(maxExposure).append(");")
                .append("step=").append(exposureStep).append(";");

        sb.append("\nfov=").append(fov);

        return sb.toString();
    }

//    @RequiresApi(Build.VERSION_CODES.Q)
    public static Map<String, CameraInfo> toMap(List<CameraInfo> cameraList) {
        // LinkedHashMap presents the items in the insertion order
        final Map<String, CameraInfo> map = new LinkedHashMap<>();
        for (CameraInfo info : cameraList) {
            map.put(info.cameraId, info);
            for (CameraInfo subInfo : info.physicalCameras) {
                map.put(info.cameraId.concat(subInfo.cameraId), subInfo);
            }
        }
        return map;
    }

}
