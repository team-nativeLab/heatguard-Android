package com.nativelap.heartguard.navigation

import androidx.lifecycle.ViewModel
import com.nativelap.heartguard.core.session.AppLinkTokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class AppLinkTokenViewModel
    @Inject
    constructor(
        private val appLinkTokenStore: AppLinkTokenStore,
    ) : ViewModel() {
        val pendingToken = appLinkTokenStore.pendingToken

        fun consume(token: String) {
            appLinkTokenStore.consume(token)
        }
    }
