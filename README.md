# Fit50+ for Android

Native Android version of **Fit50+**, built with Kotlin + Jetpack Compose.

## Included

- Fit50+ branding and supplied logo
- Email/password sign-in and registration with Firebase Auth
- Password reset email
- Google Sign-In using Android Credential Manager + Firebase Auth
- Guest mode so the rest of the app can be tested before Firebase is configured
- 7-step onboarding questionnaire
- Home dashboard
- Working exercise flow with per-exercise timers, pause/resume and rest screens
- Progress counters persisted locally
- Morning / afternoon / evening stretching routines
- Settings, logout and questionnaire reset
- RTL Hebrew UI
- GitHub Actions Android build

## Firebase / Google configuration

The repository intentionally does **not** contain Firebase secrets/config from a private Firebase project.
Set these Gradle properties in `~/.gradle/gradle.properties` locally, or as GitHub Actions secrets:

```properties
FIT50_FIREBASE_API_KEY=...
FIT50_FIREBASE_APP_ID=...
FIT50_FIREBASE_PROJECT_ID=...
FIT50_GOOGLE_WEB_CLIENT_ID=...
```

In Firebase Console:

1. Create/add Android app with package `com.fit50.app`.
2. Enable **Email/Password** and **Google** under Authentication providers.
3. Add the app signing SHA-1/SHA-256 fingerprints.
4. Use the **Web client ID** for `FIT50_GOOGLE_WEB_CLIENT_ID`.

No `google-services.json` is required by this source tree because Firebase is initialized explicitly from Gradle properties. This keeps the public repository buildable without committing project-specific configuration.

## Build

Open the repository in current stable Android Studio and run the `app` configuration.

Command-line CI uses Gradle 9.6 and AGP 9.4.0.
