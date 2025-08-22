package com.checkmate.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.checkmate.android.AppPreference;
import com.checkmate.android.R;
import com.wmspanel.libstream.Streamer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ArrayUtils {
    static <T> boolean contains(T[] array, T value) {
        for (T element : array) {
            if (element == null) {
                if (value == null) return true;
            } else {
                if (value != null && element.equals(value)) return true;
            }
        }
        return false;
    }

    static boolean contains(int[] array, int value) {
        for (int element : array) {
            if (element == value) {
                return true;
            }
        }
        return false;
    }
}

public class DeepLink {


    private class ParseContext {
        private final int MAX_OBJECT_LEVEL = 4;
        Object[] objStack = new Object[MAX_OBJECT_LEVEL];
        String[] nameStack = new String[MAX_OBJECT_LEVEL];
    }

    private static final String TAG = "SettingsUtils";

    private static final Map<String, Streamer.MODE> CONFIG_STREAMER_MODES_MAP = createStremerModesMap();
    private static final Map<String, Streamer.AUTH> CONFIG_AUTH_MODES_MAP = createStreamerAuthModesMap();
    private static final Map<String, Integer> CONFIG_RIST_PROFILE_MAP = createRistProfilesMap();

    private static final int IMPORTED_CAMERA_REAR = 0;
    private static final int IMPORTED_CAMERA_FRONT = 1;

    private int mImportedCameraId = -1;
    private Streamer.Size mImportedVideoSize;
    private int mImportedFps = -1;

    private static DeepLink instance;
    ParseContext parseCtx;

    private JSONObject mParsedLink;
    private ImportedSettingsInfo mParsedSettingsInfo;
    private ImportedSettingsInfo mImportedSettingsInfo;
    private static final int VIDEO_ERROR_INDEX = -1;
    private static final int AUDIO_ERROR_INDEX = -2;
    private static final int RECORD_ERROR_INDEX = -3;

    private final String[] ADAPTIVE_BITRATE_MODES = {"0", "1", "2"};

    static private Map<String, Streamer.MODE> createStremerModesMap() {
        Map<String, Streamer.MODE> result = new HashMap<String, Streamer.MODE>();
        result.put("av", Streamer.MODE.AUDIO_VIDEO);
        result.put("a", Streamer.MODE.AUDIO_ONLY);
        result.put("v", Streamer.MODE.VIDEO_ONLY);
        return result;
    }

    static private Map<String, Streamer.AUTH> createStreamerAuthModesMap() {
        Map<String, Streamer.AUTH> result = new HashMap<String, Streamer.AUTH>();
        result.put("lime", Streamer.AUTH.LLNW);
        result.put("peri", Streamer.AUTH.PERISCOPE);
        result.put("rtmp", Streamer.AUTH.RTMP);
        result.put("aka", Streamer.AUTH.AKAMAI);
        result.put("", Streamer.AUTH.DEFAULT);

        return result;
    }

    static private Map<String, Integer> createRistProfilesMap() {
        Map<String, Integer> result = new HashMap<>();
        result.put("simple", 0);
        result.put("main", 1);
        // Advanced  is not implemented currently, use main instead
        result.put("advanced", 1);
        return result;
    }


    public static DeepLink getInstance() {
        if (instance == null) {
            instance = new DeepLink();
        }
        return instance;
    }

    public void reset() {
        mParsedLink = null;
        mParsedSettingsInfo = null;
        mImportedSettingsInfo = null;
    }

    public boolean isDeepLinkUrl(Context ctx, Uri request) {
        if (ctx == null || request == null) {
            return false;
        }
        final String deepLinkScheme = ctx.getString(R.string.deep_link_scheme);
        final String scheme = request.getScheme();
        if (scheme == null || !scheme.equals(deepLinkScheme) ||
                request.getHost() == null || request.getPath() == null) {
            return false;
        }
        final String path = request.getHost() + request.getPath();
        final String setUrl = ctx.getString(R.string.deep_link_set_url);

        return path.startsWith(setUrl);
    }

    public void parseDeepLink(Uri request) throws JSONException {
        if (request.getQuery() == null) {
            return;
        }
        parseCtx = new ParseContext();
        JSONObject params = new JSONObject();
        // getQueryParameterNames / getQueryParameters doesn't handle duplicate names the way we need it, so parse it manually
        String[] paramsArray = request.getEncodedQuery().split("&");
        for (String param : paramsArray) {
            int nameSep = param.indexOf('=');
            if (nameSep <= 0) continue;
            String key = Uri.decode(param.substring(0, nameSep));
            String value = Uri.decode(param.substring(nameSep + 1));
            if (key.contains("[")) {
                parseNestedParams(params, key, value);
            } else {
                params.put(key, value);
            }
        }
        mParsedLink = params;
        mParsedSettingsInfo = getSettingsInfo(mParsedLink);
        parseCtx = null;
    }

