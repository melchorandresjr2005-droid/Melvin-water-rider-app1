package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemeId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.waterRiderDataStore by preferencesDataStore(name = "melvin_water_rider_prefs")

data class BackupFolderConfig(
    val folderUri: String? = null,
    val folderName: String? = null,
    val lastAutoBackupTimestamp: Long = 0L
) {
    val isConnected: Boolean
        get() = !folderUri.isNullOrBlank()
}

data class DefaultPriceConfig(
    val defaultPurifiedPrice: Double = 35.0,
    val defaultAlkalinePrice: Double = 45.0
)

data class BossContactConfig(
    val messengerUsername: String = "",
    val phoneNumber: String = ""
) {
    val hasMessenger: Boolean
        get() = messengerUsername.isNotBlank()
}

class ThemePreferences(private val context: Context) {
    private val selectedThemeKey = intPreferencesKey("selected_app_theme_id")
    private val backupFolderUriKey = stringPreferencesKey("backup_folder_uri")
    private val backupFolderNameKey = stringPreferencesKey("backup_folder_name")
    private val lastAutoBackupTimestampKey = longPreferencesKey("last_auto_backup_timestamp")
    private val defaultPurifiedPriceKey = doublePreferencesKey("default_purified_price")
    private val defaultAlkalinePriceKey = doublePreferencesKey("default_alkaline_price")
    private val bossMessengerUsernameKey = stringPreferencesKey("boss_messenger_username")
    private val bossPhoneNumberKey = stringPreferencesKey("boss_phone_number")

    val selectedThemeFlow: Flow<AppThemeId> = context.waterRiderDataStore.data
        .map { prefs ->
            AppThemeId.fromId(prefs[selectedThemeKey] ?: AppThemeId.ROYAL_DAYLIGHT.id)
        }
        .flowOn(Dispatchers.IO)

    val backupConfigFlow: Flow<BackupFolderConfig> = context.waterRiderDataStore.data
        .map { prefs ->
            BackupFolderConfig(
                folderUri = prefs[backupFolderUriKey],
                folderName = prefs[backupFolderNameKey],
                lastAutoBackupTimestamp = prefs[lastAutoBackupTimestampKey] ?: 0L
            )
        }
        .flowOn(Dispatchers.IO)

    val defaultPriceConfigFlow: Flow<DefaultPriceConfig> = context.waterRiderDataStore.data
        .map { prefs ->
            DefaultPriceConfig(
                defaultPurifiedPrice = prefs[defaultPurifiedPriceKey] ?: 35.0,
                defaultAlkalinePrice = prefs[defaultAlkalinePriceKey] ?: 45.0
            )
        }
        .flowOn(Dispatchers.IO)

    val bossContactFlow: Flow<BossContactConfig> = context.waterRiderDataStore.data
        .map { prefs ->
            BossContactConfig(
                messengerUsername = prefs[bossMessengerUsernameKey] ?: "",
                phoneNumber = prefs[bossPhoneNumberKey] ?: ""
            )
        }
        .flowOn(Dispatchers.IO)

    suspend fun setSelectedTheme(theme: AppThemeId) = withContext(Dispatchers.IO) {
        context.waterRiderDataStore.edit { prefs ->
            prefs[selectedThemeKey] = theme.id
        }
    }

    suspend fun setDefaultPrices(purifiedPrice: Double, alkalinePrice: Double) = withContext(Dispatchers.IO) {
        context.waterRiderDataStore.edit { prefs ->
            prefs[defaultPurifiedPriceKey] = purifiedPrice
            prefs[defaultAlkalinePriceKey] = alkalinePrice
        }
    }

    suspend fun setBossContact(messengerUsername: String, phoneNumber: String) = withContext(Dispatchers.IO) {
        context.waterRiderDataStore.edit { prefs ->
            prefs[bossMessengerUsernameKey] = messengerUsername.trim()
            prefs[bossPhoneNumberKey] = phoneNumber.trim()
        }
    }

    suspend fun setBackupFolder(uriString: String, folderName: String?) = withContext(Dispatchers.IO) {
        context.waterRiderDataStore.edit { prefs ->
            prefs[backupFolderUriKey] = uriString
            if (!folderName.isNullOrBlank()) {
                prefs[backupFolderNameKey] = folderName
            } else {
                prefs.remove(backupFolderNameKey)
            }
        }
    }

    suspend fun clearBackupFolder() = withContext(Dispatchers.IO) {
        context.waterRiderDataStore.edit { prefs ->
            prefs.remove(backupFolderUriKey)
            prefs.remove(backupFolderNameKey)
        }
    }

    suspend fun setLastAutoBackupTimestamp(timestamp: Long) = withContext(Dispatchers.IO) {
        context.waterRiderDataStore.edit { prefs ->
            prefs[lastAutoBackupTimestampKey] = timestamp
        }
    }
}
