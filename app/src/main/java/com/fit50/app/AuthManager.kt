package com.fit50.app

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthManager(private val context: Context) {
    private var firebaseAuth: FirebaseAuth? = null
    var initializationError: String? = null
        private set

    val configured: Boolean
        get() = BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
            initializationError == null

    init {
        if (
            BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
            BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
            BuildConfig.FIREBASE_PROJECT_ID.isNotBlank()
        ) {
            runCatching {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val options = FirebaseOptions.Builder()
                        .setApiKey(BuildConfig.FIREBASE_API_KEY)
                        .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                        .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                        .apply {
                            if (BuildConfig.FIREBASE_STORAGE_BUCKET.isNotBlank()) {
                                setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                            }
                        }
                        .build()
                    FirebaseApp.initializeApp(context, options)
                }
                firebaseAuth = FirebaseAuth.getInstance()
            }.onFailure { error ->
                firebaseAuth = null
                initializationError = error.localizedMessage ?: error.javaClass.simpleName
            }
        }
    }

    fun isSignedIn(): Boolean = runCatching { firebaseAuth?.currentUser != null }.getOrDefault(false)

    fun displayName(): String = runCatching {
        firebaseAuth?.currentUser?.displayName
            ?: firebaseAuth?.currentUser?.email?.substringBefore('@')
            ?: "מתאמן"
    }.getOrDefault("מתאמן")

    fun signIn(email: String, password: String, done: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task -> done(task.isSuccessful, task.exception?.localizedMessage) }
    }

    fun register(email: String, password: String, done: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task -> done(task.isSuccessful, task.exception?.localizedMessage) }
    }

    fun resetPassword(email: String, done: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task -> done(task.isSuccessful, task.exception?.localizedMessage) }
    }

    suspend fun signInWithGoogle(activity: Activity, done: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return done(false, "חסר Google OAuth Web Client ID עבור Android")
        }

        try {
            val googleOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val result = CredentialManager.create(context).getCredential(activity, request)
            val credential = result.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        done(task.isSuccessful, task.exception?.localizedMessage)
                    }
            } else {
                done(false, "לא התקבל חשבון Google תקין")
            }
        } catch (error: Exception) {
            done(false, error.localizedMessage ?: "Google Sign-In נכשל")
        }
    }

    fun signOut() {
        runCatching { firebaseAuth?.signOut() }
    }
}
