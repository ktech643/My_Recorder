package com.checkmate.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.checkmate.android.R;
import com.checkmate.android.database.CustomListAdapter;

import java.util.Arrays;
import java.util.List;

public class UsbPopupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_popup);

        if (getWindow() != null) {
            int margin = getResources().getDimensionPixelSize(R.dimen.dialog_margin); // Define in dimens.xml
            getWindow().setLayout(getResources().getDisplayMetrics().widthPixels - (2 * margin), ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Sample data
        String[] namesArray = getIntent().getStringArrayExtra("list");

        List<String> items = Arrays.asList(namesArray);

        ListView listView = findViewById(R.id.listView);
        CustomListAdapter adapter = new CustomListAdapter(this, items);
        listView.setAdapter(adapter);

        // Handle item selection
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedIndex", position);
            setResult(RESULT_OK, resultIntent);
            finish(); // Close the activity after selection
        });

    }
}