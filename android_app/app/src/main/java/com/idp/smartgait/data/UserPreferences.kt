package com.idp.smartgait.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Top-level extension – single DataStore instance per process. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_gait_prefs")

/**
 * Local prototype authentication and profile persistence.
 *
 * This is a student-prototype implementation.  It hashes passwords with
 * [String.hashCode] which is NOT cryptographically secure.  A production
 * app should use a proper backend with secure credential storage.
 */
class UserPreferences(private val dataStore: DataStore<Preferences>) {

    /* ── Keys ─────────────────────────────────────────── */
    private companion object {
        val IS_LOGGED_IN       = booleanPreferencesKey("is_logged_in")
        val ONBOARDING_DONE    = booleanPreferencesKey("onboarding_done")
        val USER_NAME          = stringPreferencesKey("user_name")
        val USER_EMAIL         = stringPreferencesKey("user_email")
        val PASSWORD_HASH      = stringPreferencesKey("password_hash")
        val USER_AGE           = intPreferencesKey("user_age")
        val USER_HEIGHT        = floatPreferencesKey("user_height")
        val USER_WEIGHT        = floatPreferencesKey("user_weight")
        val ACTIVITY_TYPE      = stringPreferencesKey("activity_type")
    }

    /* ── Flows ────────────────────────────────────────── */
    val isLoggedIn: Flow<Boolean> =
        dataStore.data.map { it[IS_LOGGED_IN] ?: false }

    val onboardingComplete: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    val userProfile: Flow<UserProfile> =
        dataStore.data.map { p ->
            UserProfile(
                name         = p[USER_NAME]     ?: "",
                email        = p[USER_EMAIL]    ?: "",
                age          = p[USER_AGE]      ?: 0,
                height       = p[USER_HEIGHT]   ?: 0f,
                weight       = p[USER_WEIGHT]   ?: 0f,
                activityType = p[ACTIVITY_TYPE] ?: ""
            )
        }

    /* ── Suspend helpers (one-shot reads) ─────────────── */
    suspend fun isLoggedInNow(): Boolean =
        dataStore.data.first()[IS_LOGGED_IN] ?: false

    suspend fun isOnboardingDone(): Boolean =
        dataStore.data.first()[ONBOARDING_DONE] ?: false

    suspend fun getUserProfile(): UserProfile {
        val p = dataStore.data.first()
        return UserProfile(
            name         = p[USER_NAME]     ?: "",
            email        = p[USER_EMAIL]    ?: "",
            age          = p[USER_AGE]      ?: 0,
            height       = p[USER_HEIGHT]   ?: 0f,
            weight       = p[USER_WEIGHT]   ?: 0f,
            activityType = p[ACTIVITY_TYPE] ?: ""
        )
    }

    /* ── Actions ──────────────────────────────────────── */
    suspend fun completeOnboarding() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    /** Returns true on success. */
    suspend fun signup(name: String, email: String, password: String): Boolean {
        dataStore.edit { prefs ->
            prefs[USER_NAME]      = name
            prefs[USER_EMAIL]     = email
            prefs[PASSWORD_HASH]  = password.hashCode().toString()
            prefs[IS_LOGGED_IN]   = true
        }
        return true
    }

    /** Returns true if credentials match. */
    suspend fun login(email: String, password: String): Boolean {
        val prefs = dataStore.data.first()
        val storedEmail = prefs[USER_EMAIL]    ?: return false
        val storedHash  = prefs[PASSWORD_HASH] ?: return false
        return if (email == storedEmail && password.hashCode().toString() == storedHash) {
            dataStore.edit { it[IS_LOGGED_IN] = true }
            true
        } else false
    }

    suspend fun logout() {
        dataStore.edit { it[IS_LOGGED_IN] = false }
    }

    suspend fun saveProfile(profile: UserProfile) {
        dataStore.edit { prefs ->
            prefs[USER_NAME]      = profile.name
            prefs[USER_EMAIL]     = profile.email
            prefs[USER_AGE]       = profile.age
            prefs[USER_HEIGHT]    = profile.height
            prefs[USER_WEIGHT]    = profile.weight
            prefs[ACTIVITY_TYPE]  = profile.activityType
        }
    }
}

