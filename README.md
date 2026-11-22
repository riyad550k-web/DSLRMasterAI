# DSLR Master AI — Android starter project

A native Android (Kotlin + Jetpack Compose) starter for DSLR Master AI.

## Included in this first build stage
- Dark premium-style home dashboard
- CameraX live camera preview and photo capture (runtime camera permission)
- System photo picker
- Basic brightness/contrast editing and JPEG export to Pictures/DSLRMasterAI
- AI Studio feature entry points (AI provider/API integration still required)
- Gallery landing screen

## Build
1. Install Android Studio (Ladybug or newer) and JDK 17.
2. Open this folder in Android Studio.
3. Allow Gradle sync and install Android SDK 35 if prompted.
4. Run on a physical Android device for camera testing.

## Important
This is the first functional foundation, not a finished Play Store release. AI tools are UI entry points until an AI service/API and secure backend are configured. Advanced DSLR controls depend on device Camera2 capabilities and need device-by-device testing. Before release, complete privacy policy, data safety, app signing, target API compliance, icons, screenshots, and Play Console checks.
