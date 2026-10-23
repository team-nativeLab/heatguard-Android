package com.nativelap.heartguard.core.session

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 딥링크에서 받은 일회용 현장 토큰을 연결 화면이 소비할 때까지 메모리에만 보관한다. */
@Singleton
class AppLinkTokenStore
    @Inject
    constructor() {
        private val mutablePendingToken = MutableStateFlow<String?>(null)

        val pendingToken: StateFlow<String?> = mutablePendingToken.asStateFlow()

        fun accept(uri: Uri?) {
            val token =
                uri
                    ?.takeIf { deepLink -> deepLink.scheme == SUPPORTED_SCHEME }
                    ?.getQueryParameter(TOKEN_QUERY_PARAMETER)
                    ?.takeIf(String::isNotBlank)
            if (token != null) {
                mutablePendingToken.value = token
            }
        }

        fun consume(token: String) {
            if (mutablePendingToken.value == token) {
                mutablePendingToken.value = null
            }
        }

        private companion object {
            const val SUPPORTED_SCHEME = "heatguard"
            const val TOKEN_QUERY_PARAMETER = "token"
        }
    }
