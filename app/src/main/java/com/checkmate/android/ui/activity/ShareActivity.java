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
import com.checkmate.android.databinding.ActivityShareBinding;
import com.checkmate.android.model.Media;
import com.checkmate.android.ui.fragment.PlaybackFragment;
import com.checkmate.android.ui.view.DragListView;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.ResourceUtil;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShareActivity extends BaseActionBarActivity {

    private ActivityShareBinding binding;
    ListAdapter adapter;
    List<String> mDataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SetTitle(R.string.share, -1);
        ShowActionBarIcons(true, R.id.action_back);

        adapter = new ListAdapter(this);
        binding.listView.setAdapter(adapter);
        binding.tabvTab.addTab(binding.tabvTab.newTab().setText("Pending"));
        binding.tabvTab.addTab(binding.tabvTab.newTab().setText("Accepted"));
        
        binding.tabvTab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) { // Pending

                } else { // Accepted

                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Set up click listeners
        binding.btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invite();
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
                holder.txt_email = convertView.findViewById(R.id.txt_email);
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