package com.aipoweredgita.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages authentication state and credential storage.
 * Thread-safe: coin mutations use commit() + synchronized to prevent race conditions.
 * Security: password and token are stored in EncryptedSharedPreferences.
 */
class AuthPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    /** Encrypted prefs for sensitive data (password, auth token). */
    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "auth_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_GUEST_ID = "guest_id"
        private const val KEY_LOGIN_METHOD = "login_method"
        private const val KEY_PHONE = "phone"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_TOKEN = "auth_token"

        @Volatile
        private var INSTANCE: AuthPreferences? = null

        fun getInstance(context: Context): AuthPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ── User State ─────────────────────────────────────────────────────

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isGuest: Boolean
        get() = prefs.getBoolean(KEY_IS_GUEST, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_GUEST, value).apply()

    var guestId: String?
        get() = prefs.getString(KEY_GUEST_ID, null)
        set(value) = prefs.edit().putString(KEY_GUEST_ID, value).apply()

    // ── Credentials ────────────────────────────────────────────────────

    var loginMethod: String?
        get() = prefs.getString(KEY_LOGIN_METHOD, null) // "phone" or "email"
        set(value) = prefs.edit().putString(KEY_LOGIN_METHOD, value).apply()

    var phone: String?
        get() = prefs.getString(KEY_PHONE, null)
        set(value) = prefs.edit().putString(KEY_PHONE, value).apply()

    var email: String?
        get() = prefs.getString(KEY_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var name: String?
        get() = prefs.getString(KEY_NAME, null)
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** Auth token stored in EncryptedSharedPreferences (not plaintext). */
    var token: String?
        get() = securePrefs.getString(KEY_TOKEN, null)
        set(value) = securePrefs.edit().putString(KEY_TOKEN, value).apply()

    var rememberMe: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER_ME, true)
        set(value) = prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply()

    var lastLogin: Long
        get() = prefs.getLong(KEY_LAST_LOGIN, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_LOGIN, value).apply()

    // ── Guest Local Coins ──────────────────────────────────────────────
    // Guests earn coins locally since /guest/create is broken on server

    var guestWelcomeAwarded: Boolean
        get() = prefs.getBoolean("guest_welcome_awarded", false)
        set(value) = prefs.edit().putBoolean("guest_welcome_awarded", value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    /** Convenience: true if this is a guest user or not logged in. */
    val isGuestUser: Boolean get() = !isLoggedIn || isGuest

    // ── Helper Methods ─────────────────────────────────────────────────

    /**
     * Save login state after successful authentication.
     * Token is stored in EncryptedSharedPreferences.
     * Password is never stored — if token expires, user must re-login.
     */
    fun saveLoginState(
        userId: String,
        name: String = "",
        loginMethod: String,
        token: String? = null,
        phone: String? = null,
        email: String? = null
    ) {
        // Use commit() (synchronous) so that isLoggedIn, userId, and token are immediately
        // readable after login — async apply() can cause stale reads in the UI right after login
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_GUEST, false)
            putString(KEY_LOGIN_METHOD, loginMethod)
            putString(KEY_NAME, name)
            phone?.let { putString(KEY_PHONE, it) }
            email?.let { putString(KEY_EMAIL, it) }
            putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            remove("guest_welcome_awarded") // Reset guest flag
            commit() // synchronous — ensures values are written before onLoginSuccess navigates
        }
        // Store token in encrypted prefs — also synchronous
        securePrefs.edit().apply {
            token?.let { putString(KEY_TOKEN, it) }
            commit() // synchronous — token must be readable immediately after login
        }
    }

    /**
     * Save guest state
     */
    fun saveGuestState(guestId: String) {
        prefs.edit().apply {
            putString(KEY_GUEST_ID, guestId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_GUEST, true)
            putString(KEY_USER_ID, guestId)
            putString(KEY_LOGIN_METHOD, "guest")
            putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            apply()
        }
    }

    /**
     * Get saved credentials for auto-fill (phone/email only — password is never stored).
     */
    fun getSavedCredentials(): Triple<String?, String?, String?> {
        return when (loginMethod) {
            "phone" -> Triple(phone, null, null)
            "email" -> Triple(email, null, null)
            else -> Triple(null, null, null)
        }
    }

    /**
     * Check if user has a previous guest session
     */
    fun hasGuestSession(): Boolean {
        return guestId != null && isGuest
    }

    /**
     * Clear login state (logout)
     * Credentials (email/phone/name) are always preserved for quick re-login.
     * Token and password are cleared from encrypted storage.
     */
    fun clearLoginState() {
        prefs.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_IS_LOGGED_IN)
            remove(KEY_IS_GUEST)
            remove(KEY_GUEST_ID)
            remove(KEY_LOGIN_METHOD)
            remove(KEY_LAST_LOGIN)
            // Reset guest welcome flag on logout to start fresh
            remove("guest_welcome_awarded")
            // Always preserve email/phone/name for quick re-login
            apply()
        }
        // Clear sensitive data from encrypted storage
        securePrefs.edit().clear().apply()
    }

    /**
     * Clear all data including credentials
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        securePrefs.edit().clear().apply()
    }
}