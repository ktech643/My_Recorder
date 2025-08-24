package com.checkmate.android.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.checkmate.android.R;
import com.checkmate.android.ui.fragment.StreamingFragment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class StreamingModeBottomSheet extends BottomSheetDialogFragment {

    private StreamingModeListener listener;
    private List<StreamingModeOption> options;

    public interface StreamingModeListener {
        void onStreamingModeSelected(StreamingModeOption option);
    }

    public static class StreamingModeOption {
        public String title;
        public String description;
        public int modeValue;
        public boolean isSelected;

        public StreamingModeOption(String title, String description, int modeValue, boolean isSelected) {
            this.title = title;
            this.description = description;
            this.modeValue = modeValue;
            this.isSelected = isSelected;
        }
    }

    public static StreamingModeBottomSheet newInstance(int currentMode) {
        StreamingModeBottomSheet fragment = new StreamingModeBottomSheet();
        Bundle args = new Bundle();
        args.putInt("current_mode", currentMode);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(StreamingModeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog);
        
        // Initialize options
        int currentMode = getArguments() != null ? getArguments().getInt("current_mode", 0) : 0;
        options = new ArrayList<>();
        options.add(new StreamingModeOption("Cloud Streaming", "Stream to cloud servers", 0, currentMode == 0));
        options.add(new StreamingModeOption("Local Streaming", "Stream to local network", 1, currentMode == 1));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_streaming_mode, container, false);
        
        RecyclerView recyclerView = view.findViewById(R.id.recycler_streaming_modes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new StreamingModeAdapter());
        
        return view;
    }

    private class StreamingModeAdapter extends RecyclerView.Adapter<StreamingModeAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_streaming_mode, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StreamingModeOption option = options.get(position);
            boolean isLastItem = position == options.size() - 1;
            holder.bind(option, isLastItem);
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTitle;
            private TextView tvDescription;
            private ImageView ivCheck;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_title);
                tvDescription = itemView.findViewById(R.id.tv_description);
                ivCheck = itemView.findViewById(R.id.iv_check);
            }

            void bind(StreamingModeOption option, boolean isLastItem) {
                tvTitle.setText(option.title);
                tvDescription.setText(option.description);
                ivCheck.setVisibility(option.isSelected ? View.VISIBLE : View.GONE);

                // Remove bottom margin for the last item to eliminate bottom padding
                if (isLastItem) {
                    itemView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                }

                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onStreamingModeSelected(option);
                    }
                    dismiss();
                });
            }
        }
    }
}
