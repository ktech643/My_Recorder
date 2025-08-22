package com.checkmate.android.database;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.checkmate.android.R;

import java.util.List;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final LayoutInflater inflater;
    private final int resource;

    public CustomListAdapter(@NonNull Context context, @NonNull List<String> items) {
        super(context, R.layout.simple_list_item, items);
        this.inflater = LayoutInflater.from(context);
        this.resource = R.layout.simple_list_item;
    }

    @Override
    @NonNull
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(resource, parent, false);
        }

        if (convertView != null) {
            TextView textView = convertView.findViewById(R.id.text1);
            if (textView != null) {
                textView.setText(getItem(position));
                textView.setTextColor(0xFF000000); // Set text color to black
            } else {
                Log.e("CustomListAdapter", "TextView text1 is null");
            }
        } else {
            Log.e("CustomListAdapter", "convertView is null");
        }

        return convertView;
    }

}

