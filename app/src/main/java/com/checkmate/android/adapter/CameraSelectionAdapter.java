package com.checkmate.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.checkmate.android.R;
import com.checkmate.android.model.CameraSelectionModel;

import java.util.List;

public class CameraSelectionAdapter extends RecyclerView.Adapter<CameraSelectionAdapter.ViewHolder> {
    
    private List<CameraSelectionModel> cameraList;
    private OnCameraSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnCameraSelectedListener {
        void onCameraSelected(CameraSelectionModel camera, int position);
    }

    public CameraSelectionAdapter(List<CameraSelectionModel> cameraList, OnCameraSelectedListener listener) {
        this.cameraList = cameraList;
        this.listener = listener;
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
        CameraSelectionModel camera = cameraList.get(position);
        
        holder.txtCameraName.setText(camera.getName());
        holder.imgCameraIcon.setImageResource(camera.getIconResource());
        holder.rbCameraSelected.setChecked(position == selectedPosition);
        
        // Show status if available
        if (camera.getStatus() != null && !camera.getStatus().isEmpty()) {
            holder.txtCameraStatus.setText(camera.getStatus());
            holder.txtCameraStatus.setVisibility(View.VISIBLE);
        } else {
            holder.txtCameraStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPosition = selectedPosition;
            selectedPosition = position;
            
            // Update UI
            if (oldPosition != -1) {
                notifyItemChanged(oldPosition);
            }
            notifyItemChanged(selectedPosition);
            
            // Notify listener
            if (listener != null) {
                listener.onCameraSelected(camera, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cameraList != null ? cameraList.size() : 0;
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition);
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition);
        }
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCameraIcon;
        TextView txtCameraName;
        TextView txtCameraStatus;
        RadioButton rbCameraSelected;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCameraIcon = itemView.findViewById(R.id.img_camera_icon);
            txtCameraName = itemView.findViewById(R.id.txt_camera_name);
            txtCameraStatus = itemView.findViewById(R.id.txt_camera_status);
            rbCameraSelected = itemView.findViewById(R.id.rb_camera_selected);
        }
    }
}