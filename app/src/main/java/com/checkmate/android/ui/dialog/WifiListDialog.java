package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;
import com.checkmate.android.R;
import com.checkmate.android.databinding.DialogWifilistBinding;
import com.checkmate.android.ui.view.DragListView;
import com.checkmate.android.util.MessageUtil;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener;

import java.util.ArrayList;
import java.util.List;

public class WifiListDialog extends Dialog implements DragListView.OnRefreshLoadingMoreListener {

    @Override
    public void onDragRefresh() {
        getData();
    }

    @Override
    public void onDragLoadMore() {

    }

    public interface onResultListener{
        void onResult(String ssid);
    }

    private DialogWifilistBinding binding;
    List<ScanResult> mDataList = new ArrayList<>();

    onResultListener listener;
    Context context;
    ListAdapter adapter;

    public WifiListDialog(Context context, int theme) {
        super(context, theme);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public WifiListDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogWifilistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(true);
        this.context = context;

        adapter = new ListAdapter(context);
        binding.listView.setAdapter(adapter);
        binding.listView.setOnRefreshListener(this);
        binding.listView.refresh();
        binding.listView.setOnItemClickListener((adapterView, view, position, l) -> {
            ScanResult result = mDataList.get(position - 1);
            if (listener != null) {
                listener.onResult(result.SSID);
                dismiss();
            }
        });
    }

    void getData() {
        mDataList = new ArrayList<>();
        WifiUtils.withContext(context).scanWifi(scanResults -> {
            binding.listView.onRefreshComplete();
            for (ScanResult result : scanResults) {
                if (!TextUtils.isEmpty(result.SSID)) {
                    mDataList.add(result);
                }
            }
            updateUI();
        }).start();
    }

    void updateUI() {
        if (mDataList.size() == 0) {
            MessageUtil.showToast(context, "No connections around you!");
            dismiss();
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void show() {
        // we are using try - catch in order to prevent crashing issue
        // when the activity is finished but the AsyncTask is still processing
        try {
            super.show();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    public void setResultListener(onResultListener listener) {
        this.listener = listener;
    }

    public class ListAdapter extends BaseAdapter {

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
            TextView txt_name;
            Switch swt_camera;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            ScanResult scanResult = mDataList.get(position);
            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_camera, parent, false);
                holder = new ViewHolder();
                holder.txt_name = convertView.findViewById(R.id.txt_name);
                holder.swt_camera = convertView.findViewById(R.id.swt_camera);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.swt_camera.setVisibility(View.GONE);
            holder.txt_name.setText(scanResult.SSID);
            return convertView;
        }
    }
}
