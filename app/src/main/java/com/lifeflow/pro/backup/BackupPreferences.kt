package com.lifeflow.pro.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backupDataStore by preferencesDataStore(name = "backup_settings")

class BackupPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private object Keys {
        val autoBackupEnabled = booleanPreferencesKey("auto_backup_enabled")
        val destinationTreeUri = stringPreferencesKey("auto_backup_destination_tree")
    }

    val autoBackupEnabledFlow: Flow<Boolean> = context.backupDataStore.data.map { it[Keys.autoBackupEnabled] ?: false }
    val destinationTreeUriFlow: Flow<String?> = context.backupDataStore.data.map { it[Keys.destinationTreeUri] }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[Keys.autoBackupEnabled] = enabled }
    }

    suspend fun setDestinationTreeUri(uri: String?) {
        context.backupDataStore.edit { prefs ->
            if (uri == null) prefs.remove(Keys.destinationTreeUri) else prefs[Keys.destinationTreeUri] = uri
        }
    }
}
