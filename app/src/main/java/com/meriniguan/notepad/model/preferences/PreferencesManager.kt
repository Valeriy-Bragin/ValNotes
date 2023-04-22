package com.meriniguan.notepad.model.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

private const val TAG = "PreferencesManager"

enum class SortOrder { BY_TITLE, BY_DATE_CREATED, BY_DATE_UPDATED }

class UserPreferences(val sortOrder: SortOrder, val isReducedView: Boolean)

class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private val Context._dataStore by preferencesDataStore("user_preferences")
    }

    private val dataStore : DataStore<Preferences> = context._dataStore

    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(
                preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.BY_DATE_CREATED.name
            )
            val isReducedView = preferences[PreferencesKeys.IS_REDUCED_VIEW] ?: false
            UserPreferences(sortOrder, isReducedView)
        }

    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun toggleIsReducedView() {
        dataStore.edit { preferences ->
            val currentIsReducedView = preferences[PreferencesKeys.IS_REDUCED_VIEW] ?: false
            preferences[PreferencesKeys.IS_REDUCED_VIEW] = !currentIsReducedView
        }
    }

    private object PreferencesKeys {
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val IS_REDUCED_VIEW = booleanPreferencesKey("is_reduced_view")
    }
}