package com.nativelap.heartguard

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nativelap.heartguard.core.session.AppLinkTokenStore
import com.nativelap.heartguard.navigation.HeartGuardNavHost
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Hilt가 이 Activity를 진입점으로 인식하도록 @AndroidEntryPoint가 반드시 필요하다.
 * 이 어노테이션이 없으면 Activity가 Hilt 인식 ViewModelProvider.Factory를 갖지 못해,
 * hiltViewModel()로 생성자 의존성이 있는 ViewModel(HeartGuardSessionViewModel 등)을
 * 만들려는 모든 곳에서 런타임에 크래시한다. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var appLinkTokenStore: AppLinkTokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptIncomingLink(intent)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    scrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    scrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT,
                ),
        )
        setContent {
            HeartGuardTheme {
                HeartGuardNavHost()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        acceptIncomingLink(intent)
    }

    private fun acceptIncomingLink(incomingIntent: Intent?) {
        val link = incomingIntent?.dataString ?: return
        appLinkTokenStore.accept(link)
        setIntent(Intent(incomingIntent).apply { data = null })
    }
}
