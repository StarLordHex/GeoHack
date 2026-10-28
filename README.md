# GeoHack v0.2 — experimental background radar

Android Kotlin/Jetpack Compose source project. **Not compiled or device-tested** in this environment.

## New in 0.2
- Opt-in foreground service to scan for and advertise GeoHack BLE packets while the phone is locked.
- Runtime Bluetooth/notification permissions, ongoing radar notification, Stop action.
- Rotating random 8-byte advertisement tokens (15-minute interval).
- Locally saved anonymous encountered tokens, alert notification, and Radar refresh button.
- Existing v0.1 demo terminal, targets, leaderboard, and Python tests retained.

## Important limitations
- BLE behaviour depends on handset, Android power management, manufacturer, radio support, and permissions.
- Some handsets cannot advertise and scan simultaneously. BLE advertisement payload size may exceed legacy limits on some devices.
- Tokens are **not verified player identities**. Encounter history currently records anonymous tokens only; repeated rotating tokens may represent the same phone. No secure proof of physical proximity or protection against spoofing/replay yet.
- The old simulated Target list is separate from BLE token history. Do not interpret discovered tokens as permission to attack a real device.
- Notifications may be blocked if permission is denied. Service must be manually enabled by the user and may be terminated by the OS.
- QR verification, backend identity resolution, persistence migration, and server-validated leaderboards are not yet implemented.
- No APK is included. Android compilation, integration tests and physical two-phone BLE testing are still required.

## Build
Open this folder in Android Studio on a suitable machine; use Android SDK 35, JDK 17. Gradle sync, compile, and install on two BLE-capable Android phones for testing. **Back up important files before installing development software.**

## Python rule tests
`python -m unittest discover -s tests -v`
`python -m ai_lab.engineer`

## Automatic APK builds with GitHub Actions

This project includes `.github/workflows/android-apk.yml`.

1. Create a new GitHub repository (private is fine for testing).
2. Upload the *contents* of this extracted project folder to the repository root, including the hidden `.github` folder. GitHub's browser upload accepts dragged files but may require uploading folders from a local Git client; GitHub Desktop is another option.
3. In the repository, select **Actions** > **Build GeoHack Android APK** > **Run workflow**.
4. If the build succeeds, open the workflow run, find **Artifacts**, download **GeoHack-v0.2-debug-apk**, and unzip it to obtain `app-debug.apk`.
5. Transfer the APK to your Android phone and install it. Only install APKs you trust. This is a development/debug build, not a Play Store release.

The workflow also runs automatically when source is pushed to `main` or `master`.

**Build status:** This Android project has NOT been compiled here; cloud build may expose code or configuration issues. The Python rules tests do not prove the Android app compiles. No hosting, GitHub repository, or automation has been activated on your account by this ZIP alone.
