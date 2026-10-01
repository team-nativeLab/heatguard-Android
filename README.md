# HeartGuard (heatguard-Android)

폭염 현장에서 일하는 작업자를 위해 체온 기록, 긴급 호출, 작업/휴식 사진 기록을 지원하는 Android 앱입니다.

## 목차
- [개요](#개요)
- [주요 화면](#주요-화면)
- [반응형 UI 진행 상태](#반응형-ui-진행-상태)
- [서버 API 계약 현황](#서버-api-계약-현황)
- [인증·계정 전환 정책](#인증계정-전환-정책)
- [백엔드 확인 요청](#백엔드-확인-요청)
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
현재 구현된 작업자 앱 화면(로그인, 홈, 긴급 호출, 기록 유형 선택·온도계·작업/휴식/현장 사진 기록, 저장 결과, 기록 내역·상세, 내 정보 수정, 비밀번호 변경, 문의하기, 알림 목록, 회원탈퇴)은 Jetpack Compose Navigation 3(`NavDisplay`)를 사용하며 작업자 API(`/api/v1/auth/team/*`, `/api/v1/team/*`)에 연결되어 있습니다. 서버가 제공하지 않는 값은 고정값 대신 `--`로 표시합니다. 회원가입 기능은 제공하지 않으며 계정은 관리자가 사전에 발급합니다.

PRD 문서는 저장소에 없습니다. 현재 확인 가능한 제품 범위 자료는 사용자 제공 Figma와 API 명세 v0.1이며, 별도 PRD 문서는 `확인 필요`입니다.

## 주요 화면
`app/src/main/java/com/nativelap/heartguard/navigation/HeartGuardDestination.kt`에 정의된 목적지 기준입니다.

| 화면 | 설명 |
|---|---|
| Login | 사전 발급된 작업자 계정 로그인. 회원가입 화면과 흐름은 제공하지 않음 |
| Home | 현재 온도·습도·체감온도·폭염 단계·날씨 상태(`weather.skyStatus`)·온도 변화량(`weather.temperatureDelta`), 체크 타임라인(서버 `checkTimes` + 오늘 기록으로 완료 표시). "관리자 전화"는 현장관리자 긴급호출 화면으로, "긴급 전화"는 본사 번호(`company.phone`) 다이얼(번호 없으면 비활성). 화면 재개 시 새로고침, 조회 실패 시 재시도 안내 |
| ProfileEdit / PasswordChange | 내 정보 조회·이름 수정(`/auth/team/me`, `version`으로 동시 수정 충돌 감지), 회사명(`companyName`) 표시, 비밀번호 변경(`/auth/team/password`) |
| Inquiry | 문의 등록과 내 문의 목록(상태 배지·등록일). 문의 상세 화면은 범위 밖 |
| Notifications | 홈·기록 화면의 벨에서 진입. 전체/기록/긴급/공지 분류 필터와 20개 단위 cursor 페이지를 제공합니다. 행 탭은 개별 읽음 처리만 하며, ‘모두 읽음’ 버튼은 서버의 일괄 API를 한 번 호출한 뒤 목록을 다시 조회합니다. 서버에 알림 본문 필드가 없어 제목·시각·읽음 상태를 표시합니다. 초기 로딩·오류/재시도·전체/분류별 빈 상태·추가 페이지 로딩/재시도 상태를 제공합니다 ([Figma 목록 프레임](https://www.figma.com/design/d9MzGARXpSX1Y53E64hdog/%EC%9D%B4%EC%9D%8C%EC%82%B0%EC%97%85%EA%B1%B4%EC%84%A4?node-id=914-1016), [빈 상태 프레임](https://www.figma.com/design/d9MzGARXpSX1Y53E64hdog/%EC%9D%B4%EC%9D%8C%EC%82%B0%EC%97%85%EA%B1%B4%EC%84%A4?node-id=914-1236)) |
| RecordHistory / RecordHistoryDetail | 기간(기본 최근 7일, 최대 31일)·유형 필터·유형별 건수·날짜별 기록 목록과 온도계/사진 기록 상세. 위치·팀명은 기록 당시 값, 휴식 기록은 `restMinutes` 표시 |
| Emergency / Calling | 홈 "관리자 전화"로 진입. "긴급 호출하기"를 눌러 호출 등록 → 호출 중(관리자 확인 시 연결됨) → 취소·종료. 앱 재시작 시 진행 중 호출을 이어받음 |
| RecordTypeSelection | 온도계 기록 / 작업 사진 / 휴식 사진 중 기록 유형 선택 (바텀시트) |
| TemperatureRecord | 온도계 측정값 입력 |
| FieldPhoto / WorkPhoto / RestPhoto | 사진 종류 선택 후 CameraX 전체 화면 촬영 또는 Photo Picker 앨범 선택 (종류별 최대 2장). 휴식 기록은 시작·종료 시각을 TimePicker로 선택하고 자정을 넘기는 구간을 지원 |
| PhotoCamera | 선택한 기록 유형으로 CameraX 전체 화면 촬영 |
| SaveSuccess / SaveFailure | 저장 성공/실패 결과. 실패 시 명세 오류 코드별 안내 |
| WithdrawNotice / WithdrawConfirm / WithdrawDone | 회원탈퇴 안내·확인·완료. 현재 비밀번호 확인 후 팀 계정과 세션을 비활성화 |

세션은 `core/session/SessionManager`와 Android Keystore 기반 토큰 저장소(`AndroidKeystoreTokenStorage`)가 관리하고, `HeartGuardNavHost`가 `SessionState`를 구독해 인증 화면을 전환합니다. 화면 ViewModel이 Activity 수명으로 남기 때문에, 사용자 데이터를 가진 ViewModel은 `clearStateWhenSessionEnds`로 로그아웃·세션 만료 시 이전 작업자의 값을 지웁니다. 비밀번호 확인 요청(회원탈퇴·비밀번호 변경)은 `PasswordConfirmationRequest` 태그를 붙여, 비밀번호 불일치(401)가 세션 만료로 처리되지 않게 합니다. 알림 ViewModel도 세션 종료 시 목록과 읽지 않은 수를 초기화합니다. 읽지 않은 항목을 누르면 해당 항목만 읽음 처리하고, 실패하면 읽지 않은 상태를 유지하며 Snackbar로 안내합니다.

사진 흐름은 사진 종류 선택 → CameraX 전체 화면 촬영 또는 Android Photo Picker(`PickVisualMedia`) 앨범 선택 → 공유 `RecordDraftViewModel` 목록 반영 → 업로드 및 기록 제출 순서입니다. 사진 종류별 최대 2장까지 담으며, 저장 확인 화면 없이 제출하며, 저장은 `RecordDraftViewModel`의 `viewModelScope`에서 한 번만 실행됩니다(연타 방지, 취소 시 상태 복구). CameraX 화면은 Manifest의 `CAMERA` 권한을 선언하고 런타임 권한도 요청합니다. 촬영 JPEG은 앱 캐시의 `photos/` 임시 파일로 만들고 `FileProvider` URI를 목록·미리보기·업로드에 사용합니다. 기록 흐름이 끝나거나 사진을 삭제하면 촬영 캐시 파일을 정리합니다. 온도·습도·메모 등 일부 입력 초안은 `SavedStateHandle`로 복구하며, 사진 URI나 업로드 키는 프로세스 재생성 후 복구하지 않습니다.

## 반응형 UI 진행 상태
홈, 프로필 수정, 문의, 알림, 기록 내역, 긴급 호출·호출 중, 사진 종류 선택, 온도 기록, 저장 성공·실패, 탈퇴 안내·완료 화면에는 최대 600dp 중앙 콘텐츠 래퍼와 하단 동작 폭 제한을 적용했습니다. 로그인은 폼의 실제 높이에 따라 상단 여백을 줄이고 상태바·내비게이션 바·키보드 여백을 처리합니다. 로그인 입력 영역, 알림 빈 상태, 탈퇴 완료 본문은 작은 화면에서 스크롤됩니다. 카메라 촬영 화면은 전체 화면 구성을 유지합니다. 2026-10-01 에뮬레이터에서 로그인 393×582dp 잘림 수정과 실제 IME 표시를 캡처로 확인했으며, 확대 글꼴·탈퇴 화면의 넓은 폭·스크롤은 계측 테스트로 검증했습니다. 저장 결과 제목·메시지·요약·버튼은 확대 글꼴에 맞춰 높이가 늘어나고, 메뉴는 짧은 화면에서 스크롤됩니다. 시트 본문도 최대 600dp로 제한하며 온도계 수동 입력 스위치에 접근성 이름을 연결했습니다. 저장 결과의 긴 요약·가로 오류 화면, 메뉴 스크롤, 태블릿 중앙 정렬과 스위치 semantics는 계측 테스트로 검증했습니다. 모든 작업자 프레임의 인증된 화면과 기기 크기별 시각 비교는 아직 완료되지 않았습니다.

## 서버 API 계약 현황
2026-09-28 테스트 서버(`https://heatguard-temp.https.gsmsv.site/`)와 테스트 계정으로 확인한 결과에, API 명세 v0.1(2026-09-29)에서 추가된 필드를 반영했습니다. 신규 필드(`company`, `weather.skyStatus`·`temperatureDelta`, `companyName`·`version`, 기록의 `restMinutes`·`teamName`·`workplace`·`siteName`)는 2026-09-29 실서버 응답에 있는 것을 확인했습니다. 기록 응답에는 명세에 없는 `restStartedAt`·`restEndedAt`도 있어 휴식 시간 표시에 사용합니다.

알림과 휴식 기록 계약은 사용자 제공 Downloads의 `개인 페이지 & 공유된 페이지 11` 명세를 기준으로 합니다. 알림 분류 매핑은 `RECORD`→`RECORD_CREATED`, `EMERGENCY`→`EMERGENCY_ACKNOWLEDGED`, `NOTICE`→`INQUIRY_ANSWERED`입니다.

| API | 앱 연동 | 비고 |
|---|---|---|
| `POST /auth/team/login`, `POST /auth/team/logout` | 연결 | 응답 `data{accessToken, expiresAt, user, siteId, teamId}`, refresh token 없음 |
| `GET·PATCH /auth/team/me` | 연결 | 응답 `{userId, name, email, phone, companyName, role, …, version}`. PATCH는 `name·email·phone` 중 하나 이상 필요, 앱은 `name`과 마지막으로 받은 `version`을 보냄. 409는 최신 정보 재조회 후 안내 |
| `PUT /auth/team/password` | 연결 | 요청 `{currentPassword, newPassword}`, 성공 `{changedAt}`, 현재 비밀번호 오류 401 `INVALID_CREDENTIALS`, 8자 미만 400. 변경 후에도 현재 세션 유지 |
| `GET /team` | 연결 | 기상 미입력 시 `weather`·`heatLevel`이 null, 관리자 연락처는 빈 문자열일 수 있음. `company.phone`(본사 연락처), `weather.skyStatus`, `weather.temperatureDelta` 사용 |
| `POST /team/uploads` → presigned `PUT` → `POST /team/records` | 연결 | `files[i] ↔ uploads[i]` 순서 보장, `slot`은 기록 내 사진 위치, `requiredHeaders`를 그대로 붙여 PUT. REST 기록은 필수로 선택한 `restStartedAt`·`restEndedAt`을 오프셋 포함 시각으로 보내며, 종료 시간이 같거나 이르면 다음 날로 처리. `restMinutes`는 서버가 계산 |
| `GET /team/records`, `GET /team/records/{recordId}` | 연결 | 목록은 `date` 하루 단위 필터만 지원(기간·유형 쿼리는 무시됨). 목록 항목에는 `photoKeys`만, 상세에는 `photoUrls` 추가. `restMinutes`·`teamName`·`workplace`·`siteName`은 기록 당시 값(이전 기록은 null) |
| `POST·GET /team/inquiries` | 연결 | 목록 항목 `{inquiryId, title, content, status, deliveryStatus, replies[], createdAt}`, 알 수 없는 cursor는 400 |
| `POST /team/emergency-calls`, `GET …/current`, `PATCH …/{callId}` | 연결 | `current`는 종료된 이전 호출(CANCELLED 등)도 반환하므로 앱은 자신이 시작·이어받은 `callId`만 반영. 취소·종료의 409 `INVALID_STATUS_TRANSITION`은 현재 상태를 다시 조회해 화면을 맞춤 |
| `GET /api/v1/team/notifications` | 연결 | `category`는 선택값(`ALL`·`RECORD`·`EMERGENCY`·`NOTICE`, 생략/빈 값은 `ALL`), `limit`은 1..100(기본 20), `cursor`는 선택값이며 앱은 선택한 분류를 전송합니다. 응답 `items`는 `{notificationId, type, category, title, resourceId, read, createdAt, updatedAt}`, `page`는 `{nextCursor, hasMore}`, 카운트는 전체 `unreadCount`와 분류별 `filteredUnreadCount`. 알림 유형은 `RECORD_CREATED`, `EMERGENCY_ACKNOWLEDGED`, `INQUIRY_ANSWERED`. |
| `PATCH /api/v1/team/notifications/{notificationId}/read` | 연결 | 본인에게 공개된 알림 한 건을 사용자별로 읽음 처리합니다. body 없이 요청하며 응답은 `{notificationId, read, readAt}`입니다. |
| `PATCH /api/v1/team/notifications/read-all` | 연결 | Query·Body 없이 본인의 모든 분류·페이지 알림을 읽음 처리합니다. 응답 `{updatedCount, unreadCount}`를 반영하고 목록을 유지한 채 현재 분류 첫 페이지를 재조회합니다. 재조회 실패는 기존 데이터와 읽음 결과를 보존하며 GET만 재시도합니다. 개별 읽음과 겹친 목록 조회는 처리 완료까지 대기하고, 앱 복귀 조회는 최소 5초 간격으로 실행합니다. 처리 중 재진입·연타를 차단하고 세션 종료 시 요청을 취소·무효화합니다. |
| `DELETE /team/profile` | 연결 | 선택한 탈퇴 사유를 `reason`(고정 코드 `FIELD_WORK_ENDED` 등)으로 함께 보냄. 팀 전체 비활성화라 테스트 계정으로는 실행하지 않고 단위 테스트로만 확인 |
| `GET /team/checklist`, `PUT /team/checklist/items/{itemId}` | 미연결 | 작업자 앱에 체크리스트 화면을 만들지 않기로 한 사용자 결정에 따라 범위 제외 |
| `GET /team/inquiries/{inquiryId}` | 미연결 | 작업자 앱 문의 상세 화면과 목록 화살표를 만들지 않기로 한 사용자 결정에 따라 범위 제외 |

알림 명세에서 기록·긴급 알림은 팀에 공유하고 문의 답변 알림은 작성자에게만 공개합니다. 읽음 상태는 사용자별입니다. 공지 게시 API, 과거 기록 알림 소급 생성, FCM/APNs Push 발송은 명세 범위에 없으며 앱에도 푸시 등록·수신 기능이 없습니다.

2026-10-01 추가 계약 점검에서 긴급호출의 409 후 상태 재조회가 실패하면 기존 호출과 폴링을 유지하고 실패를 표시하도록 수정했습니다. 폴링의 정상 `NONE`·호출 ID 없음 응답은 현재 호출을 종료합니다. 기록 내역·문의·알림 목록은 첫 페이지만 받고 스크롤이 끝에 가까워지면 `nextCursor`로 다음 페이지를 이어 받습니다(기록 내역은 최신 날짜부터 하루씩, 기록이 없는 날은 건너뜀). `page` 누락, `hasMore=true`인데 커서 없음, 이미 받은 커서 반복은 잘린 목록을 성공으로 보여주지 않고 실패로 알립니다. 아직 받을 목록이 남아 있으면 건수 뒤에 `+`를 붙입니다(예: `32+건`). 홈 체크 타임라인용 오늘 기록 전체 조회만 하루 100페이지 안전 상한을 둡니다.

## 인증·계정 전환 정책

- **요청 소유 세션**: 인증 요청은 만들어진 시점의 세션 스냅샷(세대·토큰)을 태그로 가지며, 전송 직전에 세션이 바뀌었으면 보내지 않습니다(`SessionChangedException`, 화면에는 오류로 노출하지 않음). 늦게 도착한 이전 세션의 401은 새 로그인을 끝내지 않습니다.
- **토큰 삭제 실패**: 로그아웃·만료 때 메모리 세션을 먼저 끝내고, 저장소 삭제가 실패하면 Keystore 키를 폐기해 남은 암호문을 읽을 수 없게 합니다. 저장값을 읽거나 복호화하지 못하면 로그아웃 상태로 시작합니다.
- **계정 전환 정리**: 로그인·로그아웃마다 세션 세대가 바뀌고, 로그아웃→재로그인이 빠르게 지나가도 홈·긴급호출·탈퇴·기록 초안·목록 ViewModel과 화면 이동 기록을 정리합니다. 업로드→저장처럼 여러 요청으로 이어지는 흐름은 단계 사이에 세대를 비교해 다른 계정으로 저장되지 않게 합니다.
- **초안 복원**: 프로세스 재생성 뒤 남은 입력 초안은 저장 토큰으로 복원된 같은 세션일 때만 되살리고, 새로 로그인한 세션이면 버립니다.

## 백엔드 확인 요청
앱에서 해결할 수 없어 서버 쪽 확인·추가가 필요한 항목입니다. 해당 UI는 고정값 없이 `--`로 표시하거나 숨깁니다.

- **체감온도 계산**: 테스트 기록(31.5°C, 습도 60%)의 체감온도가 81.9°C로 계산되어 내려왔습니다.
- **체크 완료 판정**: 홈 체크 타임라인 완료 여부 필드가 없어, 앱이 "체크 시각 ~ 다음 체크 시각 전" 사이 기록 유무로 판정합니다. 서버 기준 확인 또는 필드 추가가 필요합니다.
- **없는 필드**: 온도계 설치 여부(현재 측정 온도 유무로 표시), 기록 목록 썸네일 URL, 기록 목록 기간·유형 쿼리
- **기록 저장 검증 환경**: 2026-10-01 에뮬레이터 실서버 검증에서 사진 업로드 URL 발급·S3 업로드까지는 통과했지만, 테스트 현장에 수동 기상값이 없어 기록 저장이 `WEATHER_BASELINE_REQUIRED`로 거절됐습니다. 현장관리자 계정으로 기상값을 입력한 뒤 저장 성공(자정 경과 휴식 포함)을 다시 확인해야 합니다.
- **계약 확인**: `sha256` 인코딩(Base64/hex), `slot` 의미, `POST /team/records`의 Idempotency-Key 지원, 허용 이미지 형식·최대 크기

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
| 이미지 | Coil 3.3.0 (`coil-compose`, `coil-network-okhttp`) — 기록 상세 사진 표시. 3.4 이상은 Kotlin 2.3+ stdlib이 필요해 현재 Kotlin 2.2와 호환되는 버전 사용 |
| 직렬화 | kotlinx.serialization.json 1.8.1 |
| 빌드 | Gradle (Kotlin DSL), Version Catalog, AGP 9.2.1 |
| minSdk / targetSdk / compileSdk | 34 / 37 / 37 |

Hilt와 Retrofit/OkHttp를 사용합니다. `core/di`, `core/network`, `core/session`에는 DI 모듈, Retrofit 서비스 생성 팩토리(`ApiRetrofitFactory`), 공통 API 결과 타입(`ApiResult`/`ApiError`), 세션 관리가 있습니다. `domain`과 `data`에는 로그인·로그아웃, 내 정보·비밀번호, 홈, 사진 업로드·기록 저장, 기록 내역, 긴급 호출, 문의, 회원탈퇴 기능의 UseCase·Repository·RemoteDataSource 흐름이 있습니다. 화면 상태는 `viewmodel`에 있으며 로컬 데이터베이스(Room 등)는 없습니다.

## 아키텍처
`AGENTS.md`가 안내하는 Presentation/Domain/Data/Core 구분을 단일 `app` 모듈 안에서 패키지로 나누어 사용합니다. Compose Route/Screen이 ViewModel의 상태와 이벤트를 연결하고, ViewModel은 UseCase를 호출합니다. Repository 구현은 RemoteDataSource를 통해 Retrofit API와 사진 파일 업로드를 처리합니다.

```
view/
├── route/       # 화면별 진입점(Route) — 콜백을 받아 Screen을 조립
├── screen/      # 화면 단위 Composable(Scaffold 포함)
└── component/   # 화면별 재사용 Composable (account, auth, emergency, feedback, history, home, inquiry, menu, photo, profile, temperature 등)
viewmodel/       # account / auth / emergency / history / home / inquiry / menu / password / profile / record 상태와 이벤트
domain/          # account / auth / emergency / inquiry / profile / record / site 모델, Repository 계약, UseCase
data/            # account / auth / emergency / inquiry / profile / record / site DTO·Mapper·RemoteDataSource·Repository 구현
navigation/      # HeartGuardDestination(Navigation3 목적지), HeartGuardNavHost, 커스텀 SceneStrategy
core/component/  # 다이얼로그 배경 블러 등 공통 UI 유틸
core/network/    # Retrofit·OkHttp, API 결과 및 인증 처리
core/session/    # 세션 상태와 Keystore 기반 토큰 저장
ui/theme/        # Theme, Shapes, Type, Dimension
```

사진 Route에서 카메라 또는 앨범 출처를 선택합니다. CameraX 촬영 결과와 Photo Picker URI는 기록 종류별 목록을 보유한 공유 `RecordDraftViewModel`에 추가됩니다. 저장 버튼은 사진 1~2장을 업로드(URL 발급 → PUT)한 뒤 받은 `objectKey`로 `/api/v1/team/records`에 기록을 제출하며 `measuredAt`에는 저장 버튼 시각을 사용합니다.

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
│       │   ├── data/         # account / auth / emergency / inquiry / profile / record / site 원격 데이터와 Repository 구현
│       │   ├── domain/       # account / auth / emergency / inquiry / profile / record / site 모델, Repository 계약, UseCase
│       │   ├── viewmodel/    # account / auth / emergency / history / home / inquiry / menu / password / profile / record 상태와 이벤트
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

Release 빌드는 유효한 실제 HTTPS 서버 주소가 설정되지 않으면 검증 단계에서 실패합니다. 2026-10-01 테스트 서버와 테스트 계정으로 로그인·자동 로그인 복원, 홈 조회, 사진 촬영·업로드, 기록 내역 이어 받기, 알림 모두 읽음, 문의 목록, 긴급호출 생성·취소, 로그아웃 후 재로그인 시 초안 정리를 에뮬레이터에서 확인했습니다. 기록 저장 성공은 위 기상값 문제로 확인하지 못했습니다. 회원가입은 제공하지 않습니다.

## 테스트
`app/build.gradle.kts`에 선언된 테스트 의존성 기준입니다. 검증 시에는 아래 프로젝트 태스크를 사용합니다.

| 목적 | 명령 |
|---|---|
| 단위 테스트(JUnit4) | `./gradlew testDebugUnitTest` |
| 계측 테스트(Espresso, Compose UI Test) | `./gradlew connectedDebugAndroidTest` |
| Lint | `./gradlew lintDebug` |

알림 테스트는 API 경로·query/body 계약, 유형·분류 매핑, cursor 페이지, 개별·전체 읽음 성공/실패, 지연 응답·재진입·세션 전환, PATCH와 페이지 조회 교차, 최신 분류 대기 조회, 포그라운드 5초 갱신 간격과 읽음 완료 후 예약 갱신, 느린 오류 안내 중 분류 조회, PATCH 성공 후 GET 실패와 재시도를 다룹니다. REST 테스트는 자정 경과·1440분 경계·필수 입력·한국 날짜·SavedState 복원과 초기화를 검증합니다. 긴급호출 테스트는 409 후 Network·429·500 재조회 실패와 NONE 종료를, 문의·기록 테스트는 마지막 허용 페이지·커서 순환·누락·취소를 검증합니다. 화면 계측 테스트는 빈 목록의 새로고침 오류·읽음 완료 후 자동 페이지 조회 재개, 로그인 짧은 화면·확대 글꼴·실제 IME·오류 안내 조합, 탈퇴 중앙 폭 제한·스크롤·IME 실패 안내를 검증합니다. 2026-10-01 `testDebugUnitTest` 167개와 `connectedDebugAndroidTest` 20개가 통과했으며, `lintDebug` 오류 0개(기존 경고 46개), `assembleDebug` 성공을 확인했습니다. 실제 서버 사진 업로드·REST 저장은 이번 변경에서 아직 재검증하지 않았습니다.

## 개발 워크플로
- 코드 작업 전 `AGENTS.md`와 `.agents/skills/**/SKILL.md`를 먼저 확인하고 규칙을 따릅니다.
- GitHub Issue·브랜치·커밋·PR 규칙은 [`AGENTS.md`](AGENTS.md) 6장과 [`.agents/skills/android-github-workflow/SKILL.md`](.agents/skills/android-github-workflow/SKILL.md)를 따릅니다.

## 문서
- [`AGENTS.md`](AGENTS.md) — 저장소 공통 에이전트 계약(계층 구조, 기술 계약, 스킬 라우팅)
- `.agents/skills/` — 영역별 개발 규칙(SKILL.md 모음: DI, 네트워크, UI, Navigation, 디자인 시스템 등)
- PRD 문서: 저장소에 없음 (`확인 필요`)

## 라이선스
라이선스 파일이 저장소에 없어 확인 필요합니다.
