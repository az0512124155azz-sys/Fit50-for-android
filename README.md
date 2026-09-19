# Fit50+ for Android

Native Android version of **Fit50+**, built with Kotlin + Jetpack Compose.

## Included

- Fit50+ branding and the supplied logo
- Email/password sign-in and registration with Firebase Authentication
- Password reset email ("Forgot password")
- Google Sign-In using Android Credential Manager + Firebase Authentication
- Guest mode so the complete fitness flow can be tested before Firebase is configured
- 7-step onboarding questionnaire
- Home dashboard
- Working exercise session with per-exercise timers, pause/resume and rest screens
- Progress counters persisted locally
- Android share sheet for progress sharing
- Morning / afternoon / evening stretching routines
- Settings, logout and questionnaire reset
- Full RTL Hebrew UI
- GitHub Actions debug APK build

## Firebase / Google configuration

Firebase/Google credentials belong to your Firebase project and are intentionally not hard-coded into this public repository.
Set these Gradle properties locally, or use matching GitHub Actions secrets:

```properties
FIT50_FIREBASE_API_KEY=...
FIT50_FIREBASE_APP_ID=...
FIT50_FIREBASE_PROJECT_ID=...
FIT50_GOOGLE_WEB_CLIENT_ID=...
```

In Firebase Console:

1. Add an Android app with package `com.fit50.app`.
2. Enable **Email/Password** and **Google** under Authentication providers.
3. Add the signing SHA-1 and SHA-256 fingerprints.
4. Add the four values above as local Gradle properties or GitHub Actions secrets.
5. Use the Firebase/Google **Web client ID** for `FIT50_GOOGLE_WEB_CLIENT_ID`.

The app initializes Firebase from these build properties, so `google-services.json` is not committed to the repository.

## Build

- Android Gradle Plugin: 9.1.1
- Gradle CI: 9.3.1
- JDK: 17
- compileSdk: 37
- targetSdk: 36
- minSdk: 26
- Compose BOM: 2026.09.00

Open the repository in a current Android Studio version and run the `app` configuration, or use the GitHub Actions **Android Build** workflow to obtain the debug APK artifact.
