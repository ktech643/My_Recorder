package com.checkmate.android.ui.activity;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.checkmate.android.R;
import com.checkmate.android.model.Media;
import com.checkmate.android.ui.fragment.PlaybackFragment;
import com.checkmate.android.ui.view.DragListView;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.ResourceUtil;
import com.checkmate.android.util.TabView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends BaseActionBarActivity {

    DragListView list_view;

    TabView tabv_tab;

    ListAdapter adapter;
    List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        SetTitle(R.string.share, -1);
        ShowActionBarIcons(true, R.id.action_back);

                list_view = findViewById(R.id.list_view);
        tabv_tab = findViewById(R.id.tabv_tab);

        adapter = new ListAdapter(this);
        list_view.setAdapter(adapter);
        tabv_tab.setOnTabSelectedListener(new TabView.OnTabSelectedListener() {
            @Override
            public void onTabSelected(int index) {
                if (index == 0) { // Pending

                } else { // Accepted

                }
            }
        });

        initialize();
    }

    void initialize() {
        mDataList.add("Test 1");
        mDataList.add("Test 2");
        mDataList.add("Test 3");
        mDataList.add("Test 4");
        mDataList.add("Test 5");
        adapter.notifyDataSetChanged();
    }

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.btn_invite:
                invite();
                break;
        }
    }

    void invite() {

    }

    class ListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public ListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mDataList.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public class ViewHolder {
            TextView txt_email;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_text, parent, false);
                holder = new ViewHolder();
                
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String email = mDataList.get(position);
            holder.txt_email.setText(email);

            return convertView;
        }
    }
}