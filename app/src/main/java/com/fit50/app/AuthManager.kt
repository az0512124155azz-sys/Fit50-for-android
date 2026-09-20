package com.fit50.app

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager(private val context: Context) {
    private var firebaseAuth: FirebaseAuth? = null
    var initializationError: String? = null
        private set

    val configured: Boolean
        get() = runCatching {
            ensureFirebase() != null
        }.getOrDefault(false)

    private fun ensureFirebase(): FirebaseAuth? {
        firebaseAuth?.let { return it }

        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance().also {
                firebaseAuth = it
                initializationError = null
            }
        }.getOrElse { error ->
            firebaseAuth = null
            initializationError = error.localizedMessage ?: error.javaClass.simpleName
            null
        }
    }

    fun isSignedIn(): Boolean = runCatching {
        ensureFirebase()?.currentUser != null
    }.getOrDefault(false)

    fun displayName(): String = runCatching {
        ensureFirebase()?.currentUser?.displayName
            ?: ensureFirebase()?.currentUser?.email?.substringBefore('@')
            ?: "מתאמן"
    }.getOrDefault("מתאמן")

    fun signIn(email: String, password: String, done: (Boolean, String?) -> Unit) {
        val auth = ensureFirebase()
            ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                done(task.isSuccessful, task.exception?.localizedMessage)
            }
    }

    fun register(email: String, password: String, done: (Boolean, String?) -> Unit) {
        val auth = ensureFirebase()
            ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    done(false, task.exception?.localizedMessage)
                } else {
                    val user = auth.currentUser
                    if (user == null) {
                        done(false, "החשבון נוצר אך המשתמש לא נטען")
                    } else {
                        user.sendEmailVerification()
                            .addOnCompleteListener {
                                done(true, null)
                            }
                    }
                }
            }
    }

    fun resetPassword(email: String, done: (Boolean, String?) -> Unit) {
        ensureFirebase()
            ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")

        val normalized = email.trim().lowercase()
        if (!normalized.contains("@")) {
            return done(false, "כתובת אימייל אינה תקינה")
        }

        val db = runCatching { FirebaseFirestore.getInstance() }.getOrNull()
            ?: return done(false, "לא הצלחנו להתחבר לשירות איפוס הסיסמה")

        val request = hashMapOf<String, Any?>(
            "email" to normalized,
            "source" to "app",
            "status" to "queued",
            "createdAt" to FieldValue.serverTimestamp(),
            "error" to ""
        )

        db.collection("passwordResetRequests")
            .add(request)
            .addOnSuccessListener {
                done(true, "בקשת האיפוס התקבלה. קישור יישלח לאימייל בתוך כדקה.")
            }
            .addOnFailureListener { error ->
                done(false, error.localizedMessage ?: "לא הצלחנו ליצור בקשת איפוס")
            }
    }

    suspend fun signInWithGoogle(activity: Activity, done: (Boolean, String?) -> Unit) {
        val auth = ensureFirebase()
            ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")

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
        } catch (error: Throwable) {
            done(false, error.localizedMessage ?: "Google Sign-In נכשל")
        }
    }

    fun signOut() {
        runCatching { ensureFirebase()?.signOut() }
    }
}
