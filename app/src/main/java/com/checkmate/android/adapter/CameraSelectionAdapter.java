package com.checkmate.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.checkmate.android.R;
import java.util.List;

public class CameraSelectionAdapter extends RecyclerView.Adapter<CameraSelectionAdapter.ViewHolder> {
    
    private List<String> cameraOptions;
    private OnItemClickListener listener;
    private String currentSelection = "";
    
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    
    public CameraSelectionAdapter(List<String> cameraOptions, OnItemClickListener listener) {
        this.cameraOptions = cameraOptions;
        this.listener = listener;
    }
    
    public void setCurrentSelection(String selection) {
        this.currentSelection = selection;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_camera_selection, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String cameraOption = cameraOptions.get(position);
        holder.tvCameraName.setText(cameraOption);
        
        // Set icon based on camera type
        if (cameraOption.contains("Rear") || cameraOption.contains("rear")) {
            holder.ivCameraIcon.setImageResource(R.mipmap.ic_camera);
        } else if (cameraOption.contains("Front") || cameraOption.contains("front")) {
            holder.ivCameraIcon.setImageResource(R.mipmap.ic_camera);
        } else if (cameraOption.contains("USB") || cameraOption.contains("usb")) {
            holder.ivCameraIcon.setImageResource(R.mipmap.ic_camera);
        } else if (cameraOption.contains("Screen") || cameraOption.contains("Cast")) {
            holder.ivCameraIcon.setImageResource(R.mipmap.ic_camera);
        } else if (cameraOption.contains("Audio") || cameraOption.contains("audio")) {
            holder.ivCameraIcon.setImageResource(R.mipmap.ic_camera);
        } else {
            // WiFi camera
            holder.ivCameraIcon.setImageResource(R.mipmap.ic_camera);
        }
        
        // Show selection indicator
        boolean isSelected = cameraOption.equals(currentSelection) || 
                           (currentSelection.equals("Rear Camera") && cameraOption.contains("Rear")) ||
                           (currentSelection.equals("Front Camera") && cameraOption.contains("Front")) ||
                           (currentSelection.equals("USB Camera") && cameraOption.contains("USB")) ||
                           (currentSelection.equals("Screen Cast") && cameraOption.contains("Screen")) ||
                           (currentSelection.equals("Audio Only") && cameraOption.contains("Audio"));
        
        holder.ivSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return cameraOptions != null ? cameraOptions.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCameraIcon;
        TextView tvCameraName;
        ImageView ivSelected;
        
        ViewHolder(View itemView) {
            super(itemView);
            ivCameraIcon = itemView.findViewById(R.id.iv_camera_icon);
            tvCameraName = itemView.findViewById(R.id.tv_camera_name);
            ivSelected = itemView.findViewById(R.id.iv_selected);
        }
    }
}