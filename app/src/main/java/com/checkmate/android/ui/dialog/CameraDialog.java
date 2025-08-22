package com.checkmate.android.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.checkmate.android.AppConstant;
import com.checkmate.android.AppPreference;
import com.checkmate.android.BuildConfig;
import com.checkmate.android.R;
import com.checkmate.android.database.DBManager;
import com.checkmate.android.databinding.DialogCameraBinding;
import com.checkmate.android.model.Camera;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.ui.view.MySpinner;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;

import java.util.List;

// Butterknife import removed - using view binding instead

public class CameraDialog extends Dialog {

    private DialogCameraBinding binding;
    Camera camera = null;
    String ssid = "";
    String wifi_password = "";
    Context context;
    int camera_type;

    public static CameraDialog instance = null;

    public CameraDialog(Context context, Camera camera) {
        super(context);
        // TODO Auto-generated constructor stub
        this.camera = camera;
        ssid = camera.wifi_ssid;
        init(context);
    }

    public CameraDialog(Context context, String ssid) {
        super(context);
        // TODO Auto-generated constructor stub
        this.ssid = ssid;
        init(context);
    }

    public CameraDialog(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        this.camera = null;
        ssid = "";
        init(context);
        instance = this;
    }

    public CameraDialog(Context context, int camera_type) {
        super(context);
        // TODO Auto-generated constructor stub
        this.camera = null;
        this.camera_type = camera_type;
        ssid = "";
        init(context);
        setDefaultValues();
        instance = this;
    }

    void setDefaultValues() {
        switch (camera_type) {
            case AppConstant.WIFI_TYPE_VCS:
                binding.edtUrl.setText("rtsp://192.168.60.1/main_ch");
                if (index_wlan0 >= 0) {
                    binding.spinnerIn.setSelection(index_wlan0);
                }
                if (index_cellular >= 0) {
                    binding.spinnerOut.setSelection(index_cellular);
                }
                binding.radioUdp.setChecked(true);
                binding.radioTcp.setChecked(false);
                binding.chkAnonymous.setChecked(true);
                break;
            case AppConstant.WIFI_TYPE_LAWMATE:
                binding.edtUrl.setText("rtsp://192.168.1.254/xxxx.mov");
                if (index_wlan0 >= 0) {
                    binding.spinnerIn.setSelection(index_wlan0);
                }
                if (index_cellular >= 0) {
                    binding.spinnerOut.setSelection(index_cellular);
                }
                binding.radioUdp.setChecked(true);
                binding.radioTcp.setChecked(false);
                binding.chkAnonymous.setChecked(true);
                break;
            case AppConstant.WIFI_TYPE_ATN:
                binding.edtUrl.setText("rtsp://192.168.42.1:554/live");
                if (index_wlan0 >= 0) {
                    binding.spinnerIn.setSelection(index_wlan0);
                }
                if (index_cellular >= 0) {
                    binding.spinnerOut.setSelection(index_cellular);
                }
                binding.edtPassword.setText("atnsmarthd");
                binding.radioUdp.setChecked(true);
                binding.radioTcp.setChecked(false);
                binding.chkAnonymous.setChecked(true);
                break;
            case AppConstant.WIFI_TYPE_GENERIC:
                if (index_wlan0 >= 0) {
                    binding.spinnerIn.setSelection(index_wlan0);
                }
                if (index_wlan0 >= 0) {
                    binding.spinnerOut.setSelection(index_wlan0);
                }
            case AppConstant.WIFI_TYPE_RTSP:
                if (index_wlan0 >= 0) {
                    binding.spinnerIn.setSelection(index_wlan0);
                }
                if (index_wlan0 >= 0) {
                    binding.spinnerOut.setSelection(index_wlan0);
                }
                break;
        }
    }

    public CameraDialog(Context context, int camera_type, String ssid, String wifi_password) {
        super(context);
        // TODO Auto-generated constructor stub
        this.camera = null;
        this.camera_type = camera_type;
        this.ssid = ssid;
        this.wifi_password = wifi_password;
        init(context);
        setDefaultValues();
        instance = this;
    }

