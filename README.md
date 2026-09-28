# HeartGuard (heatguard-Android)

폭염 현장에서 일하는 작업자를 위해 체온 기록, 긴급 호출, 작업/휴식 사진 기록을 지원하는 Android 앱입니다.

## 목차
- [개요](#개요)
- [주요 화면](#주요-화면)
- [반응형 UI 진행 상태](#반응형-ui-진행-상태)
- [서버 API 계약 현황](#서버-api-계약-현황)
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
현재 코드베이스에는 사전 발급된 작업자 계정 로그인 UI와 홈, 긴급 호출, 기록 유형 선택, 온도계 기록, 작업/휴식/현장 사진 촬영, 저장 성공·실패 화면이 Jetpack Compose Navigation 3(`NavDisplay`)로 구현되어 있습니다. 회원가입 기능은 제공하지 않으며 계정은 관리자가 사전에 발급합니다.

PRD 문서는 저장소에 없습니다. 제품 목표·요구사항의 공식 출처는 `확인 필요`입니다.

## 주요 화면
`app/src/main/java/com/nativelap/heartguard/navigation/HeartGuardDestination.kt`에 정의된 목적지 기준입니다.

| 화면 | 설명 |
|---|---|
| Login | 사전 발급된 작업자 계정 로그인. 회원가입 화면과 흐름은 제공하지 않음 |
| Home | 폭염 위험 요약, 체크 타임라인, 기록·긴급 호출·기록 내역 진입 |
| ProfileEdit | 내 정보 수정 화면. 명세에 조회·수정·비밀번호 변경 경로가 있으나 요청·응답 필드가 충분히 정의되지 않아 현재는 미제공 안내를 표시 |
| Inquiry | 문의 등록 화면은 제목·내용을 API 요청으로 제출하는 흐름까지 연결됨. 문의 목록은 응답 항목 형식이 미정이라 미제공 안내를 표시 |
| RecordHistory | 기록 내역 화면. 목록·상세 API 경로가 있으나 목록 항목 형식이 정의되지 않아 현재는 미제공 안내를 표시 |
| Emergency / Calling | 관리자 긴급 호출, 호출 중 상태 |
| RecordTypeSelection | 온도계 기록 / 작업 사진 / 휴식 사진 중 기록 유형 선택 (바텀시트) |
| TemperatureRecord | 온도계 측정값 입력 |
| FieldPhoto / WorkPhoto / RestPhoto | 사진 종류 선택 후 CameraX 전체 화면 촬영 또는 Photo Picker 앨범 선택 (종류별 최대 2장) |
| PhotoCamera | 선택한 기록 유형으로 CameraX 전체 화면 촬영 |
| SaveSuccess / SaveFailure | 저장 성공/실패 결과 |
| WithdrawNotice / WithdrawConfirm / WithdrawDone | 회원탈퇴 안내·확인·완료 화면. 명세상 현재 비밀번호 확인 후 팀 계정과 세션을 비활성화하며, 현장·기존 기록·사진은 유지되고 기록·사진은 생성일부터 365일 뒤 삭제됨 |

로그인은 `POST /api/v1/auth/team/login`에 이메일과 비밀번호를 보냅니다. 명세에는 응답 항목으로 `accessToken`, `user`, `team`, `site`가 적혀 있지만 envelope와 각 객체의 필드 형식은 없습니다. 현재 앱은 `{success, data, error, meta}` envelope의 `data.accessToken`을 기대하므로 실제 서버 응답과의 일치 여부는 `확인 필요`입니다. 계정은 사전 발급 방식이며 회원가입 화면은 제공하지 않습니다. 과거 버전의 `placeholder-access-token`은 앱 시작 시 제거합니다. 세션은 `core/session/SessionManager`와 Android Keystore 기반 토큰 저장소(`AndroidKeystoreTokenStorage`)가 관리하고, `HeartGuardNavHost`가 `SessionState`를 구독해 인증 화면을 전환합니다.

홈 드로어에는 내 정보 수정, 문의하기, 로그아웃, 회원탈퇴가 있습니다. 명세된 로그아웃은 `POST /api/v1/auth/team/logout`이며 204 응답입니다. 내 정보 조회·수정·비밀번호 변경 경로는 정의되어 있으나 프로필 필드와 비밀번호 요청·응답 스키마는 비어 있습니다. 문의 등록은 `POST /api/v1/team/inquiries`와 `{title, content}` 요청이 명시되어 있고, 상세 조회 경로와 응답 필드도 있습니다. 문의 목록 응답 스키마는 `확인 필요`입니다. 회원탈퇴는 `DELETE /api/v1/team/profile`에 현재 비밀번호를 보내며 명세상 204 응답입니다. 기록 목록·상세 경로는 `GET /api/v1/team/records`와 `GET /api/v1/team/records/{recordId}`이고, 목록 항목 형식은 `확인 필요`입니다. 알림 화면과 API는 준비되지 않아 아이콘을 누르면 안내 Snackbar를 표시합니다.

사진 흐름은 사진 종류 선택 → CameraX 전체 화면 촬영 또는 Android Photo Picker(`PickVisualMedia`) 앨범 선택 → 공유 `RecordDraftViewModel` 목록 반영 → 업로드 및 기록 제출 순서입니다. 사진 종류별 최대 2장까지 담으며, 저장 확인 화면 없이 제출합니다. CameraX 화면은 Manifest의 `CAMERA` 권한을 선언하고 런타임 권한도 요청합니다. 촬영 JPEG은 앱 캐시의 `photos/` 임시 파일로 만들고 `FileProvider` URI를 목록·미리보기·업로드에 사용합니다. 기록 흐름이 끝나거나 사진을 삭제하면 촬영 캐시 파일을 정리합니다. 온도·습도·메모 등 일부 입력 초안은 `SavedStateHandle`로 복구하며, 사진 URI나 업로드 키는 프로세스 재생성 후 복구하지 않습니다.

## 반응형 UI 진행 상태
홈, 프로필 수정, 문의, 기록 내역, 긴급 호출·호출 중, 사진 종류 선택, 온도 기록, 저장 성공·실패 화면에는 화면 폭을 최대 600dp로 제한하고 넓은 화면에서 가운데 정렬하는 콘텐츠 래퍼가 적용되어 있습니다. 카메라 촬영 화면은 전체 화면 구성을 유지합니다. Figma의 모든 프레임과 실제 기기 크기별 시각 비교·검증은 완료되지 않았습니다.

## 서버 API 계약 현황
사용자가 제공한 API 명세를 기준으로 계약이 충분히 적힌 항목과 추가 정의가 필요한 항목을 구분합니다. 명세의 `제작 여부` 표시와 앱 연동 완료·실서버 검증 상태는 별개입니다.

| 계약 상태 | API | 명세 요약 |
|---|---|---|
| 요청·응답 정의 있음 | `GET /api/v1/team/checklist`, `PUT /api/v1/team/checklist/items/{itemId}` | KST 기준 체크리스트 조회·완료 저장 |
| 요청·응답 정의 있음 | `POST /api/v1/team/emergency-calls`, `GET /api/v1/team/emergency-calls/current`, `PATCH /api/v1/team/emergency-calls/{callId}` | 긴급 호출 생성·상태 확인·취소/종료 |
| 요청·응답 정의 있음 | `POST /api/v1/team/uploads` | 사진 메타데이터 검증 및 presigned URL 발급 |
| 요청·응답 정의 있음 | `POST /api/v1/team/inquiries`, `GET /api/v1/team/inquiries/{inquiryId}` | 문의 등록 및 상세 조회. 앱에서 문의 등록 흐름 연결됨 |
| 요청·응답 정의 있음 | `GET /api/v1/team/records/{recordId}`, `DELETE /api/v1/team/profile`, `POST /api/v1/auth/team/logout` | 기록 상세 조회, 현재 비밀번호를 확인하는 탈퇴 요청, 로그아웃 |
| 계약 보완 필요 | `POST /api/v1/auth/team/login` | 요청과 최상위 응답 항목명은 있으나 envelope와 객체 필드 정의가 없음 |
| 계약 보완 필요 | `GET/PATCH /api/v1/auth/team/me`, `PUT /api/v1/auth/team/password` | 프로필 조회·수정 필드와 비밀번호 요청·응답 형식이 없음 |
| 응답 스키마 일부 보완 필요 | `GET /api/v1/team` | KST 날짜, 고정 weather 객체, `site.managerPhone` 반환은 명시됨. 기상 미입력 시 측정값은 null |
| 요청 계약 정의 있음 | `POST /api/v1/team/records` | `{type, photoKeys[1..2], measuredAt, temperature?, humidity?, memo?}` 및 저장 응답 필드 정의됨. `measuredAt`은 사용자가 확정한 저장 버튼 시각 |
| 응답 계약 보완 필요 | `GET /api/v1/team/records` | cursor/limit 기반 목록 경로는 있으나 항목 형식이 비어 있음 |
| 계약 보완 필요 | `GET /api/v1/team/inquiries` | 문의 목록 응답 필드가 없음 |

홈 조회와 기록 저장은 명세 경로(`/api/v1/team`, `/api/v1/team/records`) 및 작업자 Bearer 세션을 사용합니다. 홈의 기상 측정값은 null을 허용하고, 기록 등록은 명세에 없는 `noThermometer` 필드를 보내지 않으며 온도계 수기 입력 시에도 사진을 첨부합니다. `measuredAt`은 저장 버튼 시각으로 저장합니다. 여러 사진 업로드 응답 항목과 요청 사진의 매칭 규칙은 명세에 없어 서버 확인이 필요합니다. 실제 서버 통신은 아직 검증되지 않았습니다.

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

Hilt와 Retrofit/OkHttp를 사용합니다. `core/di`, `core/network`, `core/session`에는 DI 모듈, Retrofit 서비스 생성 팩토리(`ApiRetrofitFactory`), 공통 API 결과 타입(`ApiResult`/`ApiError`), 세션 관리가 있습니다. `domain`과 `data`에는 사전 발급 계정 로그인, 사진 업로드 URL 발급·업로드, Bearer 세션 기반 홈·기록·긴급 호출, 문의 등록과 회원탈퇴 기능의 UseCase·Repository·RemoteDataSource 흐름이 있습니다. 홈 기상 수치는 nullable로 처리합니다. 기록 저장은 명세 필드를 반영했으며 `measuredAt` 산정과 사진 응답 매칭 규칙은 확인이 필요합니다. 화면 상태는 `viewmodel`에 있으며 로컬 데이터베이스(Room 등)는 없습니다.

## 아키텍처
`AGENTS.md`가 안내하는 Presentation/Domain/Data/Core 구분을 단일 `app` 모듈 안에서 패키지로 나누어 사용합니다. Compose Route/Screen이 ViewModel의 상태와 이벤트를 연결하고, ViewModel은 UseCase를 호출합니다. Repository 구현은 RemoteDataSource를 통해 Retrofit API와 사진 파일 업로드를 처리합니다.

```
view/
├── route/       # 화면별 진입점(Route) — 콜백을 받아 Screen을 조립
├── screen/      # 화면 단위 Composable(Scaffold 포함)
└── component/   # 화면별 재사용 Composable (account, auth, emergency, feedback, history, home, inquiry, menu, photo, profile, temperature 등)
viewmodel/       # account / auth / emergency / history / home / inquiry / menu / profile / record 상태와 이벤트
domain/          # account / auth / emergency / record / site 모델, Repository 계약, UseCase
data/            # account / auth / emergency / record / site DTO·Mapper·RemoteDataSource·Repository 구현
navigation/      # HeartGuardDestination(Navigation3 목적지), HeartGuardNavHost, 커스텀 SceneStrategy
core/component/  # 다이얼로그 배경 블러 등 공통 UI 유틸
core/network/    # Retrofit·OkHttp, API 결과 및 인증 처리
core/session/    # 세션 상태와 Keystore 기반 토큰 저장
ui/theme/        # Theme, Shapes, Type, Dimension
```

사진 Route에서 카메라 또는 앨범 출처를 선택합니다. CameraX 촬영 결과와 Photo Picker URI는 기록 종류별 목록을 보유한 공유 `RecordDraftViewModel`에 추가됩니다. 저장 버튼은 사진 1~2장을 업로드한 뒤 `/api/v1/team/records`에 기록을 제출하며 `measuredAt`에는 저장 버튼 시각을 사용합니다. 사진 업로드 응답 매칭 방식은 서버 확인이 필요하고 실제 백엔드 동작은 검증되지 않았습니다.

## 프로젝트 구조
```
HeartGuard/
├── app/
│   └── src/main/
│       ├── java/com/nativelap/heartguard/
│       │   ├── MainActivity.kt
│       │   ├── navigation/   # Navigation3 목적지·NavHost·SceneStrategy
│       │   ├── ui/theme/     # 디자인 토큰(Theme, Shapes, Type, Dimension)
│       │   ├── core/         # di / network(Retrofit·OkHttp 설정) / session(인증 상태) / util / component(공통 오버레이)
│       │   ├── data/         # account / auth / emergency / record / site 원격 데이터와 Repository 구현
│       │   ├── domain/       # account / auth / emergency / record / site 모델, Repository 계약, UseCase
│       │   ├── viewmodel/    # account / auth / emergency / history / home / inquiry / menu / profile / record 상태와 이벤트
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
서버 기본 주소는 Gradle 속성으로 설정합니다. Debug는 `HEARTGUARD_DEBUG_BASE_URL`을 지정할 수 있고, 지정하지 않으면 제공받은 테스트 서버 주소(`https://heatguard-temp.https.gsmsv.site/`)를 사용합니다. 다른 API 서버에 연결하려면 유효한 HTTPS 주소와 마지막 `/`를 포함해 설정하세요.

```properties
# ~/.gradle/gradle.properties 또는 -P Gradle 속성으로 설정
HEARTGUARD_DEBUG_BASE_URL=https://YOUR_DEBUG_API_HOST/
HEARTGUARD_RELEASE_BASE_URL=https://YOUR_RELEASE_API_HOST/
```

Release 빌드는 유효한 실제 HTTPS 서버 주소가 설정되지 않으면 검증 단계에서 실패합니다. 실제 서버와 계정으로 앱 전체 연동은 검증하지 않았습니다. 로그인 중첩 응답 형식과 기록의 `measuredAt` 산정 규칙은 명세에서 더 확인해야 합니다. 회원가입은 제공하지 않습니다.

## 테스트
`app/build.gradle.kts`에 선언된 테스트 의존성 기준입니다. 검증 시에는 아래 프로젝트 태스크를 사용합니다.

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