    private void parseNestedParams(JSONObject params, String key, String value) throws JSONException {
        Pattern indexPattern = Pattern.compile("\\[[a-zA-Z0-9_]*\\]", Pattern.CASE_INSENSITIVE);
        Matcher m = indexPattern.matcher(key);
        int indexStart = key.indexOf('[');
        String rootName = indexStart > 0 ? key.substring(0, indexStart) : key;
        parseCtx.objStack[0] = params;
        parseCtx.nameStack[0] = rootName;
        String keyName = rootName;
        m.reset();
        int objIndex = 1;
        Object o = params;
        while (m.find()) {
            keyName = m.group();
            keyName = keyName.substring(1, keyName.length() - 1);
            String parentName = parseCtx.nameStack[objIndex - 1];
            if (keyName.isEmpty()) {
                o = getArrayParent(params, parentName, objIndex);
                parseCtx.nameStack[objIndex] = "";
            } else {
                if (parentName.isEmpty()) {
                    o = getMapParentArray(keyName, objIndex);
                } else {
                    o = getMapParentObject(parentName, objIndex);
                }
                parseCtx.nameStack[objIndex] = keyName;
            }
            parseCtx.objStack[objIndex] = o;
            objIndex++;
        }
        if (o != null) {
            if (o instanceof JSONArray) {
                JSONArray arr = (JSONArray) o;
                arr.put(value);
            } else if (o instanceof JSONObject) {
                JSONObject obj = (JSONObject) o;
                obj.put(keyName, value);
            }
        }
    }

    private Object getArrayParent(JSONObject params, String parentName, int objIndex) throws JSONException {
        Object o = null;
        Object tmp = parseCtx.objStack[objIndex - 1];
        if (tmp instanceof JSONObject) {
            JSONObject parent = (JSONObject) tmp;
            if (parent.has(parentName)) {
                o = params.get(parentName);
            } else {
                JSONArray array = new JSONArray();
                parent.put(parentName, array);
                o = array;
            }
        }
        return o;
    }

    private Object getMapParentArray(String keyName, int objIndex) throws JSONException {
        Object o = null;
        Object tmp = parseCtx.objStack[objIndex - 1];
        if (tmp instanceof JSONArray) {
            JSONArray parent = (JSONArray) tmp;
            int idx = parent.length() - 1;
            boolean addNew = idx < 0;
            if (idx >= 0) {
                JSONObject last = parent.getJSONObject(idx);
                addNew = last != null && last.has(keyName);
                if (!addNew) {
                    o = last;
                }
            }
            if (addNew) {
                JSONObject newObject = new JSONObject();
                parent.put(newObject);
                o = newObject;
            }
        }
        return o;
    }

    private Object getMapParentObject(String parentName, int objIndex) throws JSONException {
        Object o = null;
        Object tmp = parseCtx.objStack[objIndex - 1];
        if (tmp instanceof JSONObject) {
            JSONObject parent = (JSONObject) tmp;
            if (parent.has(parentName)) {
                o = parent.get(parentName);
            } else {
                o = new JSONObject();
                parent.put(parentName, o);
            }
        }
        return o;
    }


    public static class ImportedSettingsInfo {
        public int connections;
        public int updatedConnections;
        public boolean hasVideo;
        public boolean hasAudio;
        public boolean hasRecord;
        public int deletedRecords;
        public boolean needRestart;
        private Map<Integer, String> mImportErrors = new HashMap<>();

        public boolean isEmpty() {
            return connections == 0 && deletedRecords == 0 && !hasVideo && !hasAudio && !hasRecord && mImportErrors.isEmpty();
        }
    }

    public ImportedSettingsInfo getSettingsInfo(JSONObject settings) {
        ImportedSettingsInfo info = new ImportedSettingsInfo();
        if (settings.has("conn")) {
            JSONArray connections = settings.optJSONArray("conn");
            if (connections != null) {
                info.connections = connections.length();
            }
        }
        JSONObject encoidngSettings = settings.optJSONObject("enc");
        if (encoidngSettings != null) {
            JSONObject videoSettings = encoidngSettings.optJSONObject("vid");
            info.hasVideo = videoSettings != null;
            JSONObject audioSettings = encoidngSettings.optJSONObject("aud");
            info.hasAudio = audioSettings != null;
            JSONObject recordSettings = encoidngSettings.optJSONObject("record");
            info.hasRecord = recordSettings != null;
        }
        String deleteConn = settings.optString("deleteConn");
        if (deleteConn.equals("1") || deleteConn.equals("on")) {
            long count = Connection.count(Connection.class);
            info.deletedRecords = (int) count;
        }

        return info;
    }


