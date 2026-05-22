package com.klogviewer.domain.repository

import com.klogviewer.domain.model.UserPreferences

interface PreferencesRepository {
    fun load(): UserPreferences
    fun save(preferences: UserPreferences)
}
