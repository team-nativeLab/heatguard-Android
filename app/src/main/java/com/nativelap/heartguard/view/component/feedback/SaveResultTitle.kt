package com.nativelap.heartguard.view.component.feedback

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.nativelap.heartguard.ui.theme.HeartGuardFontSize
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing

/** 저장 결과 화면(성공/실패) 상단에 공통으로 표시되는 화면 제목이다.
 *  Figma 09_저장성공/10_저장실패 스펙: 가운데 정렬된 Bold 제목이며, 두 화면에서 재사용된다. */
@Composable
fun SaveResultTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        modifier = modifier
            .fillMaxWidth()
            .height(HeartGuardSpacing.ResultTitleHeight),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = HeartGuardFontSize.ResultTitle,
            lineHeight = HeartGuardFontSize.ResultTitleLineHeight,
            fontWeight = FontWeight.Bold,
        ),
    )
}
