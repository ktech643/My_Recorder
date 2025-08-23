package com.checkmate.android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.checkmate.android.R;
import com.checkmate.android.model.Media;
import com.checkmate.android.ui.activity.ImageViewerActivity;
import com.checkmate.android.ui.activity.VideoPlayerActivity;
import com.checkmate.android.util.ResourceUtil;
import com.kongzue.dialogx.dialogs.MessageDialog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModernMediaAdapter extends RecyclerView.Adapter<ModernMediaAdapter.MediaViewHolder> {
    
    private final Context context;
    private final List<Media> mediaList;
    private boolean isSelectable = false;
    private OnDeleteClickListener deleteClickListener;
    private OnLoadMoreListener loadMoreListener;
    
    public interface OnDeleteClickListener {
        void onDeleteClick(Media media);
        void onDeleteMultiple(List<Media> selectedMedia);
    }
    
    public interface OnLoadMoreListener {
        void onLoadMore();
    }
    
    public ModernMediaAdapter(Context context) {
        this.context = context;
        this.mediaList = new ArrayList<>();
    }
    
    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }
    
    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }
    
    public void updateData(List<Media> newMediaList) {
        this.mediaList.clear();
        this.mediaList.addAll(newMediaList);
        notifyDataSetChanged();
    }
    
    public void addData(List<Media> moreMediaList) {
        int startPosition = this.mediaList.size();
        this.mediaList.addAll(moreMediaList);
        notifyItemRangeInserted(startPosition, moreMediaList.size());
    }
    
    public void setSelectable(boolean selectable) {
        this.isSelectable = selectable;
        // Clear all selections when switching modes
        for (Media media : mediaList) {
            media.is_selected = false;
        }
        notifyDataSetChanged();
    }
    
    public boolean isSelectable() {
        return isSelectable;
    }
    
    public List<Media> getSelectedMedia() {
        List<Media> selected = new ArrayList<>();
        for (Media media : mediaList) {
            if (media.is_selected) {
                selected.add(media);
            }
        }
        return selected;
    }
    
    public void selectAll(boolean selectAll) {
        for (Media media : mediaList) {
            media.is_selected = selectAll;
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.modern_media_item, parent, false);
        return new MediaViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        Media media = mediaList.get(position);
        holder.bind(media);
        
        // Trigger load more when approaching end of list
        if (position >= mediaList.size() - 3 && loadMoreListener != null) {
            loadMoreListener.onLoadMore();
        }
    }
    
    @Override
    public int getItemCount() {
        return mediaList.size();
    }
    
    class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imgThumbnail;
        private final TextView txtName;
        private final TextView txtType;
        private final TextView txtDate;
        private final TextView txtTime;
        private final TextView txtDuration;
        private final TextView txtFileSize;
        private final TextView txtResolution;
        private final ImageView icShare;
        private final ImageView icTrash;
        private final ImageView icLock;
        private final CheckBox checkbox;
        
        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            txtName = itemView.findViewById(R.id.txt_name);
            txtType = itemView.findViewById(R.id.txt_type);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtTime = itemView.findViewById(R.id.txt_time);
            txtDuration = itemView.findViewById(R.id.txt_duration);
            txtFileSize = itemView.findViewById(R.id.txt_file_size);
            txtResolution = itemView.findViewById(R.id.txt_resolution);
            icShare = itemView.findViewById(R.id.ic_share);
            icTrash = itemView.findViewById(R.id.ic_trash);
            icLock = itemView.findViewById(R.id.ic_lock);
            checkbox = itemView.findViewById(R.id.checkbox);
        }
        
        public void bind(Media media) {
            // Set basic info
            txtName.setText(media.name);
            txtDate.setText(ResourceUtil.date(media.date));
            txtTime.setText(ResourceUtil.time(media.date));
            
            // Set file size
            if (media.fileSize > 0) {
                txtFileSize.setText(formatFileSize(media.fileSize));
                txtFileSize.setVisibility(View.VISIBLE);
            } else {
                txtFileSize.setVisibility(View.GONE);
            }
            
            // Set resolution info
            if (media.resolutionWidth > 0 && media.resolutionHeight > 0) {
                txtResolution.setText(media.resolutionWidth + "×" + media.resolutionHeight);
                txtResolution.setVisibility(View.VISIBLE);
            } else {
                txtResolution.setVisibility(View.GONE);
            }
            
            // Handle media type specific setup
            if (media.type == Media.TYPE.VIDEO) {
                txtType.setText(R.string.video);
                txtType.setBackgroundResource(R.color.red);
                
                if (media.is_encrypted) {
                    imgThumbnail.setImageResource(R.mipmap.ic_lock);
                    icLock.setVisibility(View.VISIBLE);
                    txtDuration.setText(R.string.encrypted);
                    txtDuration.setVisibility(View.VISIBLE);
                } else {
                    icLock.setVisibility(View.GONE);
                    Glide.with(context)
                            .load(media.contentUri)
                            .thumbnail(0.1f)
                            .into(imgThumbnail);
                    
                    if (media.duration > 0) {
                        String duration = String.format(Locale.getDefault(), "%02d:%02d",
                                (media.duration / 1000) / 60,
                                (media.duration / 1000) % 60);
                        txtDuration.setText(duration);
                        txtDuration.setVisibility(View.VISIBLE);
                    } else {
                        txtDuration.setVisibility(View.GONE);
                    }
                }
            } else {
                txtType.setText(R.string.photo);
                txtType.setBackgroundResource(android.R.color.holo_blue_light);
                txtDuration.setVisibility(View.GONE);
                
                if (media.is_encrypted) {
                    imgThumbnail.setImageResource(R.mipmap.ic_lock);
                    icLock.setVisibility(View.VISIBLE);
                } else {
                    icLock.setVisibility(View.GONE);
                    Glide.with(context)
                            .load(media.contentUri)
                            .thumbnail(0.1f)
                            .into(imgThumbnail);
                }
            }
            
            // Handle selection mode
            checkbox.setVisibility(isSelectable ? View.VISIBLE : View.GONE);
            checkbox.setChecked(media.is_selected);
            checkbox.setOnCheckedChangeListener((button, isChecked) -> media.is_selected = isChecked);
            
            // Setup click listeners
            itemView.setOnClickListener(v -> {
                if (!isSelectable) {
                    openMedia(media);
                } else {
                    media.is_selected = !media.is_selected;
                    checkbox.setChecked(media.is_selected);
                }
            });
            
            icShare.setOnClickListener(v -> shareMedia(media));
            icTrash.setOnClickListener(v -> deleteMedia(media));
        }
        
        private void openMedia(Media media) {
            if (media.type == Media.TYPE.VIDEO) {
                openVideo(media);
            } else {
                openImage(media);
            }
        }
        
        private void openVideo(Media media) {
            if (media.is_encrypted) {
                // For encrypted videos, we should handle decryption first
                // For now, show a toast indicating that encryption support is being added
                android.widget.Toast.makeText(context, "Encrypted video support coming soon", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(context, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, media.contentUri.toString());
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_NAME, media.name);
            intent.putExtra(VideoPlayerActivity.EXTRA_FILE_SIZE, media.fileSize);
            if (media.date != null) {
                intent.putExtra(VideoPlayerActivity.EXTRA_DATE, media.date.getTime());
            }
            context.startActivity(intent);
        }
        
        private void openImage(Media media) {
            if (media.is_encrypted) {
                // For encrypted images, we should handle decryption first
                // For now, show a toast indicating that encryption support is being added
                android.widget.Toast.makeText(context, "Encrypted image support coming soon", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            Intent intent = new Intent(context, ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URI, media.contentUri.toString());
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_NAME, media.name);
            intent.putExtra(ImageViewerActivity.EXTRA_FILE_SIZE, media.fileSize);
            if (media.date != null) {
                intent.putExtra(ImageViewerActivity.EXTRA_DATE, media.date.getTime());
            }
            
            String imageInfo = ResourceUtil.date(media.date) + " • " + ResourceUtil.time(media.date);
            if (media.resolutionWidth > 0 && media.resolutionHeight > 0) {
                imageInfo += " • " + media.resolutionWidth + "×" + media.resolutionHeight;
            }
            if (media.fileSize > 0) {
                imageInfo += " • " + formatFileSize(media.fileSize);
            }
            
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_INFO, imageInfo);
            context.startActivity(intent);
        }
        
        private void shareMedia(Media media) {
            String mimeType = media.type == Media.TYPE.VIDEO ? "video/mp4" : "image/*";
            ShareCompat.IntentBuilder.from((androidx.appcompat.app.AppCompatActivity) context)
                    .setType(mimeType)
                    .setStream(media.contentUri)
                    .setChooserTitle("Share media...")
                    .startChooser();
        }
        
        private void deleteMedia(Media media) {
            MessageDialog.show(context.getString(R.string.delete), context.getString(R.string.confirm_delete),
                            context.getString(R.string.Okay), context.getString(R.string.cancel))
                    .setCancelButton((dialog, view) -> false)
                    .setOkButton((baseDialog, view) -> {
                        if (deleteClickListener != null) {
                            deleteClickListener.onDeleteClick(media);
                        }
                        return false;
                    });
        }
    }
    
    private String formatFileSize(long sizeInBytes) {
        if (sizeInBytes == 0) return "0 B";
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeInBytes) / Math.log10(1024));
        
        DecimalFormat decimalFormat = new DecimalFormat("#,##0.#");
        return decimalFormat.format(sizeInBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}