    private void init(Context context) {
        this.context = context;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        binding = DialogCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setCancelable(false);

        // Set up click listeners
        binding.btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSave();
            }
        });

        binding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClose();
            }
        });

        initSpinner();

        binding.chkAnonymous.setOnCheckedChangeListener((compoundButton, checked) -> {
            binding.edtUsername.setEnabled(!checked);
            binding.edtPassword.setEnabled(!checked);
        });

        binding.radioUdp.setOnClickListener(view -> {
            binding.radioUdp.setChecked(true);
            binding.radioTcp.setChecked(false);
        });
        binding.radioTcp.setOnClickListener(view -> {
            binding.radioTcp.setChecked(true);
            binding.radioUdp.setChecked(false);
        });
        binding.chkFullAddress.setChecked(true);

        if (camera != null) {
            binding.txtTitle.setText(R.string.edit_camera);
            binding.edtName.setText(camera.camera_name);

            binding.edtUrl.setText(camera.url);
            binding.edtUsername.setText(camera.username);
            binding.edtPassword.setText(camera.password);
            binding.edtUri.setText(camera.uri);
            binding.edtPort.setText(String.valueOf(camera.port));
            binding.chkAnonymous.setChecked(TextUtils.isEmpty(camera.username) && TextUtils.isEmpty(camera.password));
            binding.chkFullAddress.setChecked(camera.use_full_address);
            if (camera.rtsp_type == AppConstant.RTSP_UDP) {
                binding.radioUdp.setChecked(true);
                binding.radioTcp.setChecked(false);
            } else {
                binding.radioUdp.setChecked(false);
                binding.radioTcp.setChecked(true);
            }

            for (int i = 0; i < networks.length; i++) {
                String network = networks[i];
                if (TextUtils.equals(network, camera.wifi_in)) {
                    binding.spinnerIn.setSelection(i);
                }
                if (TextUtils.equals(network, camera.wifi_out)) {
                    binding.spinnerOut.setSelection(i);
                }
            }
        } else {
            binding.txtTitle.setText(R.string.new_camera);
            binding.edtName.setText("");
            binding.edtUrl.setText("rtsp://");
            binding.edtUsername.setText("");
            binding.edtPassword.setText("");
            binding.edtUri.setText("");
            binding.edtPort.setText("");
        }

        binding.chkFullAddress.setOnCheckedChangeListener((compoundButton, b) -> {
            binding.edtPort.setEnabled(!b);
            binding.edtUri.setEnabled(!b);
            binding.edtUsername.setEnabled(!b);
            binding.edtPassword.setEnabled(!b);
            binding.chkAnonymous.setEnabled(!b);
        });

        if (BuildConfig.DEBUG) {
//            binding.edtUrl.setText("rtsp://admin:Password1@96.69.46.125:7014/stream/profile1=r");
        }

    }

    public String[] networks;
    int index_wlan0 = -1;
    int index_cellular = -1;

    void initSpinner() {
        final ConnectivityManager connection_manager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network ss[] = connection_manager.getAllNetworks();

        networks = new String[ss.length];
        for (int i = 0; i < ss.length; ++i) {
            LinkProperties netInfo = connection_manager.getLinkProperties(ss[i]);

            String name = netInfo.getInterfaceName();

            if (TextUtils.isEmpty(name)) {
                name = "Unknown";
            }
            networks[i] = name;
            if (TextUtils.equals(name.toLowerCase(), "wlan0")) {
                index_wlan0 = i;
            }
            if (name.contains("rmnet")) { // cellular
                index_cellular = i;
            }
            List<LinkAddress> lincAddrs = netInfo.getLinkAddresses();
            for (LinkAddress item : lincAddrs) {

            }
        }
        SpinnerAdapter inAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, networks);
        SpinnerAdapter outAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, networks);
        binding.spinnerIn.setAdapter(inAdapter);
        binding.spinnerOut.setAdapter(outAdapter);
    }

    void onSave() {
        if (TextUtils.isEmpty(binding.edtName.getText().toString())) {
            MessageUtil.showToast(getContext(), R.string.invalid_name);
            return;
        }
        if (TextUtils.isEmpty(binding.edtUrl.getText().toString())) {
            MessageUtil.showToast(getContext(), R.string.no_url);
            return;
        }
        boolean is_anonymous = binding.chkAnonymous.isChecked();
        if (!binding.chkFullAddress.isChecked()) {
            if (TextUtils.isEmpty(binding.edtUri.getText().toString())) {
                MessageUtil.showToast(getContext(), R.string.no_uri);
                return;
            }
            if (TextUtils.isEmpty(binding.edtPort.getText().toString())) {
                MessageUtil.showToast(getContext(), R.string.no_port);
                return;
            }

            if (!is_anonymous) {
                if (TextUtils.isEmpty(binding.edtUsername.getText().toString())) {
                    MessageUtil.showToast(getContext(), R.string.invalid_username);
                    return;
                }
                if (TextUtils.isEmpty(binding.edtPassword.getText().toString())) {
                    MessageUtil.showToast(getContext(), R.string.invalid_password);
                    return;
                }
            }

        }

        String name = binding.edtName.getText().toString().trim();
        String url = binding.edtUrl.getText().toString().trim();
        String uri = binding.edtUri.getText().toString().trim();
        int port = 0;
        if (!TextUtils.isEmpty(binding.edtPort.getText().toString())) {
            port = Integer.parseInt(binding.edtPort.getText().toString());
        }

        if (!url.contains("rtsp://") || url.equalsIgnoreCase("rtsp://") && !url.startsWith("rtsp://")) {
            MessageUtil.showToast(getContext(), R.string.invalid_url);
            return;
        }

        String username = binding.edtUsername.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();
        if (is_anonymous) {
            username = "";
            password = "";
        }

        int rtsp_type = binding.radioTcp.isChecked() ? AppConstant.RTSP_TCP : AppConstant.RTSP_UDP;
        String wifi_in = binding.spinnerIn.getSelectedItem().toString();
        String wifi_out = binding.spinnerOut.getSelectedItem().toString();
        Camera camera = new Camera(name, url, port, uri, username, password, ssid, rtsp_type, wifi_in, wifi_out, binding.chkFullAddress.isChecked(), wifi_password);
        boolean is_updated = false;
        camera.camera_wifi_type = camera_type;
        if (this.camera == null || this.camera.id == -1) {
            if (DBManager.getInstance().isExistCamera(camera)) {
                MessageUtil.showToast(getContext(), R.string.invalid_camera);
                return;
            }
            DBManager.getInstance().addCamera(camera);
            is_updated = false;
        } else {
            this.camera.camera_name = name;
            this.camera.url = url;
            this.camera.username = username;
            this.camera.password = password;
            this.camera.port = port;
            this.camera.uri = uri;
            this.camera.rtsp_type = rtsp_type;
            this.camera.wifi_in = wifi_in;
            this.camera.wifi_out = wifi_out;
            this.camera.wifi_password = wifi_password;
            this.camera.use_full_address = binding.chkFullAddress.isChecked();
            DBManager.getInstance().updateCamera(this.camera);
            is_updated = true;
        }
        MainActivity.instance.is_dialog = true;
        MessageUtil.showToast(getContext(), R.string.Success);
        SettingsFragment.instance.get().initCamerasList();
        MainActivity.instance.updateMenu(is_updated);

        this.onClose();
    }

    void onClose() {
        instance = null;
        dismiss();
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
}