    private int getExistingConnCount() {
        long count = 0;
        String connNames = "";
        if (mParsedLink.has("conn")) {
            JSONArray connections = mParsedLink.optJSONArray("conn");
            for (int i = 0; i < connections.length(); i++) {
                final JSONObject conn = connections.optJSONObject(i);
                String name = conn.optString("name").trim();
                String overwrite = conn.optString("overwrite");
                if (!name.isEmpty() && (overwrite.equals("on") || overwrite.equals("1"))) {
                    if (!connNames.isEmpty()) {
                        connNames += ",";
                    }
                    connNames += "'" + name + "'";
                }
            }
            count = Connection.count(Connection.class, "name in (" + connNames + ")", null);
        }

        return (int) count;
    }

    public void importSettings(Context ctx) {
        mImportedSettingsInfo = new ImportedSettingsInfo();
        if (ctx == null || mParsedLink == null) {
            return;
        }

        if (mParsedLink.has("deleteConn")) {
            String delete = mParsedLink.optString("deleteConn");
            if (delete.equals("on") || delete.equals("1")) {
                mImportedSettingsInfo.deletedRecords = Connection.deleteAll(Connection.class);
            }
        }

        if (mParsedLink.has("conn")) {
            JSONArray connections = mParsedLink.optJSONArray("conn");
            if (connections != null) {
                if (connections.length() > 0 && Connection.count(Connection.class) > 0) {
                    //Set existing connections inactive
                    Connection.executeQuery("UPDATE Connection SET active = 0");
                }
                mImportedSettingsInfo.connections = importConnections(connections, ctx);
            }
        }
        JSONObject encoidngSettings = mParsedLink.optJSONObject("enc");
        if (encoidngSettings != null) {
            JSONObject videoSettings = encoidngSettings.optJSONObject("vid");
            if (videoSettings != null) {
                mImportedSettingsInfo.hasVideo = importVideoSettings(videoSettings, ctx);
            }
            JSONObject audioSettings = encoidngSettings.optJSONObject("aud");
            if (audioSettings != null) {
                mImportedSettingsInfo.hasAudio = importAudioSettings(audioSettings, ctx);
            }
            JSONObject recordSettings = encoidngSettings.optJSONObject("record");
            if (recordSettings != null) {
                mImportedSettingsInfo.hasRecord = importRecordSettings(recordSettings, ctx);
            }
        }
    }

    private int importConnections(JSONArray connections, Context context) {
        int importedCount = 0;
        int activeCount = 0;
        for (int i = 0; i < connections.length(); i++) {
            final JSONObject conn = connections.optJSONObject(i);
            if (conn == null) {
                mImportedSettingsInfo.mImportErrors.put(i, context.getResources().getString(R.string.settings_parse_failed));
                continue;
            }

            String id = conn.optString("id");
            String name = conn.optString("name").trim();

            final String url = conn.optString("url");
            if (url.isEmpty()) {
                mImportedSettingsInfo.mImportErrors.put(i, context.getResources().getString(R.string.no_url));
                continue;
            }
            final SettingsUtils.UriResult connResult = SettingsUtils.parseUrl(context, url);
            if (connResult.uri == null) {
                mImportedSettingsInfo.mImportErrors.put(i, connResult.error);
                continue;
            }

            if (id.isEmpty()) {
                Date now = new Date();
                id = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(now);
            }
            if (name.isEmpty()) {
                name = id;
            }

            final String rewrite = conn.optString("overwrite");
            boolean newRecord = true;
            Connection record = null;
            if (rewrite.equals("on") || rewrite.equals("1")) {
                List<Connection> found = Connection.find(Connection.class, "name=?", name);
                if (!found.isEmpty()) {
                    record = found.get(0);
                    record.url = url;
                    newRecord = false;
                }
            }
            if (record == null) {
                name = validateConnectionName(name);
                record = new Connection(name, connResult.uri);
            }
            final String mode = conn.optString("mode");
            if (!mode.isEmpty() && CONFIG_STREAMER_MODES_MAP.containsKey(mode)) {
                record.mode = CONFIG_STREAMER_MODES_MAP.get(mode).ordinal();
            }

            final String username = conn.optString("user");
            final String password = conn.optString("pass");
            final boolean haveCredentials = (!username.isEmpty() && !password.isEmpty());

            if (connResult.isRtsp() && haveCredentials) {
                record.username = username;
                record.password = password;
            }
            if (username.isEmpty() != password.isEmpty() && (connResult.isRtmp() || connResult.isRtsp())) {
                mImportedSettingsInfo.mImportErrors.put(i, context.getResources().getString(R.string.need_login_pass));
                continue;
            }

            final String target = conn.optString("target");
            final Streamer.AUTH auth = CONFIG_AUTH_MODES_MAP.get(target);
            if (connResult.isRtmp()) {
                if (auth == Streamer.AUTH.LLNW || auth == Streamer.AUTH.RTMP || auth == Streamer.AUTH.AKAMAI) {
                    if (haveCredentials) {
                        record.username = username;
                        record.password = password;
                        record.auth = auth.ordinal();
                    } else {
                        mImportedSettingsInfo.mImportErrors.put(i, context.getResources().getString(R.string.need_login_pass));
                        continue;
                    }
                } else {
                    if (haveCredentials) {
                        mImportedSettingsInfo.mImportErrors.put(i, context.getResources().getString(R.string.need_set_auth));
                        continue;
                    }
                    record.auth = auth.ordinal();
                }
            }

            if (connResult.isSrt()) {
                final String latency = conn.optString("srtlatency");
                if (!latency.isEmpty()) {
                    record.latency = parseIntSafe(latency, 0);
                }
                final String maxbw = conn.optString("srtmaxbw");
                if (!maxbw.isEmpty()) {
                    record.maxbw = parseIntSafe(maxbw, 0);
                }
                final String streamid = conn.optString("srtstreamid");
                if (!streamid.isEmpty()) {
                    record.streamid = streamid;
                }
                final String passphrase = conn.optString("srtpass");
                if (!passphrase.isEmpty()) {
                    record.passphrase = passphrase;
                }
                final String pbkeylen = conn.optString("srtpbkl");
                if (!pbkeylen.isEmpty()) {
                    int len = parseIntSafe(pbkeylen, 0);
                    if (len == 16 || len == 24 || len == 32) {
                        record.pbkeylen = len;
                    }
                }
            }

            if (connResult.isRist()) {
                final String profile = conn.optString("ristProfile");
                if (!profile.isEmpty() && CONFIG_RIST_PROFILE_MAP.containsKey(profile)) {
                    Integer profileInt = CONFIG_RIST_PROFILE_MAP.get(profile);
                    record.ristProfile = profileInt;
                } else if (!profile.isEmpty()) {
                    String error = context.getResources().getString(R.string.settings_wrong_value, "ristProfile");
                    mImportedSettingsInfo.mImportErrors.put(i, error);
                }
            }
            final String active = conn.optString("active");
            final boolean setActive = !(active.equals("0") || active.equals("off"));

            record.active = setActive && activeCount < 3;
            if (record.active) {
                activeCount++;
            }

            if (record.save() >= 0) {
                importedCount++;
                if (!newRecord) {
                    mImportedSettingsInfo.updatedConnections++;
                }
            }
        }
        return importedCount;
    }

