package com.example.customlauncher.core.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import com.example.customlauncher.core.datastore.model.UserPreferences
import java.io.IOException
import javax.inject.Inject

class ClPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>
) {
    val userData = userPreferences.data

    suspend fun setMining(mining: Boolean) {
        try {
            userPreferences.updateData {
                it.copy(isMiningOn = mining)
            }
        } catch (ioException: IOException) {
            Log.e("ClPreferences", "Failed to update user preferences", ioException)
        }
    }
}