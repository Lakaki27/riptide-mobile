package com.evertide.riptide.ui.common

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel class ToastBusHolder @Inject constructor(val bus: ToastBus) : ViewModel()