    private boolean importVideoSettings(JSONObject video, Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean preferenceChanged = false;
        String videoErrors = "";
        final String res = video.optString("res");
        if (!res.isEmpty()) {
            boolean resParsed = false;
            String[] dimensions = res.split("x");
            if (dimensions.length == 2) {
                try {
                    int w = Integer.parseInt(dimensions[0]);
                    int h = Integer.parseInt(dimensions[1]);
                    mImportedVideoSize = new Streamer.Size(w, h);
                    resParsed = true;
                } catch (NumberFormatException nfe) {
                    mImportedVideoSize = null;
                }
            }
            if (!resParsed) {
                videoErrors += context.getResources().getString(R.string.settings_wrong_value, "res") + " ";
            }
        }
        final String cameraId = video.optString("camera");
        if (!cameraId.isEmpty()) {
            mImportedCameraId = parseIntSafe(cameraId, -1);
        }
        final SharedPreferences.Editor editor = sp.edit();

        final String orientation = video.optString("orientation");
        if (!orientation.isEmpty()) {
            boolean isVertical = orientation.equals("vertical");
            editor.putBoolean(context.getString(R.string.vertical_video_key), isVertical);
            preferenceChanged = true;
        }

        final String liveRotation = video.optString("liveRotation");
        if (!liveRotation.isEmpty()) {
            final boolean rotate = !orientation.equals("off");
            editor.putBoolean(context.getString(R.string.adjust_stream_orientation_key), rotate);
            preferenceChanged = true;
        }

        final String fps = video.optString("fps");
        if (!fps.isEmpty()) {
            mImportedFps = parseIntSafe(fps, -1);
        }

        final String bitrate = video.optString("bitrate"); //kbps
        if (!bitrate.isEmpty() && bitrate.charAt(0) != '0' && bitrate.matches("\\d{3,5}")) { // Allow 100-99999 range
            final String bitrate_key = context.getString(R.string.bitrate_key);
            editor.putString(bitrate_key, bitrate);
            preferenceChanged = true;
        } else if (!bitrate.isEmpty()) {
            videoErrors += context.getResources().getString(R.string.settings_wrong_value, "bitrate") + " ";
        }

        final String adaptiveBitrate = video.optString("adaptiveBitrate");
        if (!adaptiveBitrate.isEmpty()) {
            List<String> adaptiveModes = Arrays.asList(ADAPTIVE_BITRATE_MODES);
            if (adaptiveModes.contains(adaptiveBitrate)) {
                final String bitrate_key = context.getString(R.string.adaptive_bitrate_key);
                editor.putString(bitrate_key, bitrate);
            } else {
                videoErrors += context.getResources().getString(R.string.settings_wrong_value, "adaptiveBitrate") + " ";
            }
        }

        final String adaptiveFps = video.optString("adaptiveFps");
        if (!adaptiveFps.isEmpty()) {
            final boolean isOn = adaptiveFps.equals("1") || adaptiveFps.equals("on");
            final String fps_key = context.getString(R.string.adaptive_fps_key);
            editor.putBoolean(fps_key, isOn);
        }

        final String keyframeInterval = video.optString("keyframe");
        if (!keyframeInterval.isEmpty() && keyframeInterval.charAt(0) != '0' && keyframeInterval.matches("\\d{1,2}")) { // Allow 1-99 range
            final String interval_key = context.getString(R.string.key_frame_interval_key);
            editor.putString(interval_key, keyframeInterval);
            preferenceChanged = true;
        } else if (!keyframeInterval.isEmpty()) {
            videoErrors += context.getResources().getString(R.string.settings_wrong_value, "keyframe") + " ";
        }
        final String format = video.optString("format"); //avc | hevc
        if (!format.isEmpty()) {
            final String fullFormat = "video/" + format;
            final String[] codecs = context.getResources().getStringArray(R.array.video_codec_values);
            final String codec_key = context.getString(R.string.video_codec_key);
            if (ArrayUtils.contains(codecs, fullFormat)) {
                editor.putString(codec_key, fullFormat);
                preferenceChanged = true;
            } else {
                videoErrors += context.getResources().getString(R.string.settings_wrong_value, "format") + " ";
            }
        }
        final String backgroundMode = video.optString("background");
        if (!backgroundMode.isEmpty()) {
            final boolean isOn = (backgroundMode.equals("on") || backgroundMode.equals("1"));
            final String key = context.getString(R.string.pref_foreground_service_key);
            final boolean currentOn = sp.getBoolean(key, false);
            if (isOn != currentOn) {
                mImportedSettingsInfo.needRestart = true;
            }
            editor.putBoolean(key, isOn);
            if (isOn) {
                editor.putBoolean(context.getString(R.string.radio_mode_key), false);
                editor.remove(context.getString(R.string.usb_camera_key));
            }
            preferenceChanged = true;
        }

        if (!videoErrors.isEmpty()) {
            mImportedSettingsInfo.mImportErrors.put(VIDEO_ERROR_INDEX, videoErrors);
        }
        return preferenceChanged ? editor.commit() : true;
    }

