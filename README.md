# t3_vcs_recorder
Alias = vcs
All passwords = Checkmate@321

## Internal build notes

Smoke checklist (manual):
- Switch Camera/USB/Cast/Audio while streaming: no blank; timestamp overlay during gaps.
- Start/stop recording during live stream: stream persists.
- Rotate/mirror/flip changes: applied without EGL restart.
- Change bitrate/fps in settings: updates on-the-fly via updateStreamingConfig/updateRecordingConfig.
- Kill/restart a background service: stream continues via shared EGL; preview updates to new active service.

Build requirements:
- Install Android SDK locally and set `sdk.dir` in `local.properties` or export `ANDROID_HOME`.