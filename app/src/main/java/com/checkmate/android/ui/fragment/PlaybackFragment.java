package com.checkmate.android.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.checkmate.android.model.Media;
import com.checkmate.android.ui.view.DragListView;
import com.checkmate.android.util.DefaultStorageUtils;
import com.checkmate.android.util.MessageUtil;
import com.checkmate.android.util.ResourceUtil;
import com.checkmate.android.viewmodels.EventType;
import com.checkmate.android.viewmodels.SharedViewModel;
import com.checkmate.android.ui.activity.ImageViewerActivity;
import com.checkmate.android.ui.activity.VideoPlayerActivity;
import com.checkmate.android.service.FragmentVisibilityListener;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PlaybackFragment extends BaseFragment
        implements DragListView.OnRefreshLoadingMoreListener, FragmentVisibilityListener {

    public static final String ARG_TREE_URI = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");
    public static WeakReference<PlaybackFragment> instance;

    private SharedViewModel sharedViewModel;
    private View mView;
    private boolean is_selectable = false;
    private boolean is_all_selected = false;
    private Uri treeUri;
    private List<Media> mDataList = new ArrayList<>();
    private ListAdapter adapter;

    // UI references
    DragListView list_view;
    TextView txt_select;
    TextView txt_no_data;
    TextView txt_select_all;
    TextView txt_delete;
    TextView tv_storage_path;
    TextView tv_storage_location;

    public static PlaybackFragment newInstance() {
        PlaybackFragment fragment = new PlaybackFragment();
        Bundle args = new Bundle();
        String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");

        // Validate storage location before parsing as URI
        if (storage_location == null || storage_location.isEmpty()) {
            // Set a default storage path if none is configured
            String defaultPath = DefaultStorageUtils.getDefaultStoragePath();
            AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultPath);
            storage_location = defaultPath;
        }

        // Only parse as URI if it's a valid SAF URI, otherwise use as file path
        Uri treeUri;
        if (isValidSAFUri(storage_location)) {
            treeUri = Uri.parse(storage_location);
        } else {
            // For file paths, create a file URI
            treeUri = Uri.fromFile(new File(storage_location));
        }

        args.putParcelable(ARG_TREE_URI, treeUri);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Check if the given string is a valid SAF URI
     */
    private static boolean isValidSAFUri(String uriString) {
        if (uriString == null || uriString.isEmpty()) {
            return false;
        }

        try {
            Uri uri = Uri.parse(uriString);
            // Check if it's a valid SAF URI (content:// scheme)
            if (!"content".equals(uri.getScheme())) {
                return false;
            }

            // Validate that it's a tree URI
            if (!uri.toString().contains("/tree/")) {
                return false;
            }

            // Try to get the document ID to validate
            DocumentsContract.getTreeDocumentId(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        instance = new WeakReference<>(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
            list_view.refresh();
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

        txt_select.setOnClickListener(v -> onSelect());
        txt_delete.setOnClickListener(v -> onDelete());
        txt_select_all.setOnClickListener(v -> onSelectAll());
    }

    private void handleArguments() {
        if (getArguments() != null) {
            String storage_location = AppPreference.getStr(AppPreference.KEY.STORAGE_LOCATION, "");

            // Validate and set default storage if empty
            if (storage_location == null || storage_location.isEmpty()) {
                String defaultPath = DefaultStorageUtils.getDefaultStoragePath();
                AppPreference.setStr(AppPreference.KEY.STORAGE_LOCATION, defaultPath);
                storage_location = defaultPath;
            }

            // Handle storage path based on type
            if (isValidSAFUri(storage_location)) {
                treeUri = Uri.parse(storage_location);
                String strName = getFullPathFromTreeUri(treeUri);
                tv_storage_path.setText(strName != null ? strName : "SAF Storage Selected");
            } else {
                // Handle file path
                treeUri = Uri.fromFile(new File(storage_location));
                tv_storage_path.setText(storage_location);
            }

            String storageType = AppPreference.getStr(AppPreference.KEY.Storage_Type, "Storage Location: Default Storage");
            tv_storage_location.setText(storageType);
        }
    }

    public String getFullPathFromTreeUri(Uri treeUri) {
        if (treeUri == null || treeUri.toString().isEmpty()) return null;

        // Handle file URIs (legacy file paths)
        if ("file".equals(treeUri.getScheme())) {
            return treeUri.getPath();
        }

        // Handle SAF URIs
        if (treeUri.toString().contains("/0/")) {
            return treeUri.toString();
        }

        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            String[] split = docId.split(":");
            String storageType = split[0];
            String relativePath = "";

            if (split.length >= 2) {
                relativePath = split[1];
            }
            return "/storage/" + storageType + "/" + relativePath;
        } catch (Exception e) {
            Log.e("PlaybackFragment", "Error parsing tree URI: " + treeUri, e);
            return "SAF Storage Selected";
        }
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
        list_view.refresh();

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
            decryptAndOpenVideo(media);
        } else {
            // Directly open in built-in video player
            VideoPlayerActivity.start(requireActivity(), media, null);
        }
    }

    private void openImage(Media media) {
        if (media.is_encrypted) {
            decryptAndOpenImage(media);
        } else {
            // Directly open in built-in image viewer
            ImageViewerActivity.start(requireActivity(), media, null);
        }
    }

    private void decryptAndOpenVideo(Media media) {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
        builder.setTitle("Enter Password");
        final android.widget.EditText input = new android.widget.EditText(requireActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            try {
                File tempDecryptedFile = File.createTempFile("temp_decrypted", ".mp4", requireActivity().getCacheDir());
                boolean result = decryptFile(media.contentUri, tempDecryptedFile, password);
                if (result) {
                    // Directly open in built-in video player
                    VideoPlayerActivity.start(requireActivity(), media, tempDecryptedFile.getAbsolutePath());
                } else {
                    MessageUtil.showToast(requireActivity(), "Failed to decrypt");
                    // Clean up the temporary file
                    if (tempDecryptedFile.exists()) {
                        tempDecryptedFile.delete();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                MessageUtil.showToast(requireActivity(), e.getLocalizedMessage());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void decryptAndOpenImage(Media media) {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
        builder.setTitle("Enter Password");
        final android.widget.EditText input = new android.widget.EditText(requireActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            try {
                String extension = media.name.toLowerCase().endsWith(".t3j") ? ".jpg" : ".png";
                File tempDecryptedFile = File.createTempFile("temp_decrypted", extension, requireActivity().getCacheDir());
                boolean result = decryptFile(media.contentUri, tempDecryptedFile, password);
                if (result) {
                    // Directly open in built-in image viewer
                    ImageViewerActivity.start(requireActivity(), media, tempDecryptedFile.getAbsolutePath());
                } else {
                    MessageUtil.showToast(requireActivity(), "Failed to decrypt");
                    // Clean up the temporary file
                    if (tempDecryptedFile.exists()) {
                        tempDecryptedFile.delete();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                MessageUtil.showToast(requireActivity(), e.getLocalizedMessage());
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

    private void shareFile(Media media) {
        try {
            // Validate that the media object and URI are not null
            if (media == null || media.contentUri == null) {
                MessageUtil.showToast(requireActivity(), "Invalid media file");
                return;
            }

            String mimeType = media.type == Media.TYPE.VIDEO ? "video/mp4" : "image/*";
            Uri shareUri = media.contentUri;
            
            // Handle file URIs by converting them to FileProvider URIs
            if ("file".equals(media.contentUri.getScheme())) {
                File file = new File(media.contentUri.getPath());
                if (file.exists() && file.canRead()) {
                    shareUri = androidx.core.content.FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".provider",
                        file
                    );
                } else {
                    MessageUtil.showToast(requireActivity(), "File not found or not readable");
                    return;
                }
            } else if ("content".equals(media.contentUri.getScheme())) {
                // For SAF URIs, grant temporary permissions
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                        media.contentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (SecurityException e) {
                    Log.w("Share", "Could not take persistable permission for SAF URI", e);
                    // Continue anyway, the share might still work
                }
            }
            
            // Validate that we have a valid URI to share
            if (shareUri == null) {
                MessageUtil.showToast(requireActivity(), "Could not prepare file for sharing");
                return;
            }
            
            ShareCompat.IntentBuilder.from(requireActivity())
                    .setType(mimeType)
                    .setStream(shareUri)
                    .setChooserTitle("Share media...")
                    .startChooser();
                    
        } catch (Exception e) {
            Log.e("Share", "Error sharing file", e);
            MessageUtil.showToast(requireActivity(), "Error sharing file: " + e.getMessage());
        }
    }

    private void decryptAndShareFile(Media media) {
        if (!isAdded() || getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireActivity());
        builder.setTitle("Enter Password");
        final android.widget.EditText input = new android.widget.EditText(requireActivity());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            try {
                String extension = media.type == Media.TYPE.VIDEO ? ".mp4" : 
                    (media.name.toLowerCase().endsWith(".t3j") ? ".jpg" : ".png");
                File tempDecryptedFile = File.createTempFile("temp_decrypted", extension, requireActivity().getCacheDir());
                boolean result = decryptFile(media.contentUri, tempDecryptedFile, password);
                if (result) {
                    // Share the decrypted file
                    shareDecryptedFile(media, tempDecryptedFile);
                } else {
                    MessageUtil.showToast(requireActivity(), "Failed to decrypt");
                    // Clean up the temporary file
                    if (tempDecryptedFile.exists()) {
                        tempDecryptedFile.delete();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                MessageUtil.showToast(requireActivity(), e.getLocalizedMessage());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shareDecryptedFile(Media media, File decryptedFile) {
        try {
            // Validate the decrypted file
            if (decryptedFile == null || !decryptedFile.exists() || !decryptedFile.canRead()) {
                MessageUtil.showToast(requireActivity(), "Decrypted file is not accessible");
                return;
            }

            String mimeType = media.type == Media.TYPE.VIDEO ? "video/mp4" : "image/*";
            
            // Use FileProvider to share the decrypted file
            Uri shareUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                decryptedFile
            );
            
            if (shareUri == null) {
                MessageUtil.showToast(requireActivity(), "Could not prepare decrypted file for sharing");
                return;
            }
            
            ShareCompat.IntentBuilder.from(requireActivity())
                    .setType(mimeType)
                    .setStream(shareUri)
                    .setChooserTitle("Share decrypted media...")
                    .startChooser();
                    
        } catch (Exception e) {
            Log.e("Share", "Error sharing decrypted file", e);
            MessageUtil.showToast(requireActivity(), "Error sharing decrypted file: " + e.getMessage());
        } finally {
            // Clean up the temporary file after sharing
            new android.os.Handler().postDelayed(() -> {
                if (decryptedFile != null && decryptedFile.exists()) {
                    boolean deleted = decryptedFile.delete();
                    if (!deleted) {
                        Log.w("Share", "Failed to delete temporary decrypted file: " + decryptedFile.getAbsolutePath());
                    }
                }
            }, 5000); // Delete after 5 seconds to allow sharing to complete
        }
    }

    public void onRefresh() {}

    @Override
    public void onDragRefresh() {
        loadFromTreeUriSingleDirectory();
    }

    @Override
    public void onDragLoadMore() {}
    
    // ===== FRAGMENT VISIBILITY METHODS =====
    
    @Override
    public void onFragmentVisible() {
        Log.d("PlaybackFragment", "Fragment became visible");
        // Simple visibility handling
        if (isAdded() && getActivity() != null) {
            // Initialize playback if needed
            initialize();
        }
    }
    
    @Override
    public void onFragmentHidden() {
        Log.d("PlaybackFragment", "Fragment became hidden");
        // Simple cleanup
    }

    private void loadFromTreeUriSingleDirectory() {
        if (treeUri == null) {
            txt_no_data.setVisibility(View.VISIBLE);
            return;
        }

        // Check if it's a file URI (legacy file path) or SAF URI
        if ("file".equals(treeUri.getScheme())) {
            // Handle file path
            loadFromFileDirectory();
        } else {
            // Handle SAF URI
            txt_no_data.setVisibility(View.GONE);
            new LoadDirectoryTask().execute(treeUri);
        }
    }

    private void loadFromFileDirectory() {
        String filePath = treeUri.getPath();
        if (filePath == null) {
            txt_no_data.setVisibility(View.VISIBLE);
            return;
        }

        File directory = new File(filePath);
        if (!directory.exists() || !directory.isDirectory()) {
            txt_no_data.setVisibility(View.VISIBLE);
            return;
        }

        txt_no_data.setVisibility(View.GONE);
        new LoadFileDirectoryTask().execute(directory);
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

            // Scan directory
            DocumentFile[] children = rootDoc.listFiles();
            if (children == null) return result;

            for (DocumentFile doc : children) {
                if (doc.isDirectory()) continue;
                String name = doc.getName();
                if (TextUtils.isEmpty(name)) continue;

                Media media = new Media();
                media.name = name;
                media.date = new Date(doc.lastModified());
                media.file = doc;
                media.contentUri = doc.getUri();
                media.fileSize = doc.length();

                String lowerName = name.toLowerCase();
                if (lowerName.endsWith(".t3v")) {
                    media.type = Media.TYPE.VIDEO;
                    media.is_encrypted = true;
                } else if (lowerName.endsWith(".t3j")) {
                    media.type = Media.TYPE.PHOTO;
                    media.is_encrypted = true;
                } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") ||
                        lowerName.endsWith(".mkv") || lowerName.endsWith(".avi")) {
                    media.type = Media.TYPE.VIDEO;
                } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") || lowerName.endsWith(".gif")) {
                    media.type = Media.TYPE.PHOTO;
                } else {
                    continue;
                }

                // Extract metadata for non-encrypted files
                if (!media.is_encrypted) {
                    extractAndStoreMetadata(context, media);
                }

                result.add(media);
            }

            // Sort by date (newest first)
            result.sort((m1, m2) -> Long.compare(m2.date.getTime(), m1.date.getTime()));

            return result;
        }

        private void extractAndStoreMetadata(Context context, Media media) {
            try {
                if (media.type == Media.TYPE.VIDEO) {
                    // For videos, try to extract metadata using MediaMetadataRetriever
                    try {
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(context, media.contentUri);

                        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        media.duration = durationStr != null ? Long.parseLong(durationStr) : 0;

                        String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                        String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

                        media.resolutionWidth = widthStr != null ? Integer.parseInt(widthStr) : 0;
                        media.resolutionHeight = heightStr != null ? Integer.parseInt(heightStr) : 0;

                    } catch (Exception e) {
                        Log.w("Metadata", "Could not extract video metadata for: " + media.contentUri);
                        // Don't log the full exception to avoid showing errors to users
                    }
                } else if (media.type == Media.TYPE.PHOTO) {
                    // For images, use BitmapFactory to get dimensions
                    try {
                        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                        options.inJustDecodeBounds = true;

                        try (InputStream is = context.getContentResolver().openInputStream(media.contentUri)) {
                            if (is != null) {
                                android.graphics.BitmapFactory.decodeStream(is, null, options);
                            }
                        }

                        media.resolutionWidth = options.outWidth;
                        media.resolutionHeight = options.outHeight;

                    } catch (Exception e) {
                        Log.w("Metadata", "Could not extract image metadata for: " + media.contentUri);
                        // Don't log the full exception to avoid showing errors to users
                    }
                }

            } catch (Exception e) {
                Log.w("Metadata", "Error extracting metadata for: " + media.contentUri);
                // Don't log the full exception to avoid showing errors to users
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

    private class LoadFileDirectoryTask extends AsyncTask<File, Void, List<Media>> {
        @Override
        protected List<Media> doInBackground(File... files) {
            List<Media> result = new ArrayList<>();
            File directory = files[0];

            if (directory == null || !directory.exists() || !directory.isDirectory()) {
                return result;
            }

            // Scan directory
            File[] children = directory.listFiles();
            if (children == null) return result;

            for (File file : children) {
                if (file.isDirectory()) continue;

                String name = file.getName();
                if (TextUtils.isEmpty(name)) continue;

                Media media = new Media();
                media.name = name;
                media.date = new Date(file.lastModified());
                media.contentUri = Uri.fromFile(file);
                media.fileSize = file.length();

                String lowerName = name.toLowerCase();
                if (lowerName.endsWith(".t3v")) {
                    media.type = Media.TYPE.VIDEO;
                    media.is_encrypted = true;
                } else if (lowerName.endsWith(".t3j")) {
                    media.type = Media.TYPE.PHOTO;
                    media.is_encrypted = true;
                } else if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") ||
                        lowerName.endsWith(".mkv") || lowerName.endsWith(".avi")) {
                    media.type = Media.TYPE.VIDEO;
                } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") || lowerName.endsWith(".gif")) {
                    media.type = Media.TYPE.PHOTO;
                } else {
                    continue;
                }

                // Extract metadata for non-encrypted files
                if (!media.is_encrypted) {
                    extractAndStoreMetadataForFile(media);
                }

                result.add(media);
            }

            // Sort by date (newest first)
            result.sort((m1, m2) -> Long.compare(m2.date.getTime(), m1.date.getTime()));

            return result;
        }

        private void extractAndStoreMetadataForFile(Media media) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();

                // Handle different file types and URI schemes
                if (media.type == Media.TYPE.VIDEO) {
                    // For videos, try to extract metadata
                    try {
                        if ("file".equals(media.contentUri.getScheme())) {
                            // For file URIs, use the path directly
                            retriever.setDataSource(media.contentUri.getPath());
                        } else {
                            // For SAF URIs, use ContentResolver
                            try (ParcelFileDescriptor pfd = requireContext().getContentResolver()
                                    .openFileDescriptor(media.contentUri, "r")) {
                                if (pfd != null) {
                                    retriever.setDataSource(pfd.getFileDescriptor());
                                } else {
                                    Log.w("Metadata", "Could not open file descriptor for: " + media.contentUri);
                                    return;
                                }
                            }
                        }

                        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        media.duration = durationStr != null ? Long.parseLong(durationStr) : 0;

                        String widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                        String heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);

                        media.resolutionWidth = widthStr != null ? Integer.parseInt(widthStr) : 0;
                        media.resolutionHeight = heightStr != null ? Integer.parseInt(heightStr) : 0;

                    } catch (Exception e) {
                        Log.w("Metadata", "Could not extract video metadata for: " + media.contentUri);
                        // Don't log the full exception to avoid showing errors to users
                    }
                } else if (media.type == Media.TYPE.PHOTO) {
                    // For images, use BitmapFactory to get dimensions
                    try {
                        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
                        options.inJustDecodeBounds = true;

                        if ("file".equals(media.contentUri.getScheme())) {
                            android.graphics.BitmapFactory.decodeFile(media.contentUri.getPath(), options);
                        } else {
                            try (InputStream is = requireContext().getContentResolver().openInputStream(media.contentUri)) {
                                if (is != null) {
                                    android.graphics.BitmapFactory.decodeStream(is, null, options);
                                }
                            }
                        }

                        media.resolutionWidth = options.outWidth;
                        media.resolutionHeight = options.outHeight;

                    } catch (Exception e) {
                        Log.w("Metadata", "Could not extract image metadata for: " + media.contentUri);
                        // Don't log the full exception to avoid showing errors to users
                    }
                }

            } catch (Exception e) {
                Log.w("Metadata", "Error extracting metadata for file: " + media.contentUri);
                // Don't log the full exception to avoid showing errors to users
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
                        if (media.is_selected) {
                            if ("file".equals(media.contentUri.getScheme())) {
                                // Delete file path
                                new File(media.contentUri.getPath()).delete();
                            } else if (media.file != null) {
                                // Delete SAF URI
                                media.file.delete();
                            }
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
            final Media media = mDataList.get(position);
            holder.txt_name.setText(media.name);
            holder.txt_date.setText(ResourceUtil.date(media.date));
            holder.txt_time.setText(ResourceUtil.time(media.date));

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
            } else {
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
                if (media.is_encrypted) {
                    // For encrypted files, offer to decrypt and share
                    MessageDialog.show("Share Encrypted File", 
                        "This file is encrypted. Would you like to decrypt it first and then share?",
                        "Decrypt & Share", "Cancel")
                        .setCancelButton((dialog, view) -> false)
                        .setOkButton((baseDialog, view) -> {
                            decryptAndShareFile(media);
                            return false;
                        });
                    return;
                }
                
                shareFile(media);
            });

            holder.ic_trash.setOnClickListener(v ->
                    MessageDialog.show(getString(R.string.delete), getString(R.string.confirm_delete),
                                    getString(R.string.Okay), getString(R.string.cancel))
                            .setCancelButton((dialog, view) -> false)
                            .setOkButton((baseDialog, view) -> {
                                if ("file".equals(media.contentUri.getScheme())) {
                                    new File(media.contentUri.getPath()).delete();
                                } else if (media.file != null) {
                                    media.file.delete();
                                }
                                list_view.refresh();
                                return false;
                            }));

            return convertView;
        }
    }
}