    private boolean importAudioSettings(JSONObject audio, Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sp.edit();
        boolean preferenceChanged = false;
        String audioErrors = "";
        int[] supportedSampleRates = {44100};
        int maxInputChannelCount = 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final MediaCodecInfo info = SettingsUtils.selectCodec(MediaFormat.MIMETYPE_AUDIO_AAC);
            final MediaCodecInfo.CodecCapabilities capabilities = info.getCapabilitiesForType(MediaFormat.MIMETYPE_AUDIO_AAC);
            final MediaCodecInfo.AudioCapabilities audioCapabilities = capabilities.getAudioCapabilities();
            supportedSampleRates = audioCapabilities.getSupportedSampleRates();
            maxInputChannelCount = audioCapabilities.getMaxInputChannelCount();
        }

        String bitrate = audio.optString("bitrate");
        if (!bitrate.isEmpty()) {
            if (!bitrate.equals("0")) {
                bitrate += "000"; //"convert" to bps
            }
            String[] bitrateArray = context.getResources().getStringArray(R.array.audio_bitrate_values);
            if (ArrayUtils.contains(bitrateArray, bitrate)) {
                final String bitrate_key = context.getString(R.string.pref_audio_bitrate_key);
                editor.putString(bitrate_key, bitrate);
                preferenceChanged = true;
            } else {
                audioErrors += context.getResources().getString(R.string.settings_wrong_value, "bitrate") + " ";
            }
        }
        final String channelsStr = audio.optString("channels");
        if (!channelsStr.isEmpty()) {
            int num = parseIntSafe(channelsStr, -1);
            if (num > 0 && num <= maxInputChannelCount) {
                final String bitrate_key = context.getString(R.string.channel_count_key);
                editor.putString(bitrate_key, channelsStr);
                preferenceChanged = true;
            } else {
                audioErrors += context.getResources().getString(R.string.settings_wrong_value, "channels") + " ";
            }
        }
        String sampleRateStr = audio.optString("samples");
        if (!sampleRateStr.isEmpty()) {
            int sampleRate = parseIntSafe(sampleRateStr, -1);
            if (ArrayUtils.contains(supportedSampleRates, sampleRate)) {
                final String bitrate_key = context.getString(R.string.sample_rate_key);
                editor.putString(bitrate_key, sampleRateStr);
                preferenceChanged = true;
            } else {
                audioErrors += context.getResources().getString(R.string.settings_wrong_value, "samples") + " ";
            }
        }

