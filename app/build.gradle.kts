plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun Project.fit50Prop(name: String, fallback: String = ""): String = providers.gradleProperty(name).orNull ?: fallback
fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.fit50.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.fit50.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_API_KEY", fit50Prop("FIT50_FIREBASE_API_KEY", "AIzaSyA-6Vv4xXV7FxG9SedTtxMl7YpNL5egmAY").asBuildConfigString())
        buildConfigField("String", "FIREBASE_APP_ID", fit50Prop("FIT50_FIREBASE_APP_ID", "1:1570044363:web:4d7260347d578d89c11e4e").asBuildConfigString())
        buildConfigField("String", "FIREBASE_PROJECT_ID", fit50Prop("FIT50_FIREBASE_PROJECT_ID", "fit50-plus").asBuildConfigString())
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", fit50Prop("FIT50_FIREBASE_STORAGE_BUCKET", "fit50-plus.firebasestorage.app").asBuildConfigString())
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", fit50Prop("FIT50_GOOGLE_WEB_CLIENT_ID").asBuildConfigString())
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.18.0")

    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
