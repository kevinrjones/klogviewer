package com.klogviewer.domain.repository

import com.klogviewer.domain.model.UserPreferences

data class PreferencesSaveOptions(
    val allowPlaintextSecretFallback: Boolean = false
)

sealed interface PreferencesSaveResult {
    data object Saved : PreferencesSaveResult
    data object RequiresPlaintextSecretConfirmation : PreferencesSaveResult
    data class Failed(val reason: String?) : PreferencesSaveResult
}

interface PreferencesRepository {
    fun load(): UserPreferences
    fun save(
        preferences: UserPreferences,
        options: PreferencesSaveOptions = PreferencesSaveOptions()
    ): PreferencesSaveResult
}
