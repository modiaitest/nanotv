package app.nanotv.tv.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "nanotv_prefs")

data class AppSettingsState(
    val isLoaded: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val hasParentPin: Boolean = false,
    val familyProfileName: String = "Family Space",
    val autoplayEnabled: Boolean = true,
    val lastSyncEpochMs: Long = 0L,
    val lastSyncStatus: String = "Ready"
)

object PinHasher {
    private const val SALT = "nanotv_parent_guard_v1:"

    fun hashPin(pin: String): String {
        val clean = pin.trim()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((SALT + clean).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

class AppPreferences(private val context: Context) {
    private val keyOnboardingDone = booleanPreferencesKey("onboarding_completed")
    private val keyPinHash = stringPreferencesKey("parent_pin_sha256")
    private val keyProfileName = stringPreferencesKey("family_profile_name")
    private val keyAutoplay = booleanPreferencesKey("autoplay_enabled")
    private val keyLastSyncMs = longPreferencesKey("last_sync_epoch_ms")
    private val keyLastSyncStatus = stringPreferencesKey("last_sync_status")

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        val pinHash = prefs[keyPinHash].orEmpty()
        AppSettingsState(
            isLoaded = true,
            onboardingCompleted = prefs[keyOnboardingDone] ?: false,
            hasParentPin = pinHash.isNotBlank(),
            familyProfileName = prefs[keyProfileName]?.ifBlank { "Family Space" } ?: "Family Space",
            autoplayEnabled = prefs[keyAutoplay] ?: true,
            lastSyncEpochMs = prefs[keyLastSyncMs] ?: 0L,
            lastSyncStatus = prefs[keyLastSyncStatus] ?: "Ready"
        )
    }

    suspend fun completeParentOnboarding(pin: String, profileName: String) {
        val hashed = PinHasher.hashPin(pin)
        context.dataStore.edit { prefs ->
            prefs[keyPinHash] = hashed
            prefs[keyProfileName] = profileName.trim().ifBlank { "Family Space" }
            prefs[keyOnboardingDone] = true
        }
    }

    suspend fun updateParentPin(newPin: String) {
        val hashed = PinHasher.hashPin(newPin)
        context.dataStore.edit { prefs ->
            prefs[keyPinHash] = hashed
        }
    }

    suspend fun verifyParentPin(candidatePin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val savedHash = prefs[keyPinHash].orEmpty()
        if (savedHash.isBlank()) return true
        return PinHasher.hashPin(candidatePin) == savedHash
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keyAutoplay] = enabled }
    }

    suspend fun updateSyncResult(epochMs: Long, status: String) {
        context.dataStore.edit { prefs ->
            prefs[keyLastSyncMs] = epochMs
            prefs[keyLastSyncStatus] = status
        }
    }
}
