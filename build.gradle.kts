// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

// 코드 작성 후 자동 줄바꿈(포맷)을 위한 Spotless·ktlint 설정. `./gradlew spotlessApply`로 포맷하고 `spotlessCheck`로 확인한다.
// Spotless는 .editorconfig를 읽지 않으므로 아래 값이 기준이며, IDE 포맷과 맞추려면 .editorconfig도 같은 값으로 유지한다.
val ktlintEditorConfig =
    mapOf(
        "ktlint_code_style" to "ktlint_official",
        "max_line_length" to 120,
        // 매개변수가 둘 이상이면 함수 시그니처를 매개변수마다 줄바꿈한다
        "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to 2,
        "ij_kotlin_allow_trailing_comma" to true,
        "ij_kotlin_allow_trailing_comma_on_call_site" to true,
        // Compose 함수(대문자 시작)와 한글 등 서술형 이름을 쓰는 테스트 함수는 함수 이름 규칙에서 제외한다
        "ktlint_function_naming_ignore_when_annotated_with" to "Composable,Test",
        // 줄바꿈과 무관하게 상수·파일 이름 변경을 요구하는 규칙은 포맷 범위 밖이라 끈다
        "ktlint_standard_property-naming" to "disabled",
        "ktlint_standard_filename" to "disabled",
    )

spotless {
    kotlin {
        // 단일 :app 모듈 소스만 대상으로 해 로컬 worktree·빌드 산출물이 포맷되지 않게 한다
        target("app/src/**/*.kt")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintEditorConfig)
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint(libs.versions.ktlint.get()).editorConfigOverride(ktlintEditorConfig)
    }
}
