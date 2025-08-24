package com.checkmate.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.database.FileStoreDb;
import com.checkmate.android.model.Media;
import com.checkmate.android.ui.view.DragListView;
import com.checkmate.android.ui.activity.ImageViewerActivity;
import com.checkmate.android.ui.activity.VideoPlayerActivity;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.ResourceUtil;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
import com.kongzue.dialogx.util.TextInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PlaybackFragment extends BaseFragment
        implements DragListView.OnRefreshLoadingMoreListener {

    public static final String ARG_TREE_URI = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
    public static WeakReference<PlaybackFragment> instance;

    private SharedViewModel sharedViewModel;
    private View mView;
    private boolean is_selectable = false;
    private boolean is_all_selected = false;
    private Uri treeUri;
    private List<Media> mDataList = new ArrayList<>();
    private ListAdapter adapter;
    private FileStoreDb fileStoreDb;

    // UI references
    DragListView list_view;
    TextView txt_select;
    TextView txt_no_data;
    TextView txt_select_all;
    TextView txt_delete;
    TextView tv_storage_path;
    TextView tv_storage_location;
    TextView tv_refresh_hint;

    public static PlaybackFragment newInstance() {
        PlaybackFragment fragment = new PlaybackFragment();
        Bundle args = new Bundle();
        String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
        
        // Only set treeUri if it's a valid content URI
        if (storage_location.startsWith("content://")) {
            Uri treeUri = Uri.parse(storage_location);
            args.putParcelable(ARG_TREE_URI, treeUri);
        }
        
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        instance = new WeakReference<>(this);
        fileStoreDb = new FileStoreDb(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (fileStoreDb != null) {
            fileStoreDb.close();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_playback, container, false);
        updateUI();
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            loadMediaFromAllSources();
            updateUI();
        }
    }

    public void updateUI() {
        initViews();
        handleArguments();
        initialize();
    }

    private void initViews() {
        list_view = mView.findViewById(R.id.list_view);
        txt_no_data = mView.findViewById(R.id.txt_no_data);
        txt_select = mView.findViewById(R.id.txt_select);
        txt_select_all = mView.findViewById(R.id.txt_select_all);
        txt_delete = mView.findViewById(R.id.txt_delete);
        tv_storage_path = mView.findViewById(R.id.tv_storage_path);
        tv_storage_location = mView.findViewById(R.id.tv_storage_location);
        tv_refresh_hint = mView.findViewById(R.id.tv_refresh_hint);

        txt_select.setOnClickListener(v -> onSelect());
        txt_delete.setOnClickListener(v -> onDelete());
        txt_select_all.setOnClickListener(v -> onSelectAll());
    }

    private void handleArguments() {
        if (getArguments() != null) {
            String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
            
            // Check if storage_location is a valid content URI or just a file path
            if (storage_location.startsWith("content://")) {
                treeUri = Uri.parse(storage_location);
            } else {
                // It's a file path, not a content URI - we can't use DocumentFile with this
                treeUri = null;
                tv_storage_path.setText(storage_location);
                tv_storage_location.setText("Storage Location: File Path (Limited Access)");
                return;
            }
            
            String strName = getFullPathFromTreeUri(treeUri);
            tv_storage_path.setText(strName);
            String storageType = AppPreference.getStr(AppPreference.KEY.Storage_Type, "Storage Location: Default Storage");
            tv_storage_location.setText(storageType);
        }
    }

    public String getFullPathFromTreeUri(Uri treeUri) {
        if (treeUri == null || treeUri.toString().isEmpty()) return null;
        if (treeUri.toString().contains("/0/")) {
            return treeUri.toString();
        }
        String docId = DocumentsContract.getTreeDocumentId(treeUri);
        String[] split = docId.split(":");
        String storageType = split[0];
        String relativePath = "";

        if (split.length >= 2) {
            relativePath = split[1];
        }
        return "/storage/" + storageType + "/" + relativePath;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity())
                .get(SharedViewModel.class);

        sharedViewModel.getEventLiveData().observe(getViewLifecycleOwner(), event -> {
            if (event != null) {
                SharedViewModel.EventPayload payload = event.getContentIfNotHandled();
                if (payload != null && payload.getEventType() == EventType.INIT_FUN_PLAY_BACK) {
                    initialize();
                }
            }
        });
    }

    private void initialize() {
        is_selectable = false;
        adapter = new ListAdapter(getActivity());
        list_view.setAdapter(adapter);
        list_view.setOnRefreshListener(this);
        
        // Load from all available sources
        loadMediaFromAllSources();

        list_view.setOnItemClickListener((adapterView, view, i, l) -> {
            Media media = mDataList.get(i - 1);
            AppPreference.setBool(AppPreference.KEY.IS_FOR_PLAYBACK_LOCATION, true);

            if (media.type == Media.TYPE.VIDEO) {
                openVideo(media);
            } else {
                openImage(media);
            }
        });
    }

    private void openVideo(Media media) {
        if (media.is_encrypted) {
            decryptAndOpen(media, "video/mp4");
        } else {
            Intent intent = new Intent(getActivity(), VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, media.contentUri.toString());
            intent.putExtra(VideoPlayerActivity.EXTRA_IS_ENCRYPTED, false);
            startActivity(intent);
        }
    }

    private void openImage(Media media) {
        if (media.is_encrypted) {
            decryptAndOpen(media, "image/*");
        } else {
            Intent intent = new Intent(getActivity(), ImageViewerActivity.class);
            intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URI, media.contentUri.toString());
            intent.putExtra(ImageViewerActivity.EXTRA_IS_ENCRYPTED, false);
            startActivity(intent);
        }
    }

    private void decryptAndOpen(Media media, String mimeType) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getActivity());
        builder.setTitle("Enter Password");
        final android.widget.EditText input = new android.widget.EditText(getActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            try {
                File tempDecryptedFile = File.createTempFile("temp_decrypted", ".mp4", getActivity().getCacheDir());
                boolean result = decryptFile(media.contentUri, tempDecryptedFile, password);
                if (result) {
                    Uri fileUri = FileProvider.getUriForFile(getActivity(), getActivity().getPackageName() + ".provider", tempDecryptedFile);
                    
                    // Launch appropriate in-app viewer based on mime type
                    Intent intent;
                    if (mimeType.startsWith("video/")) {
                        intent = new Intent(getActivity(), VideoPlayerActivity.class);
                        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, fileUri.toString());
                        intent.putExtra(VideoPlayerActivity.EXTRA_IS_ENCRYPTED, true);
                    } else {
                        intent = new Intent(getActivity(), ImageViewerActivity.class);
                        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URI, fileUri.toString());
                        intent.putExtra(ImageViewerActivity.EXTRA_IS_ENCRYPTED, true);
                    }
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } else {
                    MessageUtil.showToast(getActivity(), "Failed to decrypt");
                }
            } catch (IOException e) {
                e.printStackTrace();
                MessageUtil.showToast(getActivity(), e.getLocalizedMessage());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    public boolean decryptFile(Uri encryptedUri, File outputFile, String password) {
        try (InputStream fis = requireContext().getContentResolver().openInputStream(encryptedUri);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            if (fis == null) return false;

            byte[] salt = new byte[16];
            byte[] iv = new byte[16];
            if (fis.read(salt) != salt.length || fis.read(iv) != iv.length) {
                return false;
            }

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

            try (CipherInputStream cis = new CipherInputStream(fis, cipher)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public void onRefresh() {
        loadMediaFromAllSources();
    }

    @Override
    public void onDragRefresh() {
        loadMediaFromAllSources();
    }

    @Override
    public void onDragLoadMore() {}

    private void loadMediaFromAllSources() {
        txt_no_data.setVisibility(View.GONE);
        new LoadAllMediaTask().execute();
    }
    
    private void loadFromTreeUriSingleDirectory() {
        if (treeUri == null) {
            // Load from file paths instead
            loadMediaFromAllSources();
            return;
        }
        txt_no_data.setVisibility(View.GONE);
        new LoadDirectoryTask().execute(treeUri);
    }


    private class LoadDirectoryTask extends AsyncTask<Uri, Void, List<Media>> {
        @Override
        protected List<Media> doInBackground(Uri... uris) {
            List<Media> result = new ArrayList<>();
            if (treeUri == null || treeUri.toString().isEmpty()) {
                return result;
            }

            Context context = requireContext();
            DocumentFile rootDoc = DocumentFile.fromTreeUri(context, treeUri);
            if (rootDoc == null || !rootDoc.isDirectory()) {
                return result;
            }

            // Get existing records from DB
            Map<String, Media> dbMediaMap = new HashMap<>();
            List<Media> dbMediaList = fileStoreDb.getMediaForPath(treeUri.toString());
            for (Media media : dbMediaList) {
                // Use URI as key to ensure uniqueness
                dbMediaMap.put(media.contentUri.toString(), media);
            }

            // Create a set to track files we've found in directory
            Set<String> foundUris = new HashSet<>();

            // Scan directory
            DocumentFile[] children = rootDoc.listFiles();
            if (children == null) return result;

            for (DocumentFile doc : children) {
                if (doc.isDirectory()) continue;
                String name = doc.getName();
                if (TextUtils.isEmpty(name)) continue;

                Uri contentUri = doc.getUri();
                String uriString = contentUri.toString();
                foundUris.add(uriString);

                Media media = dbMediaMap.get(uriString);

                // Create new media if doesn't exist in DB
                if (media == null) {
                    media = new Media();
                    media.name = name;
                    media.date = new Date(doc.lastModified());
                    media.file = doc;
                    media.contentUri = contentUri;

                    String lowerName = name.toLowerCase();
                    if (lowerName.endsWith(".t3v")) {
                        media.type = Media.TYPE.VIDEO;
                        media.is_encrypted = true;
                    } else if (lowerName.endsWith(".t3j")) {
                        media.type = Media.TYPE.PHOTO;
                        media.is_encrypted = true;
                    } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") ||
                            lowerName.endsWith(".mkv") || lowerName.endsWith(".avi") ||
                            lowerName.endsWith(".3gp") || lowerName.endsWith(".webm")) {
                        media.type = Media.TYPE.VIDEO;
                    } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                            lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                            lowerName.endsWith(".bmp") || lowerName.endsWith(".webp")) {
                        media.type = Media.TYPE.IMAGE;
                    } else {
                        // Skip non-media files - only include VIDEO, PHOTO, and IMAGE types
                        continue;
                    }

                    // Extract and store metadata for non-encrypted files
                    if (!media.is_encrypted) {
                        extractAndStoreMetadata(context, media);
                    }

                    // Insert into database
                    fileStoreDb.logFile(
                            media.name,
                            uriString,
                            media.date.getTime(),
                            media.type == Media.TYPE.VIDEO ? "video" : 
                            media.type == Media.TYPE.IMAGE ? "image" : "photo",
                            media.is_encrypted,
                            media.duration,
                            media.resolutionWidth,
                            media.resolutionHeight,
                            media.fileSize
                    );

                    // Add to result list
                    result.add(media);
                } else {
                    // Update existing media object with current DocumentFile
                    media.file = doc;
                    result.add(media);
                }
            }

            // Add database records not found in current directory
            for (Media media : dbMediaMap.values()) {
                if (!foundUris.contains(media.contentUri.toString())) {
                    // Try to get DocumentFile from URI
                    DocumentFile doc = DocumentFile.fromSingleUri(context, Uri.parse(media.contentUri.toString()));
                    if (doc != null && doc.exists()) {
                        media.file = doc;
                        result.add(media);
                    } else {
                        // Remove from database if file no longer exists
                        fileStoreDb.deleteByPath(media.contentUri.toString());
                    }
                }
            }

            // Sort by date (newest first)
            result.sort((m1, m2) ->
                    Long.compare(m2.date.getTime(), m1.date.getTime()));

            return result;
        }

        private void extractAndStoreMetadata(Context context, Media media) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, media.contentUri);

                if (media.type == Media.TYPE.VIDEO) {
                    String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    media.duration = durationStr != null ? Long.parseLong(durationStr) : 0;
                }

                String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                if (widthStr == null) {
                    widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH);
                }

                String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                if (heightStr == null) {
                    heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT);
                }

                media.resolutionWidth = widthStr != null ? Integer.parseInt(widthStr) : 0;
                media.resolutionHeight = heightStr != null ? Integer.parseInt(heightStr) : 0;

                // Get file size
                try (Cursor cursor = context.getContentResolver().query(
                        media.contentUri,
                        new String[]{OpenableColumns.SIZE},
                        null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                        if (sizeIndex != -1) {
                            media.fileSize = cursor.getLong(sizeIndex);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("Metadata", "Error extracting metadata", e);
            }
        }

        @Override
        protected void onPostExecute(List<Media> result) {
            mDataList.clear();
            mDataList.addAll(result);
            adapter.notifyDataSetChanged();
            txt_no_data.setVisibility(mDataList.isEmpty() ? View.VISIBLE : View.GONE);
            list_view.onRefreshComplete();
        }
    }

    private class LoadAllMediaTask extends AsyncTask<Void, Void, List<Media>> {
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        
        @Override
        protected List<Media> doInBackground(Void... voids) {
            List<Media> result = new ArrayList<>();
            
            try {
                // Only load from database (SAF and file paths) - ignore MediaStore
                result.addAll(loadFromSAF());
                result.addAll(loadFromFilePaths());
                
                // Sort by date (newest first)
                if (!result.isEmpty()) {
                    Collections.sort(result, (m1, m2) -> Long.compare(m2.date.getTime(), m1.date.getTime()));
                }
                
            } catch (Exception e) {
                Log.e("LoadAllMedia", "Error loading media", e);
            }
            
            return result;
        }
        
        private List<Media> loadFromSAF() {
            List<Media> result = new ArrayList<>();
            
            // Check if treeUri is null or invalid
            if (treeUri == null) {
                Log.w("LoadSAF", "treeUri is null, skipping SAF loading");
                return result;
            }
            
            try {
                Context context = requireContext();
                DocumentFile rootDoc = DocumentFile.fromTreeUri(context, treeUri);
                if (rootDoc == null || !rootDoc.isDirectory()) {
                    Log.w("LoadSAF", "rootDoc is null or not a directory");
                    return result;
                }

                // Get existing records from DB
                Map<String, Media> dbMediaMap = new HashMap<>();
                List<Media> dbMediaList = fileStoreDb.getMediaForPath(treeUri.toString());
                for (Media media : dbMediaList) {
                    dbMediaMap.put(media.contentUri.toString(), media);
                }

                // Scan directory
                DocumentFile[] children = rootDoc.listFiles();
                if (children == null) return result;

                for (DocumentFile doc : children) {
                    if (doc.isDirectory()) continue;
                    String name = doc.getName();
                    if (TextUtils.isEmpty(name)) continue;

                    Uri contentUri = doc.getUri();
                    String uriString = contentUri.toString();

                    Media media = dbMediaMap.get(uriString);
                    if (media == null) {
                        media = new Media();
                        media.name = name;
                        media.contentUri = contentUri;
                        media.path = treeUri.toString();
                        media.date = new Date(doc.lastModified());
                        media.file = doc;

                        // Determine media type - only include VIDEO and IMAGE types
                        if (isVideoFile(name)) {
                            media.type = Media.TYPE.VIDEO;
                            extractVideoMetadata(media);
                        } else if (isImageFile(name)) {
                            media.type = Media.TYPE.IMAGE;
                        } else {
                            // Skip non-media files
                            continue;
                        }

                        // Check if encrypted
                        media.is_encrypted = name.endsWith(".enc");

                        // Save to database
                        long id = fileStoreDb.insert(media);
                        media.id = id;
                    }
                    result.add(media);
                }
                
            } catch (Exception e) {
                Log.e("LoadSAF", "Error loading from SAF", e);
            }
            
            return result;
        }
        
        private List<Media> loadFromFilePaths() {
            List<Media> result = new ArrayList<>();
            
            try {
                String storagePath = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
                
                // If storage path is not a SAF URI, treat it as a file path
                if (!storagePath.startsWith("content://") && !storagePath.isEmpty()) {
                    File directory = new File(storagePath);
                    if (directory.exists() && directory.isDirectory()) {
                        File[] files = directory.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile()) {
                                    String name = file.getName();
                                    if (isImageFile(name) || isVideoFile(name)) {
                                        Media media = new Media();
                                        media.name = name;
                                        media.contentUri = Uri.fromFile(file);
                                        media.path = storagePath;
                                        media.date = new Date(file.lastModified());
                                        media.fileSize = file.length();
                                        
                                        // Determine media type - only include VIDEO and IMAGE types
                                        if (isVideoFile(name)) {
                                            media.type = Media.TYPE.VIDEO;
                                            extractVideoMetadata(media);
                                        } else if (isImageFile(name)) {
                                            media.type = Media.TYPE.IMAGE;
                                        } else {
                                            // Skip non-media files
                                            continue;
                                        }
                                        
                                        // Check if encrypted
                                        media.is_encrypted = name.endsWith(".enc");
                                        
                                        result.add(media);
                                    }
                                }
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                Log.e("LoadFilePaths", "Error loading from file paths", e);
            }
            
            return result;
        }
        
        private boolean isImageFile(String fileName) {
            String ext = fileName.toLowerCase();
            return ext.endsWith(".jpg") || ext.endsWith(".jpeg") || ext.endsWith(".png") || 
                   ext.endsWith(".gif") || ext.endsWith(".bmp") || ext.endsWith(".webp") ||
                   ext.endsWith(".jpg.enc") || ext.endsWith(".jpeg.enc") || ext.endsWith(".png.enc");
        }
        
        private boolean isVideoFile(String fileName) {
            String ext = fileName.toLowerCase();
            return ext.endsWith(".mp4") || ext.endsWith(".avi") || ext.endsWith(".mkv") || 
                   ext.endsWith(".mov") || ext.endsWith(".wmv") || ext.endsWith(".flv") ||
                   ext.endsWith(".webm") || ext.endsWith(".3gp") || ext.endsWith(".mp4.enc");
        }
        
        private void extractVideoMetadata(Media media) {
            try (Cursor cursor = requireContext().getContentResolver().query(media.contentUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        media.fileSize = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                Log.e("Metadata", "Error extracting metadata", e);
            }
        }

        @Override
        protected void onPostExecute(List<Media> result) {
            mDataList.clear();
            mDataList.addAll(result);
            adapter.notifyDataSetChanged();
            
            // Center the no data message and make it more prominent
            if (mDataList.isEmpty()) {
                txt_no_data.setVisibility(View.VISIBLE);
                txt_no_data.setText("No media files found\nPull down to refresh");
                list_view.setVisibility(View.GONE);
            } else {
                txt_no_data.setVisibility(View.GONE);
                list_view.setVisibility(View.VISIBLE);
            }
            
            list_view.onRefreshComplete();
        }
        
        private void updateStorageLocationDisplay(List<Media> mediaList) {
            int safCount = 0;
            int filePathCount = 0;
            int mediaStoreCount = 0;
            
            for (Media media : mediaList) {
                if (media.path != null) {
                    if (media.path.startsWith("content://")) {
                        safCount++;
                    } else if (media.path.equals("MediaStore")) {
                        mediaStoreCount++;
                    } else {
                        filePathCount++;
                    }
                }
            }
            
            StringBuilder displayText = new StringBuilder();
            displayText.append("Multiple Sources: ");
            
            if (safCount > 0) {
                displayText.append("SAF (").append(safCount).append(") ");
            }
            if (filePathCount > 0) {
                displayText.append("Files (").append(filePathCount).append(") ");
            }
            if (mediaStoreCount > 0) {
                displayText.append("MediaStore (").append(mediaStoreCount).append(")");
            }
            
            tv_storage_location.setText(displayText.toString());
            
            // Show combined path info
            String storagePath = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
            if (!storagePath.isEmpty()) {
                if (storagePath.startsWith("content://")) {
                    tv_storage_path.setText("SAF: " + getFullPathFromTreeUri(treeUri));
                } else {
                    tv_storage_path.setText("Path: " + storagePath);
                }
            } else {
                tv_storage_path.setText("All available storage locations");
            }
        }
    }

    private void onSelect() {
        is_selectable = !is_selectable;
        for (Media media : mDataList) {
            media.is_selected = false;
        }
        txt_delete.setTextColor(is_selectable ?
                getResources().getColor(R.color.black) :
                getResources().getColor(R.color.gray_dark));
        adapter.notifyDataSetChanged();
    }

    private void onSelectAll() {
        if (!is_selectable) return;
        for (Media media : mDataList) {
            media.is_selected = !is_all_selected;
        }
        is_all_selected = !is_all_selected;
        adapter.notifyDataSetChanged();
    }

    private void onDelete() {
        if (!is_selectable) return;
        boolean anySelected = false;
        for (Media media : mDataList) {
            if (media.is_selected) {
                anySelected = true;
                break;
            }
        }
        if (!anySelected) return;

        MessageDialog.show(getString(R.string.delete), getString(R.string.confirm_delete_multi),
                        getString(R.string.Okay), getString(R.string.cancel))
                .setCancelButton((dialog, view) -> false)
                .setOkButton((baseDialog, view) -> {
                    for (Media media : mDataList) {
                        if (media.is_selected && media.file != null) {
                            media.file.delete();
                            fileStoreDb.deleteByPath(media.contentUri.toString());
                        }
                    }
                    list_view.refresh();
                    return false;
                });
    }

    private class ListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;

        ListAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() { return mDataList.size(); }

        @Override
        public Object getItem(int position) { return mDataList.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        class ViewHolder {
            ImageView img_thumbnail;
            TextView txt_name, txt_type, txt_date, txt_time, txt_duration;
            ImageView ic_share, ic_trash;
            CheckBox checkbox;

            ViewHolder(View convertView) {
                img_thumbnail = convertView.findViewById(R.id.img_thumbnail);
                txt_name = convertView.findViewById(R.id.txt_name);
                txt_type = convertView.findViewById(R.id.txt_type);
                txt_date = convertView.findViewById(R.id.txt_date);
                txt_time = convertView.findViewById(R.id.txt_time);
                txt_duration = convertView.findViewById(R.id.txt_duration);
                ic_share = convertView.findViewById(R.id.ic_share);
                ic_trash = convertView.findViewById(R.id.ic_trash);
                checkbox = convertView.findViewById(R.id.checkbox);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_media, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder = (ViewHolder) convertView.getTag();
            final Media media = mDataList.get(position);
            holder.txt_name.setText(media.name);
            holder.txt_date.setText(ResourceUtil.date(media.date.getTime()));
            holder.txt_time.setText(ResourceUtil.time(media.date.getTime()));

            if (media.type == Media.TYPE.VIDEO) {
                holder.txt_type.setText(R.string.video);
                if (media.is_encrypted) {
                    holder.img_thumbnail.setImageResource(R.mipmap.ic_lock);
                    holder.txt_duration.setText(R.string.encrypted);
                } else {
                    Glide.with(requireActivity())
                            .load(media.contentUri)
                            .thumbnail(0.1f)
                            .into(holder.img_thumbnail);

                    if (media.duration > 0) {
                        String duration = String.format(Locale.getDefault(), "%02d:%02d",
                                (media.duration / 1000) / 60,
                                (media.duration / 1000) % 60);
                        holder.txt_duration.setText(duration);
                    } else {
                        holder.txt_duration.setText("--:--");
                    }
                }
                holder.txt_duration.setVisibility(View.VISIBLE);
            } else if (media.type == Media.TYPE.IMAGE) {
                holder.txt_type.setText("Image");
                holder.txt_duration.setVisibility(View.GONE);
                if (media.is_encrypted) {
                    holder.img_thumbnail.setImageResource(R.mipmap.ic_lock);
                } else {
                    Glide.with(getActivity())
                            .load(media.contentUri)
                            .thumbnail(0.1f)
                            .into(holder.img_thumbnail);
                }
            } else {
                // Handle PHOTO type (legacy)
                holder.txt_type.setText(R.string.photo);
                holder.txt_duration.setVisibility(View.GONE);
                if (media.is_encrypted) {
                    holder.img_thumbnail.setImageResource(R.mipmap.ic_lock);
                } else {
                    Glide.with(getActivity())
                            .load(media.contentUri)
                            .thumbnail(0.1f)
                            .into(holder.img_thumbnail);
                }
            }

            holder.checkbox.setVisibility(is_selectable ? View.VISIBLE : View.GONE);
            holder.checkbox.setChecked(media.is_selected);
            holder.checkbox.setOnCheckedChangeListener((btn, isChecked) ->
                    media.is_selected = isChecked);

            holder.ic_share.setOnClickListener(v -> {
                try {
                    String mimeType = media.type == Media.TYPE.VIDEO ? "video/mp4" : "image/*";
                    
                    // Use FileProvider for safe sharing
                    Uri shareUri;
                    if (media.contentUri.getScheme().equals("file")) {
                        // Convert file URI to content URI using FileProvider
                        shareUri = FileProvider.getUriForFile(
                            requireActivity(),
                            requireActivity().getPackageName() + ".provider",
                            new File(media.contentUri.getPath())
                        );
                    } else {
                        // Already a content URI, use as is
                        shareUri = media.contentUri;
                    }
                    
                    ShareCompat.IntentBuilder.from(requireActivity())
                            .setType(mimeType)
                            .setStream(shareUri)
                            .setChooserTitle("Share media...")
                            .startChooser();
                } catch (Exception e) {
                    Log.e("Share", "Error sharing media", e);
                    // Fallback to simple text sharing
                    ShareCompat.IntentBuilder.from(requireActivity())
                            .setType("text/plain")
                            .setText("Media: " + media.name)
                            .setChooserTitle("Share media...")
                            .startChooser();
                }
            });

            holder.ic_trash.setOnClickListener(v ->
                    MessageDialog.show(getString(R.string.delete), getString(R.string.confirm_delete),
                                    getString(R.string.Okay), getString(R.string.cancel))
                            .setCancelButton((dialog, view) -> false)
                            .setOkButton((baseDialog, view) -> {
                                if (media.file != null) {
                                    media.file.delete();
                                    fileStoreDb.deleteByPath(media.contentUri.toString());
                                    list_view.refresh();
                                }
                                return false;
                            }));

            return convertView;
        }
    }

    public interface ActivityFragmentCallbacks {
        void isDialog(boolean isShow);
        void showDialog();
        void dismissDialog();
    }
}