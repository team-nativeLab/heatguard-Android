package com.nativelap.heartguard.view.screen.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nativelap.heartguard.R
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.brand.BrandMark

/** 저장된 세션을 확인하는 동안 시스템 스플래시에 이어 로그인 화면과 같은 로고를 화면 중앙에 보여 주는 인트로 화면이다. */
@Composable
internal fun IntroScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.authBackground,
        // 시스템 스플래시 아이콘과 같은 위치가 되도록 시스템 바 inset 없이 창 전체 중앙에 둔다.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            BrandMark(
                brandPainter = painterResource(R.drawable.heart_guard_logo),
                contentDescription = stringResource(R.string.brand_name),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IntroScreenPreview() {
    HeartGuardTheme {
        IntroScreen()
    }
}
