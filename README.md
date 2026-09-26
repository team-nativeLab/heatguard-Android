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
| FieldPhoto / WorkPhoto / RestPhoto | 사진 종류 선택 후 CameraX 전체 화면 촬영 또는 Photo Picker 앨범 선택 (종류별 최대 2장) |
| SaveConfirmation | 저장 전 확인 다이얼로그 |
| SaveSuccess / SaveFailure | 저장 성공/실패 결과 |

로그인·회원가입은 실제 인증 API가 아직 연결되지 않은 임시 흐름이며, 로그인 성공 시 placeholder 토큰을 저장합니다(`HeartGuardSessionViewModel.onLoginSucceeded()` 참고). 세션 상태는 `core/session/SessionManager`와 Keystore 기반 토큰 저장소(`TokenStorage`)가 관리하고, `HeartGuardNavHost`가 `SessionState`를 구독해 인증/비인증 화면을 전환합니다. 서버 통신 계층은 구성되어 있지만 `app/build.gradle.kts`의 기본 API 주소는 placeholder이므로 실제 백엔드 환경 설정은 확인이 필요합니다.

사진 흐름은 사진 종류 선택 → CameraX 전체 화면 촬영 또는 Android Photo Picker(`PickVisualMedia`) 앨범 선택 → 공유 `RecordDraftViewModel` 목록 반영 → 저장 확인 및 업로드 순서입니다. 현장·작업·휴식 사진은 종류별 최대 2장까지 담습니다. CameraX 화면은 Manifest의 `CAMERA` 권한을 선언하고 런타임 권한도 요청합니다. 촬영 JPEG은 앱 캐시의 `photos/` 임시 파일로 만들고 `FileProvider` URI를 목록·미리보기·업로드에 사용합니다. 기록 흐름이 끝나거나 사진을 삭제하면 촬영 캐시 파일을 정리합니다.

기록 저장은 선택한 사진을 presigned URL로 업로드한 뒤 해당 키와 기록 정보를 서버에 제출합니다. 성공·실패 화면은 제출 결과를 표시합니다. 서버 주소와 테스트 팀 토큰 설정이 placeholder인 상태라 실제 서버 환경에서의 동작은 확인이 필요합니다.

## 기술 스택
`gradle/libs.versions.toml`, `app/build.gradle.kts` 기준으로 확인한 값입니다.

| 영역 | 사용 기술 |
|---|---|
| 언어 | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01), Material 3 |
| Navigation | AndroidX Navigation3 (`navigation3-runtime`, `navigation3-ui` 1.1.6), `NavDisplay` |
| 사진 | CameraX 1.6.2, Android Photo Picker |
| DI | Hilt |
| 네트워크 | Retrofit, OkHttp, kotlinx.serialization 컨버터 |
| 직렬화 | kotlinx.serialization.json 1.8.1 |
| 빌드 | Gradle (Kotlin DSL), Version Catalog, AGP 9.2.1 |
| minSdk / targetSdk / compileSdk | 34 / 37 / 37 |

Hilt와 Retrofit/OkHttp를 사용합니다. `core/di`, `core/network`, `core/session`에는 DI 모듈, Retrofit 서비스 생성 팩토리(`ApiRetrofitFactory`), 공통 API 결과 타입(`ApiResult`/`ApiError`), 세션 관리가 있습니다. `domain`과 `data`에는 현장 기록 사진 업로드·기록 제출, 긴급 호출, 현장 개요 기능의 UseCase·Repository·RemoteDataSource 흐름이 구현되어 있고, 화면 상태는 `viewmodel`에 있습니다. 로그인은 아직 임시 흐름이며 로컬 데이터베이스(Room 등)는 없습니다.

## 아키텍처
`AGENTS.md`가 안내하는 Presentation/Domain/Data/Core 구분을 단일 `app` 모듈 안에서 패키지로 나누어 사용합니다. Compose Route/Screen이 ViewModel의 상태와 이벤트를 연결하고, ViewModel은 UseCase를 호출합니다. Repository 구현은 RemoteDataSource를 통해 Retrofit API와 사진 파일 업로드를 처리합니다.

```
view/
├── route/       # 화면별 진입점(Route) — 콜백을 받아 Screen을 조립
├── screen/      # 화면 단위 Composable(Scaffold 포함)
└── component/   # 화면별 재사용 Composable (auth, home, emergency, photo, temperature, feedback 등)
viewmodel/       # 화면 상태 및 사용자 이벤트 처리 (home, emergency, record)
domain/          # 현장 기록·긴급 호출·현장 개요 모델, Repository 계약, UseCase
data/            # DTO·Mapper·RemoteDataSource·Repository 구현
navigation/      # HeartGuardDestination(Navigation3 목적지), HeartGuardNavHost, 커스텀 SceneStrategy
core/component/  # 다이얼로그 배경 블러 등 공통 UI 유틸
core/network/    # Retrofit·OkHttp, API 결과 및 인증 처리
core/session/    # 세션 상태와 Keystore 기반 토큰 저장
ui/theme/        # Theme, Shapes, Type, Dimension
```

사진 Route에서 카메라 또는 앨범 출처를 선택합니다. CameraX 촬영 결과와 Photo Picker URI는 기록 종류별 목록을 보유한 공유 `RecordDraftViewModel`에 추가되며, 저장 확인 화면에서 사진을 업로드한 뒤 기록 정보를 제출합니다. 실제 백엔드 연결 환경은 확인이 필요합니다.

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
│       │   ├── data/         # record / emergency / site 원격 데이터와 Repository 구현
│       │   ├── domain/       # 모델, Repository 계약, UseCase
│       │   ├── viewmodel/    # home / emergency / record 상태와 이벤트
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
