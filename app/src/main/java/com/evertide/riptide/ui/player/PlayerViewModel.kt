package com.evertide.riptide.ui.player

import androidx.lifecycle.ViewModel
import com.evertide.riptide.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel
@Inject
constructor(
        val playerRepository: PlayerRepository,
) : ViewModel()
