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
import com.checkmate.android.model.Camera;
import com.checkmate.android.ui.fragment.SettingsFragment;
import com.checkmate.android.ui.view.MySpinner;
import com.checkmate.android.util.CommonUtil;
import com.checkmate.android.util.MainActivity;
import com.checkmate.android.util.MessageUtil;

import java.util.List;

public class CameraDialog extends Dialog {

    EditText edt_name;

    TextView txt_title;

    EditText edt_url;

    EditText edt_username;

    EditText edt_password;

    CheckBox chk_anonymous;

    EditText edt_port;

    EditText edt_uri;

    CheckBox chk_full_address;

    RadioButton radio_udp;

    RadioButton radio_tcp;

    Camera camera = null;
    String ssid = "";
    String wifi_password = "";

    public MySpinner spinner_in;

    public MySpinner spinner_out;

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
                edt_url.setText("rtsp://192.168.60.1/main_ch");
                if (index_wlan0 >= 0) {
                    spinner_in.setSelection(index_wlan0);
                }
                if (index_cellular >= 0) {
                    spinner_out.setSelection(index_cellular);
                }
                radio_udp.setChecked(true);
                radio_tcp.setChecked(false);
                chk_anonymous.setChecked(true);
                break;
            case AppConstant.WIFI_TYPE_LAWMATE:
                edt_url.setText("rtsp://192.168.1.254/xxxx.mov");
                if (index_wlan0 >= 0) {
                    spinner_in.setSelection(index_wlan0);
                }
                if (index_cellular >= 0) {
                    spinner_out.setSelection(index_cellular);
                }
                radio_udp.setChecked(true);
                radio_tcp.setChecked(false);
                chk_anonymous.setChecked(true);
                break;
            case AppConstant.WIFI_TYPE_ATN:
                edt_url.setText("rtsp://192.168.42.1:554/live");
                if (index_wlan0 >= 0) {
                    spinner_in.setSelection(index_wlan0);
                }
                if (index_cellular >= 0) {
                    spinner_out.setSelection(index_cellular);
                }
                edt_password.setText("atnsmarthd");
                radio_udp.setChecked(true);
                radio_tcp.setChecked(false);
                chk_anonymous.setChecked(true);
                break;
            case AppConstant.WIFI_TYPE_GENERIC:
                if (index_wlan0 >= 0) {
                    spinner_in.setSelection(index_wlan0);
                }
                if (index_wlan0 >= 0) {
                    spinner_out.setSelection(index_wlan0);
                }
            case AppConstant.WIFI_TYPE_RTSP:
                if (index_wlan0 >= 0) {
                    spinner_in.setSelection(index_wlan0);
                }
                if (index_wlan0 >= 0) {
                    spinner_out.setSelection(index_wlan0);
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
        setContentView(R.layout.dialog_camera);
        setCancelable(false);

                edt_name = findViewById(R.id.edt_name);
        txt_title = findViewById(R.id.txt_title);
        edt_url = findViewById(R.id.edt_url);
        edt_username = findViewById(R.id.edt_username);
        edt_password = findViewById(R.id.edt_password);
        chk_anonymous = findViewById(R.id.chk_anonymous);
        edt_port = findViewById(R.id.edt_port);
        edt_uri = findViewById(R.id.edt_uri);
        chk_full_address = findViewById(R.id.chk_full_address);
        radio_udp = findViewById(R.id.radio_udp);
        radio_tcp = findViewById(R.id.radio_tcp);

        initSpinner();

        chk_anonymous.setOnCheckedChangeListener((compoundButton, checked) -> {
            edt_username.setEnabled(!checked);
            edt_password.setEnabled(!checked);
        });

        radio_udp.setOnClickListener(view -> {
            radio_udp.setChecked(true);
            radio_tcp.setChecked(false);
        });
        radio_tcp.setOnClickListener(view -> {
            radio_tcp.setChecked(true);
            radio_udp.setChecked(false);
        });
        chk_full_address.setChecked(true);

        if (camera != null) {
            txt_title.setText(R.string.edit_camera);
            edt_name.setText(camera.camera_name);

            edt_url.setText(camera.url);
            edt_username.setText(camera.username);
            edt_password.setText(camera.password);
            edt_uri.setText(camera.uri);
            edt_port.setText(String.valueOf(camera.port));
            chk_anonymous.setChecked(TextUtils.isEmpty(camera.username) && TextUtils.isEmpty(camera.password));
            chk_full_address.setChecked(camera.use_full_address);
            if (camera.rtsp_type == AppConstant.RTSP_UDP) {
                radio_udp.setChecked(true);
                radio_tcp.setChecked(false);
            } else {
                radio_udp.setChecked(false);
                radio_tcp.setChecked(true);
            }

            for (int i = 0; i < networks.length; i++) {
                String network = networks[i];
                if (TextUtils.equals(network, camera.wifi_in)) {
                    spinner_in.setSelection(i);
                }
                if (TextUtils.equals(network, camera.wifi_out)) {
                    spinner_out.setSelection(i);
                }
            }
        } else {
            txt_title.setText(R.string.new_camera);
            edt_name.setText("");
            edt_url.setText("rtsp://");
            edt_username.setText("");
            edt_password.setText("");
            edt_uri.setText("");
            edt_port.setText("");
        }

        chk_full_address.setOnCheckedChangeListener((compoundButton, b) -> {
            edt_port.setEnabled(!b);
            edt_uri.setEnabled(!b);
            edt_username.setEnabled(!b);
            edt_password.setEnabled(!b);
            chk_anonymous.setEnabled(!b);
        });

        if (BuildConfig.DEBUG) {
//            edt_url.setText("rtsp://admin:Password1@96.69.46.125:7014/stream/profile1=r");
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
        spinner_in.setAdapter(inAdapter);
        spinner_out.setAdapter(outAdapter);
    }

    public void OnClick(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                onSave();
                break;
            case R.id.btn_close:
                onClose();
                break;
        }
    }