        final String audioOnly = audio.optString("audioOnly");
        if (!audioOnly.isEmpty()) {
            final boolean isOn = audioOnly.equals("1") || audioOnly.equals("on");
            final String radioKey = context.getString(R.string.radio_mode_key);
            final boolean currentOn = sp.getBoolean(radioKey, false);
            if (isOn != currentOn) {
                mImportedSettingsInfo.needRestart = true;
            }
            editor.putBoolean(radioKey, isOn);
            if (isOn) {
                editor.putBoolean(context.getString(R.string.pref_foreground_service_key), true);
                editor.remove(context.getString(R.string.usb_camera_key));
            }
        }

        if (!audioErrors.isEmpty()) {
            mImportedSettingsInfo.mImportErrors.put(AUDIO_ERROR_INDEX, audioErrors);
        }
        return preferenceChanged ? editor.commit() : true;
    }

    private boolean importRecordSettings(JSONObject record, Context context) {
        boolean preferenceChanged = false;
        String recordErrors = "";

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sp.edit();
        final String recEnabled = record.optString("enable");
        boolean isRec = false;
        if (!recEnabled.isEmpty()) {
            isRec = recEnabled.equals("1") || recEnabled.equals("on");
            final String rec_key = context.getString(R.string.pref_mp4rec_key);
            editor.putBoolean(rec_key, isRec);
            preferenceChanged = true;
        }
        if (isRec) {
            final String durationStr = record.optString("duration");
            if (!durationStr.isEmpty()) {
                final int duration = parseIntSafe(durationStr, -1);
                if (duration >= 0 && duration < 1440) {
                    final String duration_key = context.getString(R.string.record_duration_key);
                    editor.putString(duration_key, durationStr);
                    preferenceChanged = true;
                } else {
                    recordErrors += context.getResources().getString(R.string.settings_wrong_value, "duration") + " ";
                }
            }
        }

        if (!recordErrors.isEmpty()) {
            mImportedSettingsInfo.mImportErrors.put(RECORD_ERROR_INDEX, recordErrors);
        }
        return preferenceChanged ? editor.commit() : true;
    }


    public boolean hasParsedSettings() {
        return (mParsedSettingsInfo != null && !mParsedSettingsInfo.isEmpty());
    }

    public String getImportConfirmationBody(Context ctx) {
        if (ctx == null || mParsedSettingsInfo == null || mParsedSettingsInfo.isEmpty()) {
            return "";
        }
        String message = ctx.getString(R.string.settings_import_body);
        if (mParsedSettingsInfo.deletedRecords > 0) {
            message += ctx.getString(R.string.settings_import_remove_existing, mParsedSettingsInfo.deletedRecords);
        }
        int updateCount = mParsedSettingsInfo.deletedRecords > 0 ? 0 : getExistingConnCount();
        if (updateCount > 0) {
            int newCount = mParsedSettingsInfo.connections - updateCount;
            if (newCount > 0) {
                message += ctx.getString(R.string.settings_import_connections, newCount, updateCount);
            } else {
                message += ctx.getString(R.string.settings_import_connections_updated, updateCount);
            }
        } else {
            message += ctx.getString(R.string.settings_import_connections_new, mParsedSettingsInfo.connections);
        }

        if (mParsedSettingsInfo.hasVideo) {
            message += "<br>" + ctx.getString(R.string.pref_fragment_header_video);
        }
        if (mParsedSettingsInfo.hasAudio) {
            message += "<br>" + ctx.getString(R.string.pref_fragment_header_audio);
        }
        if (mParsedSettingsInfo.hasRecord) {
            message += "<br>" + ctx.getString(R.string.pref_fragment_header_mp4rec);
        }

        return message;
    }

    public boolean hasImportedSettings() {
        return (mImportedSettingsInfo != null && !mImportedSettingsInfo.isEmpty());
    }

    public boolean needRestart() {
        return (mImportedSettingsInfo != null && mImportedSettingsInfo.needRestart);
    }


    public String getImportResultBody(Context ctx, boolean fromPreferences) {
        String import_status = "";
        if (mImportedSettingsInfo == null) {
            return "";
        }
        ImportedSettingsInfo info = mImportedSettingsInfo;
        if (ctx != null && info != null && !info.isEmpty()) {
            String avStatus = "";
            List<String> settingsList = new ArrayList<>();
            if (info.hasAudio) {
                settingsList.add(ctx.getString(R.string.settings_imported_audio));
            }
            if (info.hasVideo) {
                settingsList.add(ctx.getString(R.string.settings_imported_video));
            }
            if (info.hasRecord) {
                settingsList.add(ctx.getString(R.string.settings_imported_record));
            }
            switch (settingsList.size()) {
                case 1:
                    avStatus = settingsList.get(0);
                    break;
                case 2:
                    avStatus = ctx.getString(R.string.settings_imported_2, settingsList.get(0), settingsList.get(1));
                    break;
                case 3:
                    avStatus = ctx.getString(R.string.settings_imported_3, settingsList.get(0), settingsList.get(1), settingsList.get(2));
                    break;
            }

            if (info.connections > 0) {
                String connString = ctx.getString(R.string.settings_imported_new, mImportedSettingsInfo.connections);
                if (info.updatedConnections > 0 && info.updatedConnections == info.connections) {
                    connString = ctx.getString(R.string.settings_imported_updated, mImportedSettingsInfo.connections);
                } else if (info.updatedConnections > 0) {
                    int newCount = info.connections - info.updatedConnections;
                    connString = ctx.getString(R.string.settings_imported_new_updated, info.connections, newCount, info.updatedConnections);
                }
                if (!avStatus.isEmpty()) {
                    import_status = ctx.getString(R.string.settings_imported_connections_and_settings, connString, avStatus);
                } else {
                    import_status = ctx.getString(R.string.settings_imported_connections, connString);
                }
            } else {
                import_status = ctx.getString(R.string.settings_imported_settings, avStatus);
            }
        }
        Map<Integer, String> importErrors = mImportedSettingsInfo.mImportErrors;
        if (!importErrors.isEmpty()) {
            Integer[] keys = importErrors.keySet().toArray(new Integer[0]);
            Arrays.sort(keys);
            String errors = "";
            for (Integer index : keys) {
                String err = importErrors.get(index);
                switch (index) {
                    case AUDIO_ERROR_INDEX:
                        errors += ctx.getString(R.string.settings_error_audio, err);
                        break;
                    case VIDEO_ERROR_INDEX:
                        errors += ctx.getString(R.string.settings_error_video, err);
                        break;
                    case RECORD_ERROR_INDEX:
                        errors += ctx.getString(R.string.settings_error_record, err);
                        break;
                    default:
                        errors += ctx.getString(R.string.settings_error_connection, index + 1, err);
                }
            }

            import_status += ctx.getString(R.string.settings_has_errors, errors);
        }
        if (fromPreferences && mImportedSettingsInfo.needRestart) {
            import_status += "\n" + ctx.getString(R.string.service_restart_info);
        }
        mImportedSettingsInfo = null;
        return import_status;
    }

    static private String validateConnectionName(String name) {
        Pattern numberPattern = Pattern.compile("\\s*\\d+$");
        Matcher nameMatch = numberPattern.matcher(name);
        long sequence = -1;
        long nameOrd = 0;
        String pureName = name;

        List<Connection> duplicates = Connection.find(Connection.class, "name like ?", pureName + "%");
        if (duplicates.isEmpty()) {
            return name;
        }
        for (Connection conn : duplicates) {
            String connName = conn.name;
            if (connName.equals(pureName)) {
                sequence = 1;
                continue;
            }
            Matcher m = numberPattern.matcher(connName);
            m.reset();
            if (m.find()) {
                int id;
                String numStr = m.group();
                String prefix = connName.substring(0, m.start());
                if (!prefix.equals(pureName)) {
                    //Existing connection name don't match new name exactly
                    continue;
                }
                id = parseIntSafe(numStr.trim(), 0);
                if (id > sequence) {
                    sequence = id;
                }
            }
        }
        if (sequence >= 0) {
            pureName += String.format(" %d", sequence == 0 ? 2 : sequence + 1);
        }
        return pureName;
    }

    static private int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return fallback;
        }
    }

    public boolean hasImportedActiveCamera() {
        return mImportedCameraId >= 0;
    }

    public CameraInfo getActiveCameraInfo(List<CameraInfo> cameraList, Context context) {
        CameraInfo cameraInfo = null;
        if (!hasImportedActiveCamera()) {
            return null;
        }
        if (cameraList == null || cameraList.size() == 0) {
            Log.e(TAG, "no camera found");
        } else {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            for (CameraInfo cursor : cameraList) {
                if ((mImportedCameraId == IMPORTED_CAMERA_REAR && cursor.lensFacing == CameraInfo.LENS_FACING_BACK) ||
                        (mImportedCameraId == IMPORTED_CAMERA_FRONT && cursor.lensFacing == CameraInfo.LENS_FACING_FRONT)) {
                    cameraInfo = cursor;
                    break;
                }
            }
            if (cameraInfo != null) {
                final SharedPreferences.Editor edit = sp.edit();
                edit.putString(context.getString(R.string.cam_key), cameraInfo.cameraId);
                edit.apply();
            }
            mImportedCameraId = -1;
            if (cameraInfo == null) {
                cameraInfo = cameraList.get(0);
            }
        }
        return cameraInfo;
    }

    public boolean hasImportedVideoSize() {
        return mImportedVideoSize != null;
    }

    public Streamer.Size getVideoSize(CameraInfo cameraInfo, Context context) {
        Streamer.Size videoSize = null;
        if (cameraInfo == null || cameraInfo.recordSizes == null || cameraInfo.recordSizes.length == 0) {
            return videoSize;
        }
        if (!hasImportedVideoSize()) {
            return videoSize;
        }
        final String video_size_key = context.getString(R.string.video_size_key);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        for (int i = 0; i < cameraInfo.recordSizes.length; i++) {
            Streamer.Size size = cameraInfo.recordSizes[i];
            // approximate comparison to match (1920x1080) == (1920x1088) and similar
            if (Math.abs(size.width - mImportedVideoSize.width) < 10 &&
                    Math.abs(size.height - mImportedVideoSize.height) < 10) {
                final String indexStr = Integer.toString(i);
                final SharedPreferences.Editor edit = sp.edit();
                edit.putString(video_size_key, indexStr);
                edit.apply();
                videoSize = size;
                break;
            }
        }
        if (videoSize == null) {
            videoSize = cameraInfo.recordSizes[0];
        }
        mImportedVideoSize = null;

        // Reduce 4K to FullHD, because some encoders can fail with 4K frame size.
        // https://source.android.com/compatibility/android-cdd.html#5_2_video_encoding
        // Video resolution: 320x240px, 720x480px, 1280x720px, 1920x1080px.
        // If no FullHD support found, leave video size as is.
        if (videoSize.width > 1920 || videoSize.height > 1088) {
            for (Streamer.Size size : cameraInfo.recordSizes) {
                if (size.width == 1920 && (size.height == 1080 || size.height == 1088)) {
                    videoSize = size;
                    Log.d(TAG, "Reduce 4K to " + size.height + "p");
                    break;
                }
            }
        }
        return videoSize;
    }

    public boolean hasImportedFpsRange() {
        return mImportedFps > 0;
    }

    public Streamer.FpsRange findFpsRange(Context context, Streamer.FpsRange[] fpsRanges) {
        if (fpsRanges == null || fpsRanges.length < 2) {
            // old devices usually provide single fps range per camera
            // so app don't need to set it explicitly
            return null;
        }
        if (!hasImportedFpsRange()) {
            return null;
        }
        float targetFps = mImportedFps * 1.0f;
        if (fpsRanges[0].fpsMax > 1000) {
            targetFps *= 1000f;
        }
        Streamer.FpsRange fpsRange = nearestFpsRange(fpsRanges, targetFps);
        if (fpsRange != null) {
            AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MIN, fpsRange.fpsMin);
            AppPreference.setInt(AppPreference.KEY.FPS_RANGE_MAX, fpsRange.fpsMax);
            for (int i = 0; i < fpsRanges.length; i++) {
                if (fpsRanges[i].equals(fpsRange)) {
                    String strIndex = Integer.toString(i + 1);
                    AppPreference.setStr(AppPreference.KEY.FPS_RANGE, strIndex);
                    break;
                }
            }
        }
        mImportedFps = -1;
        return fpsRange;
    }

    public Streamer.FpsRange nearestFpsRange(Streamer.FpsRange[] fpsRanges, float targetFps) {
        float minDistance = 1e10f;
        Streamer.FpsRange range = null;
        for (Streamer.FpsRange r : fpsRanges) {
            if (r.fpsMin > targetFps || r.fpsMax < targetFps) {
                continue;
            }
            float distance = ((r.fpsMax - targetFps) * (r.fpsMax - targetFps) + Math.abs(r.fpsMin - targetFps));
            if (distance < minDistance) {
                range = r;
                if (distance < 0.01f) {
                    break;
                }
                minDistance = distance;
            }
        }
        return range;
    }

    //Can be called only after camera initialization
    public void updateCameraParameters(Context context) {
        if (!(hasImportedActiveCamera() || hasImportedVideoSize() || hasImportedFpsRange())) {
            return;
        }
        boolean cam2 = SettingsUtils.isUsingCamera2(context);
        final List<CameraInfo> cameraList = CameraManager.getCameraList(context, cam2);
        if (cameraList == null || cameraList.size() == 0) {
            return;
        }
        CameraInfo info = SettingsUtils.getActiveCameraInfo(context, cameraList);
        if (hasImportedVideoSize()) {
            getVideoSize(info, context);
        }
        if (hasImportedFpsRange()) {
            findFpsRange(context, info.fpsRanges);
        }
    }

}
