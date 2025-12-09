package app.excoda.core.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GlobalSettingsViewModel @Inject constructor(
    val repository: GlobalSettingsRepository
) : ViewModel()