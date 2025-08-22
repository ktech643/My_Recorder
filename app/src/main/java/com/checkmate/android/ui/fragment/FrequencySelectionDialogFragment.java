package com.checkmate.android.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.checkmate.android.R;
import com.kongzue.dialogx.dialogs.BottomDialog;
import com.kongzue.dialogx.interfaces.OnBindView;

public class FrequencySelectionDialogFragment extends Fragment {

    // Define a callback interface
    public interface FrequencySelectionListener {
        void onFrequencySelected(String frequency);
    }

    private FrequencySelectionListener listener;

    // Setter for the listener
    public void setFrequencySelectionListener(FrequencySelectionListener listener) {
        this.listener = listener;
    }

    public void showFrequencySelectionDialog() {
        BottomDialog.show(new OnBindView<BottomDialog>(R.layout.dialog_frequency_selection) {
            @Override
            public void onBind(BottomDialog dialog, View v) {
                // Find buttons in the layout
                Button btnOption1 = v.findViewById(R.id.btnOption1);
                Button btnOption5 = v.findViewById(R.id.btnOption5);
                Button btnOption10 = v.findViewById(R.id.btnOption10);
                Button btnOption15 = v.findViewById(R.id.btnOption15);
                Button btnOption30 = v.findViewById(R.id.btnOption30);
                Button btnCancel = v.findViewById(R.id.btnCancel);

                // Set a common click listener for all buttons
                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String frequency = ((Button) v).getText().toString();
                        if (listener != null) {
                            listener.onFrequencySelected(frequency);
                        }
                        dialog.dismiss();
                    }
                };

                btnOption1.setOnClickListener(clickListener);
                btnOption5.setOnClickListener(clickListener);
                btnOption10.setOnClickListener(clickListener);
                btnOption15.setOnClickListener(clickListener);
                btnOption30.setOnClickListener(clickListener);

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });
            }
        }).setCancelable(true);
    }
}