package com.fit50.app

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthManager(private val context: Context) {
    companion object {
        private const val TAG = "Fit50GoogleAuth"
    }

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
        val auth = ensureFirebase()
            ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")

        val normalized = email.trim().lowercase()
        if (!normalized.contains("@")) {
            return done(false, "כתובת אימייל אינה תקינה")
        }

        auth.sendPasswordResetEmail(normalized)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    done(true, "קישור איפוס נשלח לאימייל.")
                } else {
                    done(false, task.exception?.localizedMessage ?: "שליחת קישור האיפוס נכשלה")
                }
            }
    }

    suspend fun signInWithGoogle(activity: Activity, done: (Boolean, String?) -> Unit) {
        val auth = ensureFirebase()
            ?: return done(false, initializationError ?: "Firebase עדיין לא הוגדר בפרויקט")

        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return done(false, "חסר Google OAuth Web Client ID עבור Android")
        }

        try {
            // This request originates from an explicit Google button. The dedicated
            // option also supports adding/re-authenticating an account and avoids a
            // known account-picker failure on some Android 14+ devices.
            val googleOption = GetSignInWithGoogleOption.Builder(
                BuildConfig.GOOGLE_WEB_CLIENT_ID
            )
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
        } catch (error: GetCredentialCancellationException) {
            done(false, "החיבור עם Google בוטל. אפשר לנסות שוב.")
        } catch (error: NoCredentialException) {
            done(false, "לא נמצא חשבון Google זמין. הוסיפו חשבון Google למכשיר ונסו שוב.")
        } catch (error: GetCredentialProviderConfigurationException) {
            Log.w(TAG, "Google credential provider is unavailable", error)
            done(false, "שירות ההתחברות של Google אינו זמין. עדכנו את Google Play Services ונסו שוב.")
        } catch (error: GetCredentialUnsupportedException) {
            Log.w(TAG, "Credential Manager is unsupported", error)
            done(false, "המכשיר אינו תומך כרגע בחיבור Google. עדכנו את Android ואת Google Play Services.")
        } catch (error: GetCredentialInterruptedException) {
            Log.w(TAG, "Google sign-in was interrupted", error)
            done(false, "החיבור עם Google הופסק. נסו שוב.")
        } catch (error: Throwable) {
            Log.w(TAG, "Google sign-in failed", error)
            done(false, error.localizedMessage ?: "החיבור עם Google נכשל. נסו שוב.")
        }
    }

    fun signOut() {
        runCatching { ensureFirebase()?.signOut() }
    }
}
