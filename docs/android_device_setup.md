# Run PrioSort on an Android device

PrioSort is a native Android application. The app targets Android API 36 and
supports Android 8.0 (API 26) and newer. Notification capture uses Android's
Notification Listener service and is processed locally in the app's Room
database.

## Build and install

1. Install Android Studio, Android SDK Platform 36, and JDK 17.
2. Open this repository's `android/` directory in Android Studio and allow Gradle
   sync to finish. The checked-in wrapper uses Gradle 8.11.1 and the project uses
   Android Gradle Plugin 8.9.1.
3. Select a connected Android device with USB debugging enabled, or start an
   emulator running API 26 or newer.
4. Run the `app` configuration, or use `gradlew.bat :app:assembleDebug` from
   `android/`.
5. Android Studio installs the debug app. For a manual install, use the APK at
   `android/app/build/outputs/apk/debug/app-debug.apk`.

## Enable notification capture

On the device, open **Settings → Apps → Special app access → Notification
access** (wording varies by Android version), enable **PrioSort**, and confirm
the system prompt. Return to PrioSort; incoming notifications will appear in
the Smart Inbox after capture is active. The app can only reorder notifications
inside its Smart Inbox; Android does not let third-party apps reorder the system
notification shade.

The current API 36 debug build is available at `artifacts/PrioSort-debug.apk`.
It is debug-signed for testing and sideloading. The SHA-256 digest is recorded in
`artifacts/PrioSort-debug.apk.sha256`. Physical-device behavior still needs to be
verified on the target Android devices.

## Build environment note

The Gradle wrapper scripts and wrapper JAR are included in this project. The
first build may need internet access to download Gradle and Android dependencies.
