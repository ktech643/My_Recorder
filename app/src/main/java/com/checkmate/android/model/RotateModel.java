package com.checkmate.android.model;

import android.content.Context;

import com.checkmate.android.AppConstant;
import com.checkmate.android.R;

import java.util.ArrayList;
import java.util.List;

public class RotateModel {
    public RotateModel(int resourceId, String title, boolean is_selected) {
        this.resourceId = resourceId;
        this.title = title;
        this.is_selected = is_selected;
    }

    public int resourceId;
    public String title;
    public boolean is_selected = false;

    public static List<RotateModel> initialize(Context context, int is_rotated, boolean is_flipped, boolean is_mirrored) {
        List<RotateModel> models = new ArrayList<>();
        models.add(new RotateModel(R.mipmap.ic_rotate_90, context.getString(R.string.rotate_90), is_rotated == AppConstant.is_rotated_90));
        models.add(new RotateModel(R.mipmap.ic_rotate_90, context.getString(R.string.rotate_180), is_rotated == AppConstant.is_rotated_180));
        models.add(new RotateModel(R.mipmap.ic_rotate_90, context.getString(R.string.rotate_270), is_rotated == AppConstant.is_rotated_270));
        models.add(new RotateModel(R.mipmap.ic_flip, context.getString(R.string.mirror), is_flipped));
        models.add(new RotateModel(R.mipmap.ic_mirror, context.getString(R.string.flip), is_mirrored));
        models.add(new RotateModel(R.mipmap.ic_camera, context.getString(R.string.normal), !is_flipped && (is_rotated == AppConstant.is_rotated_0) && !is_mirrored));
        return models;
    }

    public static List<RotateModel> cameraModels(List<String> list) {
        List<RotateModel> models = new ArrayList<>();
        for (String val : list) {
            models.add(new RotateModel(R.mipmap.ic_camera, val, false));
        }
        return models;
    }

}
