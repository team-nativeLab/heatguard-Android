# HeartGuard (heatguard-Android)

폭염 현장에서 일하는 작업자를 위해 체온 기록, 긴급 호출, 작업/휴식 사진 기록을 지원하는 Android 앱입니다.

## 목차
- [개요](#개요)
- [주요 화면](#주요-화면)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [테스트](#테스트)
- [개발 워크플로](#개발-워크플로)
- [문서](#문서)
- [라이선스](#라이선스)

## 개요
폭염 환경에서 작업하는 현장 인력의 체온·작업/휴식 상태를 기록하고, 위험 상황 발생 시 관리자에게 긴급 호출을 보낼 수 있도록 돕는 것을 목표로 하는 Android 앱입니다.
현재 코드베이스에는 로그인/회원가입 이후 홈, 긴급 호출, 기록 유형 선택, 온도계 기록, 작업/휴식/현장 사진 촬영, 저장 확인·성공·실패 화면까지의 화면 흐름이 Jetpack Compose Navigation 3(`NavDisplay`)로 구현되어 있습니다.

PRD 문서는 저장소에 없습니다. 제품 목표·요구사항의 공식 출처는 `확인 필요`입니다.

## 주요 화면
`app/src/main/java/com/nativelap/heartguard/navigation/HeartGuardDestination.kt`에 정의된 목적지 기준입니다.

| 화면 | 설명 |
|---|---|
| Login / SignUp | 로그인, 회원가입 |
| Home | 폭염 위험 요약, 체크 타임라인, 기록·긴급 호출 진입 |
| Emergency / Calling | 관리자 긴급 호출, 호출 중 상태 |
| RecordTypeSelection | 온도계 기록 / 작업 사진 / 휴식 사진 중 기록 유형 선택 (바텀시트) |
| TemperatureRecord | 온도계 측정값 입력 |
| FieldPhoto / WorkPhoto / RestPhoto | 현장·작업·휴식 사진 촬영 |
| SaveConfirmation | 저장 전 확인 다이얼로그 |
| SaveSuccess / SaveFailure | 저장 성공/실패 결과 |

`HeartGuardNavHost.kt`에는 실제 서버 연동 전까지 저장 성공/실패를 번갈아 시뮬레이션하는 임시 로직이 `TODO` 주석과 함께 남아 있습니다. 로그인 상태는 `core/session/SessionManager`가 Keystore 기반 토큰 저장소(`TokenStorage`)를 통해 관리하며, `HeartGuardNavHost`가 이 상태(`SessionState`)를 구독해 인증/비인증 화면을 전환합니다. 다만 로그인 성공 시 저장하는 값은 실제 로그인 API가 아직 없어 placeholder 토큰이며(`HeartGuardSessionViewModel.onLoginSucceeded()`의 TODO 참고), 실제 서버 인증 연동은 되어 있지 않습니다.

## 기술 스택
`gradle/libs.versions.toml`, `app/build.gradle.kts` 기준으로 확인한 값입니다.

| 영역 | 사용 기술 |
|---|---|
| 언어 | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01), Material 3 |
| Navigation | AndroidX Navigation3 (`navigation3-runtime`, `navigation3-ui` 1.1.6), `NavDisplay` |
| DI | Hilt |
| 네트워크 | Retrofit, OkHttp, kotlinx.serialization 컨버터 |
| 직렬화 | kotlinx.serialization.json 1.8.1 |
| 빌드 | Gradle (Kotlin DSL), Version Catalog, AGP 9.2.1 |
| minSdk / targetSdk / compileSdk | 34 / 37 / 37 |

DI(Hilt)와 네트워크(Retrofit/OkHttp) 의존성은 `app/build.gradle.kts`에 이미 추가되어 있고, `core/di`·`core/network`·`core/session`에 Hilt 모듈, Retrofit 서비스 생성 팩토리(`ApiRetrofitFactory`), 공통 API 결과 타입(`ApiResult`/`ApiError`), 세션 관리 골격이 구성되어 있습니다. 다만 이 하부 구조 위에 얹는 화면별 Repository·UseCase·ViewModel은 아직 없으며(세션 상태 구독용 `HeartGuardSessionViewModel` 1개 제외), 로컬 저장소(Room 등)도 아직 도입되지 않았습니다. 현재는 Presentation(Compose UI) 계층과 네트워크 하부 구조는 갖춰졌지만 그 둘을 잇는 Domain/Data 계층이 비어 있는 상태입니다.

## 아키텍처
`AGENTS.md`는 이 저장소 계열(hopes, BookOn, HeartGuard, moil)의 공통 계약으로 MVVM + Presentation/Domain/Data/Core 계층 분리, ViewModel + StateFlow 단방향 데이터 흐름을 기본값으로 명시합니다. 다만 현재 HeartGuard 코드베이스는 이 계약을 아직 전면적으로 구현하지 않았으며, 다음과 같이 화면(View) 계층만 계층화되어 있습니다.

```
view/
├── route/       # 화면별 진입점(Route) — 콜백을 받아 Screen을 조립
├── screen/      # 화면 단위 Composable(Scaffold 포함)
└── component/   # 화면별 재사용 Composable (auth, home, emergency, photo, temperature, feedback 등)
navigation/      # HeartGuardDestination(Navigation3 목적지), HeartGuardNavHost, 커스텀 SceneStrategy
core/component/  # 다이얼로그 배경 블러 등 공통 UI 유틸
ui/theme/        # Theme, Shapes, Type, Dimension
```

ViewModel, UseCase, Repository, DataSource 계층의 도입 여부와 시점은 `확인 필요`입니다.

## 프로젝트 구조
```
HeartGuard/
├── app/
│   └── src/main/
│       ├── java/com/nativelap/heartguard/
│       │   ├── MainActivity.kt
│       │   ├── navigation/   # Navigation3 목적지·NavHost·SceneStrategy
│       │   ├── ui/theme/     # 디자인 토큰(Theme, Shapes, Type, Dimension)
│       │   ├── core/         # di / network(Retrofit·OkHttp 설정) / session(인증 상태) / component(공통 오버레이)
│       │   └── view/         # route / screen / component
│       ├── assets/licenses/pretendard/  # 폰트 라이선스
│       └── res/
├── .agents/skills/            # 이 저장소 계열 공통 개발 규칙(SKILL.md 모음)
├── AGENTS.md                  # 저장소 공통 에이전트 계약
├── gradle/libs.versions.toml  # 의존성 버전 카탈로그
└── settings.gradle.kts        # 모듈 구성 (:app 단일 모듈)
```

## 시작하기
### 요구사항
- JDK 및 Android Studio 버전: `확인 필요` (저장소에 명시된 값 없음)
- Android SDK: compileSdk/targetSdk 37, minSdk 34 (`app/build.gradle.kts` 기준)

### 설치 및 실행
```bash
git clone https://github.com/team-nativeLab/heatguard-Android.git
cd heatguard-Android
./gradlew assembleDebug
```

### 환경 설정
현재 `local.properties`, `BuildConfig` 등에 필요한 별도 API 키나 비밀 값은 확인되지 않았습니다.

## 테스트
`app/build.gradle.kts`에 선언된 테스트 의존성 기준이며, 실제 실행 결과는 확인하지 않았습니다.

| 목적 | 명령 |
|---|---|
| 단위 테스트(JUnit4) | `./gradlew testDebugUnitTest` |
| 계측 테스트(Espresso, Compose UI Test) | `./gradlew connectedDebugAndroidTest` |
| Lint | `./gradlew lintDebug` |

## 개발 워크플로
- 코드 작업 전 `AGENTS.md`와 `.agents/skills/**/SKILL.md`를 먼저 확인하고 규칙을 따릅니다.
- GitHub Issue·브랜치·커밋·PR 규칙은 [`AGENTS.md`](AGENTS.md) 6장과 [`.agents/skills/android-github-workflow/SKILL.md`](.agents/skills/android-github-workflow/SKILL.md)를 따릅니다.

## 문서
- [`AGENTS.md`](AGENTS.md) — 저장소 공통 에이전트 계약(계층 구조, 기술 계약, 스킬 라우팅)
- `.agents/skills/` — 영역별 개발 규칙(SKILL.md 모음: DI, 네트워크, UI, Navigation, 디자인 시스템 등)
- PRD 문서: 저장소에 없음 (`확인 필요`)

## 라이선스
라이선스 파일이 저장소에 없어 확인 필요합니다.
