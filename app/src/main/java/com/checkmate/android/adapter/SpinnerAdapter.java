package com.checkmate.android.adapter;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.checkmate.android.R;
import com.checkmate.android.model.RotateModel;

import java.util.List;

public class SpinnerAdapter extends ArrayAdapter<RotateModel> {

    LayoutInflater layoutInflater;
    List<RotateModel> modelList;

    public SpinnerAdapter(Activity context, int resourceId, int textViewId, List<RotateModel> list) {
        super(context, resourceId, textViewId, list);
        layoutInflater = context.getLayoutInflater();
        modelList = list;
    }

    public void setModelList(List<RotateModel> list) {
        modelList = list;
        notifyDataSetChanged();
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.cell_dropdown_rotate, parent, false);
            holder = new ViewHolder();
            holder.img_item = convertView.findViewById(R.id.img_item);
            holder.txt_item = convertView.findViewById(R.id.txt_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        RotateModel model = modelList.get(position);
        if (model.resourceId > 0) {
            holder.img_item.setVisibility(View.VISIBLE);
            holder.img_item.setImageResource(model.resourceId);
        } else {
            holder.img_item.setVisibility(View.GONE);
        }
        holder.txt_item.setText(model.title);
        if (model.is_selected) {
            holder.txt_item.setTextColor(getContext().getResources().getColor(R.color.red));
        } else {
            holder.txt_item.setTextColor(getContext().getResources().getColor(R.color.black));
        }
        return convertView;
    }

    static class ViewHolder {
        ImageView img_item;
        TextView txt_item;
    }
}