    void onSave() {
        if (TextUtils.isEmpty(edt_name.getText().toString())) {
            MessageUtil.showToast(getContext(), R.string.invalid_name);
            return;
        }
        if (TextUtils.isEmpty(edt_url.getText().toString())) {
            MessageUtil.showToast(getContext(), R.string.no_url);
            return;
        }
        boolean is_anonymous = chk_anonymous.isChecked();
        if (!chk_full_address.isChecked()) {
            if (TextUtils.isEmpty(edt_uri.getText().toString())) {
                MessageUtil.showToast(getContext(), R.string.no_uri);
                return;
            }
            if (TextUtils.isEmpty(edt_port.getText().toString())) {
                MessageUtil.showToast(getContext(), R.string.no_port);
                return;
            }

            if (!is_anonymous) {
                if (TextUtils.isEmpty(edt_username.getText().toString())) {
                    MessageUtil.showToast(getContext(), R.string.invalid_username);
                    return;
                }
                if (TextUtils.isEmpty(edt_password.getText().toString())) {
                    MessageUtil.showToast(getContext(), R.string.invalid_password);
                    return;
                }
            }

        }

        String name = edt_name.getText().toString().trim();
        String url = edt_url.getText().toString().trim();
        String uri = edt_uri.getText().toString().trim();
        int port = 0;
        if (!TextUtils.isEmpty(edt_port.getText().toString())) {
            port = Integer.parseInt(edt_port.getText().toString());
        }

        if (!url.contains("rtsp://") || url.equalsIgnoreCase("rtsp://") && !url.startsWith("rtsp://")) {
            MessageUtil.showToast(getContext(), R.string.invalid_url);
            return;
        }

        String username = edt_username.getText().toString().trim();
        String password = edt_password.getText().toString().trim();
        if (is_anonymous) {
            username = "";
            password = "";
        }

        int rtsp_type = radio_tcp.isChecked() ? AppConstant.RTSP_TCP : AppConstant.RTSP_UDP;
        String wifi_in = spinner_in.getSelectedItem().toString();
        String wifi_out = spinner_out.getSelectedItem().toString();
        Camera camera = new Camera(name, url, port, uri, username, password, ssid, rtsp_type, wifi_in, wifi_out, chk_full_address.isChecked(), wifi_password);
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
            this.camera.use_full_address = chk_full_address.isChecked();
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
