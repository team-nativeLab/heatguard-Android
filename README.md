# HeartGuard (heatguard-Android)

폭염 현장에서 일하는 작업자를 위해 체온 기록, 긴급 호출, 작업/휴식 사진 기록을 지원하는 Android 앱입니다.

## 목차
- [개요](#개요)
- [주요 화면](#주요-화면)
- [반응형 UI 진행 상태](#반응형-ui-진행-상태)
- [서버 API 계약 현황](#서버-api-계약-현황)
- [기록 저장 안전성](#기록-저장-안전성)
- [인증·계정 전환 정책](#인증계정-전환-정책)
- [백엔드 확인 요청](#백엔드-확인-요청)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [검증과 품질 검사](#검증과-품질-검사)
- [개발 워크플로](#개발-워크플로)
- [문서](#문서)
- [라이선스](#라이선스)

## 개요
폭염 환경에서 작업하는 현장 인력의 체온·작업/휴식 상태를 기록하고, 위험 상황 발생 시 관리자에게 긴급 호출을 보낼 수 있도록 돕는 것을 목표로 하는 Android 앱입니다.
현재 구현된 작업자 앱 화면(Team 로그인, 이메일 인증 기반 가입, 현장 연결·선택, 홈, 체크리스트, 긴급 호출, 기록, 프로필, 문의, 알림, 회원탈퇴)은 Jetpack Compose Navigation 3(`NavDisplay`)를 사용합니다. 로그인·가입 이후 기능은 Team 세션과 `/api/v1/auth/team/*`, `/api/v1/team/*` API를 사용합니다. 기존 `APP_ACCOUNT`로 저장된 세션은 권한을 바꾸지 않고 만료시켜 다시 Team으로 로그인하도록 합니다. Team 가입·이메일 인증·현장 연결 API의 서버 반영 상태와 실환경 검증 범위는 아래 계약 현황에 기록합니다. 전화번호 인증 현장 연결 링크는 `heartguard://team/invitations/{token}` 커스텀 URI로 받습니다. 기존 HTTPS 현장·초대 링크는 더 이상 처리하지 않습니다. URI의 토큰은 Android Keystore에 암호화해 저장하며, 새 링크를 열면 최신 토큰으로 교체되고 연결 완료 또는 취소 시 삭제됩니다. 이 URI를 열려면 HeartGuard가 설치되어 있어야 합니다. 서버가 제공하지 않는 값은 고정값 대신 `--`로 표시합니다.

PRD 문서는 저장소에 없습니다. 사용자 확인에 따라 기존 README·사용자 제공 요구사항·Figma·API 명세 v0.1을 작업 범위 자료로 사용하며, 별도 PRD는 생성하지 않습니다.

## 주요 화면
`app/src/main/java/com/nativelap/heartguard/navigation/HeartGuardDestination.kt`에 정의된 목적지 기준입니다.

| 화면 | 설명 |
|---|---|
| Login / SignUpForm / PhoneNumberLink | Team 로그인, 이메일 인증코드 기반 Team 가입·로그인, 커스텀 URI에서 전달된 현장 토큰과 담당자 전화번호로 현장 연동 |
| Home | 현재 온도·습도·체감온도·폭염 단계·날씨 상태(`weather.skyStatus`)·온도 변화량(`weather.temperatureDelta`), 체크 타임라인(서버 `checkTimes` + 오늘 기록으로 완료 표시). "관리자 전화"는 기존 현장관리자 긴급호출 화면으로 이동한다. "긴급 전화"는 임시 본사 번호(010-5080-4623)를 확인창과 전화 앱에 표시하며, 전화 앱에서 사용자가 발신을 확인한다. 화면 재개 시 새로고침, 조회 실패 시 재시도 안내 |
| ProfileEdit / PasswordChange | 내 정보 조회·이름 수정(`/auth/team/me`, `version`으로 동시 수정 충돌 감지), 프로필 사진 조회·업로드·삭제, 회사명(`companyName`) 표시, 비밀번호 변경(`/auth/team/password`) |
| Inquiry | 문의 등록과 내 문의 목록(상태 배지·등록일). 문의 상세 화면은 범위 밖 |
| Notifications | 홈·기록 화면의 벨에서 진입. 전체/기록/긴급/공지 분류 필터와 20개 단위 cursor 페이지를 제공합니다. 응답의 `type`과 `resourceId`는 데이터 계층에서 알림 대상(기록 `recordId`·긴급호출 `callId`·문의 답변 `inquiryId`)으로 구분해 매핑하며, 명세에 없는 `type`은 대상이 없습니다. 행 탭은 안 읽은 알림을 읽음 처리하고 관련 화면을 엽니다(기록 → 기록 상세, 문의 답변 → 문의하기, 긴급호출 확인 → 진행 중인 호출이 있을 때만 호출 중 화면). 이미 읽은 알림도 탭할 수 있으며, ‘모두 읽음’ 버튼은 서버의 일괄 API를 한 번 호출한 뒤 목록을 다시 조회합니다. 서버에 알림 본문 필드가 없어 제목·시각·읽음 상태를 표시합니다. 초기 로딩·오류/재시도·전체/분류별 빈 상태·추가 페이지 로딩/재시도 상태를 제공합니다 ([Figma 목록 프레임](https://www.figma.com/design/d9MzGARXpSX1Y53E64hdog/%EC%9D%B4%EC%9D%8C%EC%82%B0%EC%97%85%EA%B1%B4%EC%84%A4?node-id=914-1016), [빈 상태 프레임](https://www.figma.com/design/d9MzGARXpSX1Y53E64hdog/%EC%9D%B4%EC%9D%8C%EC%82%B0%EC%97%85%EA%B1%B4%EC%84%A4?node-id=914-1236)) |
| RecordHistory / RecordHistoryDetail | 기간(기본 최근 7일, 최대 31일)·유형 필터·유형별 건수·날짜별 기록 목록과 온도계/사진 기록 상세. 위치·팀명은 기록 당시 값, 휴식 기록은 `restMinutes` 표시 |
| Emergency / Calling | 홈 "관리자 전화"로 진입. "긴급 호출하기"를 눌러 호출 등록 → 호출 중(관리자 확인 시 연결됨) → 취소·종료. 앱 재시작 시 진행 중 호출을 이어받음 |
| RecordTypeSelection | 온도계 기록 / 작업 사진 / 휴식 사진 중 기록 유형 선택 (바텀시트) |
| TemperatureRecord | 온도계 측정값 입력 |
| FieldPhoto / WorkPhoto / RestPhoto | 사진 종류 선택 후 기본 시스템 카메라 앱 촬영 또는 Photo Picker 앨범 선택 (종류별 최대 2장). 휴식 기록은 시작·종료 시각을 TimePicker로 선택하고 자정을 넘기는 구간을 지원 |
| PhotoCamera | 선택한 기록 유형으로 기본 시스템 카메라 앱을 열어 촬영 |
| SaveSuccess / SaveFailure | 저장 성공·명확한 실패·결과 확인 필요를 구분. 결과 불명 제출은 재전송 대신 기록 내역 확인을 안내하며, 성공 후 로컬 정리 실패는 별도 경고 |
| WithdrawNotice / WithdrawConfirm / WithdrawDone | 회원탈퇴 안내·확인·완료. 현재 비밀번호 확인 후 팀 계정과 세션을 비활성화 |

세션은 `core/session/SessionManager`와 Android Keystore 기반 토큰 저장소(`AndroidKeystoreTokenStorage`)가 관리하고, `HeartGuardNavHost`가 `SessionState`를 구독해 인증 화면을 전환합니다. 화면 ViewModel이 Activity 수명으로 남기 때문에, 사용자 데이터를 가진 ViewModel은 `clearStateWhenSessionEnds`로 로그아웃·세션 만료 시 이전 작업자의 값을 지웁니다. 비밀번호 확인 요청(회원탈퇴·비밀번호 변경)은 `PasswordConfirmationRequest` 태그를 붙여, 비밀번호 불일치(401)가 세션 만료로 처리되지 않게 합니다. 알림 ViewModel도 세션 종료 시 목록과 읽지 않은 수를 초기화합니다. 읽지 않은 항목을 누르면 해당 항목만 읽음 처리하고, 실패하면 읽지 않은 상태를 유지하며 Snackbar로 안내합니다.

사진 흐름은 사진 종류 선택 → 기본 시스템 카메라 앱 촬영 또는 Android Photo Picker(`PickVisualMedia`) 앨범 선택 → 공유 `RecordDraftViewModel` 목록 반영 → 업로드 및 기록 제출 순서입니다. 카메라 앱은 사용자가 촬영 버튼을 눌렀을 때 열리며, AndroidX `TakePicture` 계약이 앱 캐시의 `photos/` 하위 `FileProvider` URI에 임시 접근 권한을 전달합니다. 앱은 `CAMERA` 권한을 요청하지 않습니다. 카메라가 취소되거나 파일이 비어 있으면 해당 임시 파일을 지웁니다. 사진 종류별 최대 2장까지 담으며, 저장 확인 화면 없이 제출하며, 저장은 `RecordDraftViewModel`의 `viewModelScope`에서 실행하며 연타를 차단합니다. POST 이후 취소되거나 응답을 확인하지 못한 제출은 로컬 표식을 유지해 같은 제출의 재전송을 차단합니다. 기록 흐름이 끝나거나 사진을 삭제하면 촬영 캐시 파일을 정리합니다. 온도·습도·메모 등 일부 입력 초안과 외부 카메라로 전달한 미완료 촬영 URI는 복구하지만, 선택 사진 URI와 업로드 키는 프로세스 재생성 후 복구하지 않습니다.

## 반응형 UI 진행 상태
홈, 프로필 수정, 문의, 알림, 기록 내역, 긴급 호출·호출 중, 사진 종류 선택, 온도 기록, 저장 성공·실패, 탈퇴 안내·완료 화면에는 최대 600dp 중앙 콘텐츠 래퍼와 하단 동작 폭 제한을 적용했습니다. 로그인은 폼의 실제 높이에 따라 상단 여백을 줄이고 상태바·내비게이션 바·키보드 여백을 처리합니다. 키보드가 없을 때는 폼을 스크롤하고 하단 로그인 동작을 유지하며, 키보드가 열리면 폼과 하단 동작을 함께 스크롤해 짧은 높이에서도 입력칸을 확인할 수 있도록 합니다. 탈퇴 확인의 안내 문구는 별도 스크롤 영역을 제공합니다. 로그인 입력 영역, 알림 빈 상태, 탈퇴 완료 본문은 작은 화면에서 스크롤됩니다. 사진 촬영 안내는 앱 화면에서 제공하며 촬영은 시스템 카메라 앱으로 전환합니다. 2026-10-01 에뮬레이터에서 로그인 393×582dp 잘림 수정과 실제 IME 표시를 캡처로 확인했습니다. 확대 글꼴·탈퇴 화면의 넓은 폭·스크롤은 과거 계측 테스트로 검증했으나, 테스트 소스 제거 후 자동으로 재현할 수 없습니다. 저장 결과 제목·메시지·요약·버튼은 확대 글꼴에 맞춰 높이가 늘어나고, 메뉴는 짧은 화면에서 스크롤됩니다. 시트 본문도 최대 600dp로 제한하며 온도계 수동 입력 스위치에 접근성 이름을 연결했습니다. 저장 결과의 긴 요약·가로 오류 화면, 메뉴 스크롤, 태블릿 중앙 정렬과 스위치 semantics는 당시 계측 테스트로 확인했습니다. 이 결과는 역사적 기록이며 현재 자동 검증은 제공하지 않습니다. 모든 작업자 프레임의 인증된 화면과 기기 크기별 시각 비교는 아직 완료되지 않았습니다.

로그인 브랜드 이미지는 사용자 제공 투명 PNG를 `app/src/main/res/drawable-nodpi/heart_guard_logo.png`로 사용합니다. `BrandMark`는 80dp 영역에서 `ContentScale.Fit`으로 원본 비율을 유지합니다. 설치한 앱의 로그인 화면에서 PNG 표시를 시각 확인했으며 [검증 보고서](docs/verification/2026-10-04-functional-regression.md)에 캡처를 보관했습니다. 호출 취소 버튼에는 1dp 테두리와 surface 배경을 적용했습니다.

## 서버 API 계약 현황
2026-10-27 기준 서버 API 명세 v0.1의 추가 요구사항 PR #32는 머지되어 테스트 서버에 배포됐습니다. 배포 후 서버 상태 확인은 200 UP이었지만, 이는 모든 API의 Android 연동이나 실환경 검증 완료를 뜻하지 않습니다. 특히 프로필 사진 API는 배포됐으나 S3 업로드가 `403 InvalidAccessKeyId`로 실패해 실제 업로드·확정·조회 검증이 남아 있습니다. 이메일 발송의 실환경 전달 여부도 별도 검증이 필요합니다.

기존 필드 기준은 2026-09-28 테스트 서버와 2026-09-29 API 명세 v0.1에서 확인한 내용입니다. 신규 필드(`company`, `weather.skyStatus`·`temperatureDelta`, `companyName`·`version`, 기록의 `restMinutes`·`teamName`·`workplace`·`siteName`)는 당시 실서버 응답에 있었습니다. 기록 응답에는 명세에 없는 `restStartedAt`·`restEndedAt`도 있어 휴식 시간 표시에 사용합니다.

알림과 휴식 기록 계약은 사용자 제공 Downloads의 `개인 페이지 & 공유된 페이지 11` 명세를 기준으로 합니다. 알림 분류 매핑은 `RECORD`→`RECORD_CREATED`, `EMERGENCY`→`EMERGENCY_ACKNOWLEDGED`, `NOTICE`→`INQUIRY_ANSWERED`입니다.

앱은 작업자 인증·기능에 Team API를 사용합니다. #32는 단계별 Team 이메일 인증과 가입 계약을 명시하며, 가입은 세션을 만들지 않고 로그인 토큰이나 쿠키를 발급하지 않습니다. 가입 완료 후 로그인 API를 별도로 호출해야 합니다. 프로필 사진 API는 `TEAM_MEMBER` 권한으로 사용할 수 있으며 팀 소속은 요구하지 않습니다. 체크리스트·기록 조회에는 활성 팀 소속이 필요합니다. 커스텀 URI `heartguard://team/invitations/{token}`만 수신하며 HTTPS 링크는 처리하지 않습니다. 토큰은 Keystore 암호화 저장소에 성공·취소까지 보관합니다. 기존 `APP_ACCOUNT` 토큰을 `TEAM_MEMBER`로 재분류하지 않으며, 앱은 이전 세션을 로컬에서 만료시킵니다. 서버는 마이그레이션 전에 발급된 앱 토큰을 폐기하거나 만료시키는 정책을 적용해야 합니다.

| API | 앱 연동 | 비고 |
|---|---|---|
| `POST /auth/team/login`, `POST /auth/team/logout` | 연결 | 응답 `data{accessToken, expiresAt, user, siteId, teamId}`, refresh token 없음 |
| `GET·PATCH /auth/team/me` | 연결 | 응답 `{userId, name, email, phone, companyName, role, …, version}`. PATCH는 `name·email·phone` 중 하나 이상 필요, 앱은 `name`과 마지막으로 받은 `version`을 보냄. 409는 최신 정보 재조회 후 안내 |
| `PUT /auth/team/password` | 연결 | 요청 `{currentPassword, newPassword}`, 성공 `{changedAt}`, 현재 비밀번호 오류 401 `INVALID_CREDENTIALS`, 8자 미만 400. 변경 후에도 현재 세션 유지 |
| `GET /team` | 연결 | 기상 미입력 시 기상·비교 필드는 null, 관리자 연락처는 빈 문자열일 수 있음. 응답 DTO가 `company.phone`, `weather.observedAt`·`skyStatus`·`temperatureDelta`·비교값, `unreadNotificationCount`를 파싱한다. 화면에서 사용하는 값만 Domain에 매핑한다. |
| `POST /team/uploads` → presigned `PUT` → `POST /team/records` | 연결 | `files[i] ↔ uploads[i]` 순서 보장, `slot`은 기록 내 사진 위치, `requiredHeaders`를 그대로 붙여 PUT. REST 기록은 필수로 선택한 `restStartedAt`·`restEndedAt`을 오프셋 포함 시각으로 보내며, 종료 시간이 같거나 이르면 다음 날로 처리. `restMinutes`는 서버가 계산 |
| `GET /team/records`, `GET /team/records/{recordId}` | 연결 | 목록은 선택적 `date=YYYY-MM-DD` 하루 단위 필터를 지원하고 `measuredAt`을 서울 날짜로 변환해 페이지네이션 전에 적용합니다. 기간·유형 쿼리는 지원하지 않음. 목록 항목에는 `photoKeys`만, 상세에는 `photoUrls` 추가. `restMinutes`·`teamName`·`workplace`·`siteName`은 기록 당시 값(이전 기록은 null) |
| `POST·GET /team/inquiries` | 연결 | 목록 항목 `{inquiryId, title, content, status, deliveryStatus, replies[], createdAt}`, 알 수 없는 cursor는 400 |
| `POST /team/emergency-calls`, `GET …/current`, `PATCH …/{callId}` | 연결 | `current`는 종료된 이전 호출(CANCELLED 등)도 반환하므로 앱은 자신이 시작·이어받은 `callId`만 반영. 취소·종료의 409 `INVALID_STATUS_TRANSITION`은 현재 상태를 다시 조회해 화면을 맞춤 |
| `GET /api/v1/team/notifications` | 연결 | `category`는 선택값(`ALL`·`RECORD`·`EMERGENCY`·`NOTICE`, 생략/빈 값은 `ALL`), `limit`은 1..100(기본 20), `cursor`는 선택값이며 앱은 선택한 분류를 전송합니다. 응답 `items`는 `{notificationId, type, category, title, resourceId, read, createdAt, updatedAt}`, `page`는 `{nextCursor, hasMore}`, 카운트는 전체 `unreadCount`와 분류별 `filteredUnreadCount`. 알림 유형은 `RECORD_CREATED`, `EMERGENCY_ACKNOWLEDGED`, `INQUIRY_ANSWERED`. |
| `PATCH /api/v1/team/notifications/{notificationId}/read` | 연결 | 본인에게 공개된 알림 한 건을 사용자별로 읽음 처리합니다. body 없이 요청하며 응답은 `{notificationId, read, readAt}`입니다. |
| `PATCH /api/v1/team/notifications/read-all` | 연결 | Query·Body 없이 본인의 모든 분류·페이지 알림을 읽음 처리합니다. 응답 `{updatedCount, unreadCount}`를 반영하고 목록을 유지한 채 현재 분류 첫 페이지를 재조회합니다. 재조회 실패는 기존 데이터와 읽음 결과를 보존하며 GET만 재시도합니다. 개별 읽음과 겹친 목록 조회는 처리 완료까지 대기하고, 앱 복귀 조회는 최소 5초 간격으로 실행합니다. 처리 중 재진입·연타를 차단하고 세션 종료 시 요청을 취소·무효화합니다. |
| `DELETE /team/profile` | 연결 | 선택한 탈퇴 사유를 `reason`(고정 코드 `FIELD_WORK_ENDED` 등)으로 함께 보냄. 팀 전체 비활성화라 실계정으로 실행하지 않았으며, 과거 단위 테스트로만 확인했습니다. 현재 테스트 소스는 없습니다. |
| `GET /team/checklist`, `PUT /team/checklist/items/{itemId}` | 연결 | GET은 날짜 쿼리 없이 서버의 서울 날짜와 활성 항목을 반환합니다. PUT 응답의 체크 상태를 반영하고 재진입 시 다시 조회합니다. 체크 상태는 같은 팀에서 공유됩니다. |
| `POST /auth/team/email-codes`, `POST /auth/team/email-codes/verify`, `POST /auth/team/register`, `POST /auth/team/login`, `POST /auth/team/logout` | 연결 | 인증코드 검증이 일회성 `verificationToken`을 반환하고 가입 요청에 사용합니다. 인증·가입 API는 PUBLIC입니다. 가입은 무소속 `TEAM_MEMBER` 계정을 만들며 세션을 발급하지 않으므로, 가입 성공 후 로그인 화면으로 이동해 사용자가 명시적으로 로그인합니다. 서버 #32 테스트 서버 배포 완료; SMTP 실환경 전달은 확인 필요. |
| `POST /auth/team/profile-photo/uploads`, `PUT /auth/team/profile-photo`, `GET /auth/team/profile-photo`, `DELETE /auth/team/profile-photo` | UI까지 연결 | 조회, presigned URL 발급, 파일 PUT, 업로드 확정, 사진 연결 해제를 프로필 화면에서 제공합니다. 허용 MIME은 JPEG·PNG·WebP, 파일 크기는 1~5 MiB. 서명 URL은 메모리에서만 사용하며 서버가 반환한 만료 시각을 확인합니다. 서버 API 배포 완료, S3 PUT은 `403 InvalidAccessKeyId`로 실환경 성공 검증 미완료. |
| `POST /team/links/claim`, `GET /team/sites`, `POST /team/sites/{siteId}/select`, `DELETE /team/sites/{siteId}/link` | Android Team 계약 반영, 서버 검증 대기 | 커스텀 URI 현장 연결 토큰과 담당자 전화번호로 현장을 연동하고 목록·선택·연결 해제를 제공한다. 전화번호 대조는 SMS 인증이 아니다. pending 토큰은 Keystore 암호화 저장소에 연결 성공·취소까지 보관한다. |
| `GET /team/dashboard` | Android Team 계약 반영, 서버 검증 대기 | Team 세션으로 선택 현장 대시보드를 조회하고 전송 직전 저장된 `X-Site-Id`를 읽는다. 서버는 요청 현장이 해당 Team 계정에 연결됐는지 검증해야 한다. 작업·자료·팀원 관리 전체 화면은 아직 구현하지 않았다. |
| `GET /team/inquiries/{inquiryId}` | 미연결 | 작업자 앱 문의 상세 화면과 목록 화살표를 만들지 않기로 한 사용자 결정에 따라 범위 제외 |

알림·기록 내역·홈·긴급 호출·문의·프로필 변경의 RemoteDataSource는 성공 envelope의 `success`·`error`·필수 `data`를 검증하며, 모순되거나 누락된 응답을 성공으로 표시하지 않습니다. 정상 빈 목록과 현재 호출 `NONE`/`callId=null`은 유지합니다. 비밀번호 변경은 오류 없는 `success=true/data=null` 응답과 호환되며, Unit 응답인 로그아웃·회원탈퇴에는 필수-data 검증을 적용하지 않습니다. HTTP 200의 명시적 거절 코드가 HTTP 상태별 화면 안내와 같은 의미인지 여부는 서버 계약 확인이 필요합니다.

알림 명세에서 기록·긴급 알림은 팀에 공유하고 문의 답변 알림은 작성자에게만 공개합니다. 읽음 상태는 사용자별입니다. 공지 게시 API, 과거 기록 알림 소급 생성, FCM/APNs Push 발송은 명세 범위에 없으며 앱에도 푸시 등록·수신 기능이 없습니다.

2026-10-01 추가 계약 점검에서 긴급호출의 409 후 상태 재조회가 실패하면 기존 호출과 폴링을 유지하고 실패를 표시하도록 수정했습니다. 폴링의 정상 `NONE`·호출 ID 없음 응답은 현재 호출을 종료합니다. 기록 내역·문의·알림 목록은 첫 페이지만 받고 스크롤이 끝에 가까워지면 `nextCursor`로 다음 페이지를 이어 받습니다(기록 내역은 최신 날짜부터 하루씩, 기록이 없는 날은 건너뜀). `page` 누락, `hasMore=true`인데 커서 없음, 이미 받은 커서 반복은 잘린 목록을 성공으로 보여주지 않고 실패로 알립니다. 아직 받을 목록이 남아 있으면 건수 뒤에 `+`를 붙입니다(예: `32+건`). 홈 체크 타임라인용 오늘 기록 전체 조회만 하루 100페이지 안전 상한을 둡니다.

2026-10-04 호출 재진입 수정에서는 요청·폴링·화면 이동 효과에 호출 흐름과 세션 세대, 호출 ID를 연결해 이전 호출의 지연 응답·종료 이벤트가 새 호출을 닫지 않게 했습니다. Route는 `RESUMED` 수명 상태에서만 효과를 수집하고 현재 흐름에 속하는 효과인지 확인합니다.

## 기록 저장 안전성

기록 저장 POST 전용 Retrofit 서비스는 요청 본문을 재사용 불가로 감싸고 연결 복구 재시도·HTTP/HTTPS 리다이렉트를 비활성화합니다. 408·503 등의 응답에 따른 OkHttp 자동 재전송도 차단하며, 다른 API의 기존 재시도 정책은 유지합니다.

기록 저장 응답은 HTTP 성공만으로 판단하지 않습니다. `requireSuccessData`가 `success=true`, 오류 없음, 필수 `data`와 비어 있지 않은 `recordId`를 확인합니다. 모순되거나 누락된 응답, 응답 유실, 저장 여부를 확정할 수 없는 서버 오류는 `Unknown`으로 분류하며 성공 화면을 표시하지 않습니다. 명세의 명확한 거절 코드만 일반 실패로 처리합니다.

`SubmitFieldRecordUseCase`는 현재 작업자 `userId`를 조회하고 POST 전에 제출 ID·기록 유형·측정 시각을 로컬에 기록합니다. `RecordSubmissionRepository` → `RecordSubmissionLocalDataSource` 흐름으로 `noBackupFilesDir/record-submissions` 아래 `AtomicFile`에 저장하며, 파일명은 서버 주소와 작업자 ID의 SHA-256으로 계정·서버별로 분리합니다. 사진·메모·토큰은 이 표식에 저장하지 않습니다. 파일을 읽거나 확정 기록할 수 없으면 서버 제출을 진행하지 않습니다.

결과 불명 표식은 앱 재시작·로그아웃 후에도 남고, 같은 작업자로 로그인하면 기록 내역 화면에 서버 기록과 별도로 표시됩니다. 해당 제출 ID는 사진 업로드와 POST를 다시 실행하지 않습니다. 서버 내역과 시간·유형으로 자동 매칭하거나 이전 제출을 재시도 가능한 상태로 풀지 않으며, 내역 확인 후 별도의 새 기록을 작성할 수 있습니다. 서버의 Idempotency-Key 지원은 아직 확인되지 않아, 서로 다른 새 제출이나 다른 기기의 중복 저장까지 보장하지는 않습니다.

서버 저장 성공 후 표식 정리에 실패하면 성공 결과를 유지하면서 로컬 정리 필요를 안내합니다. 현재 ViewModel이 알고 있는 성공 표식은 기록 내역 진입·재시도 때 서버 재전송 없이 로컬 파일 정리만 재시도합니다. 정리가 끝나기 전에 프로세스가 종료되면 남은 표식은 결과 확인 필요 항목으로 표시될 수 있습니다.

## 인증·계정 전환 정책

- **요청 소유 세션**: 인증 요청은 만들어진 시점의 세션 스냅샷(세대·토큰)을 태그로 가지며, 전송 직전에 세션이 바뀌었으면 보내지 않습니다(`SessionChangedException`, 화면에는 오류로 노출하지 않음). 늦게 도착한 이전 세션의 401은 새 로그인을 끝내지 않습니다.
- **토큰 삭제 실패**: 로그아웃·만료 때 메모리 세션을 먼저 끝내고, 저장소 삭제가 실패하면 Keystore 키를 폐기해 남은 암호문을 읽을 수 없게 합니다. 저장값을 읽거나 복호화하지 못하면 로그아웃 상태로 시작합니다.
- **계정 전환 정리**: 로그인·로그아웃마다 세션 세대가 바뀌고, 로그아웃→재로그인이 빠르게 지나가도 홈·긴급호출·탈퇴·기록 초안·목록 ViewModel과 화면 이동 기록을 정리합니다. 업로드→저장처럼 여러 요청으로 이어지는 흐름은 단계 사이에 세대를 비교해 다른 계정으로 저장되지 않게 합니다. 기록 제출은 시작 시점의 `expectedSessionGeneration`을 UseCase·Repository·RemoteDataSource까지 전달하고, Retrofit의 로컬 `RequestSessionGeneration` 태그를 호출 생성 시에도 확인합니다. 이 값은 서버 요청 본문이나 헤더에 추가하지 않습니다.
- **초안 복원**: 프로세스 재생성 뒤 남은 입력 초안은 저장 토큰으로 복원된 같은 세션일 때만 되살리고, 새로 로그인한 세션이면 버립니다.

## 백엔드 확인 요청
앱에서 해결할 수 없어 서버 쪽 확인·추가가 필요한 항목입니다. 해당 UI는 고정값 없이 `--`로 표시하거나 숨깁니다.

- **체감온도 계산**: 테스트 기록(31.5°C, 습도 60%)의 체감온도가 81.9°C로 계산되어 내려왔습니다.
- **체크 완료 판정**: 홈 체크 타임라인 완료 여부 필드가 없어, 앱이 "체크 시각 ~ 다음 체크 시각 전" 사이 기록 유무로 판정합니다. 서버 기준 확인 또는 필드 추가가 필요합니다.
- **없는 필드**: 온도계 설치 여부(현재 측정 온도 유무로 표시), 기록 목록 썸네일 URL, 기록 목록 기간·유형 쿼리
- **현장작업자 온도 기록 검증**: 2026-10-01 실서버에서 온도계 기록(직접 입력) 촬영→업로드→저장→상세 사진→기록 알림까지 성공했습니다. 휴식·작업 기록은 서버가 `WEATHER_BASELINE_REQUIRED`(현장 기상값 미입력)로 거절해 저장 성공은 확인하지 못했고, 앱은 오류 안내를 표시합니다.
- **계약 확인**: `sha256` 인코딩(Base64/hex), `slot` 의미, `POST /team/records`의 Idempotency-Key 지원, 허용 이미지 형식·최대 크기

## 기술 스택
`gradle/libs.versions.toml`과 각 모듈의 `build.gradle.kts` 기준으로 확인한 값입니다.

| 영역 | 사용 기술 |
|---|---|
| 언어 | Kotlin 2.2.10 |
| UI | Jetpack Compose (BOM 2026.02.01), Material 3 |
| Navigation | AndroidX Navigation3 (`navigation3-runtime`, `navigation3-ui` 1.1.6), `NavDisplay` |
| 사진 | AndroidX Activity `TakePicture`, Android Photo Picker |
| DI | Hilt |
| 네트워크 | Retrofit, OkHttp, kotlinx.serialization 컨버터 |
| 이미지 | Coil 3.3.0 (`coil-compose`, `coil-network-okhttp`) — 기록 상세 사진 표시. 3.4 이상은 Kotlin 2.3+ stdlib이 필요해 현재 Kotlin 2.2와 호환되는 버전 사용 |
| 직렬화 | kotlinx.serialization.json 1.8.1 |
| 빌드 | Gradle (Kotlin DSL), Version Catalog, AGP 9.2.1 |
| minSdk / targetSdk / compileSdk | 34 / 37 / 37 |

Hilt와 Retrofit/OkHttp를 사용합니다. `core/di`, `core/network`, `core/session`에는 DI 모듈, Retrofit 서비스 생성 팩토리(`ApiRetrofitFactory`), 공통 API 결과 타입(`ApiResult`/`ApiError`), 세션 관리가 있습니다. `domain`과 `data`에는 로그인·로그아웃, 내 정보·비밀번호, 홈, 사진 업로드·기록 저장, 기록 내역, 긴급 호출, 문의, 회원탈퇴 기능의 UseCase·Repository·RemoteDataSource 흐름이 있습니다. 화면 상태는 `viewmodel`에 있습니다. 로컬 데이터베이스(Room 등)는 없으며, 결과 불명 기록 제출 표식은 Android `AtomicFile`로 별도 영속화합니다.

## 아키텍처
`AGENTS.md`가 안내하는 Presentation/Domain/Data/Core 책임을 `:app`, `:domain`, `:data`, `:core` Gradle 모듈로 나눕니다. 기존 Kotlin 패키지명과 화면·API 흐름은 유지합니다. Compose Route/Screen이 ViewModel의 상태와 이벤트를 연결하고, ViewModel은 UseCase를 호출합니다. Repository 구현은 RemoteDataSource를 통해 Retrofit API와 사진 파일 업로드를 처리하고, 기록 제출 표식은 LocalDataSource를 통해 처리합니다. `RecordDataModule`이 제출 Repository와 LocalDataSource 인터페이스를 Hilt로 연결하며, 로컬 파일 접근은 Application Context에서 얻은 경로·IO dispatcher·Singleton Mutex를 사용합니다.

모듈 간 컴파일 의존성은 다음과 같습니다. `:app`은 Hilt 조립을 위해 `:data`도 포함하지만, ViewModel은 기존처럼 UseCase와 Domain 계약을 사용합니다. `:domain`은 기존 사진 모델의 Android `Uri` 계약을 유지하기 위해 Android Library로 구성합니다.

```text
:app ──▶ :domain ──▶ :core
  ├────▶ :data ────▶ :domain, :core
  └────▶ :core
```

아래 패키지명은 소속 모듈의 `src/main/java/com/nativelap/heartguard/` 아래를 기준으로 합니다.

```text
:app
view/
├── route/       # 화면별 진입점(Route) — 콜백을 받아 Screen을 조립
├── screen/      # 화면 단위 Composable(Scaffold 포함)
└── component/   # 화면별 재사용 Composable (account, auth, emergency, feedback, history, home, inquiry, menu, photo, profile, temperature 등)
viewmodel/       # account / auth / emergency / history / home / inquiry / menu / password / profile / record 상태와 이벤트
navigation/      # HeartGuardDestination(Navigation3 목적지), HeartGuardNavHost, 커스텀 SceneStrategy
core/component/  # 다이얼로그 배경 블러 등 공통 UI 유틸
core/util/       # 사진 파일·촬영 URI·전화 앱 실행 유틸
ui/theme/        # Theme, Shapes, Type, Dimension

:domain
domain/          # 모델, Repository 계약, UseCase

:data
data/            # DTO·Mapper·RemoteDataSource·Repository 구현, record/local 제출 표식

:core
core/di/         # 공통 Hilt 모듈·dispatcher
core/network/    # Retrofit·OkHttp, API 결과 및 인증 처리
core/session/    # 세션 상태와 Keystore 기반 토큰 저장
```

사진 Route에서 카메라 또는 앨범 출처를 선택합니다. 시스템 카메라 촬영 결과와 Photo Picker URI는 기록 종류별 목록을 보유한 공유 `RecordDraftViewModel`에 추가됩니다. 저장 버튼은 사진 1~2장을 업로드(URL 발급 → PUT)한 뒤 받은 `objectKey`로 `/api/v1/team/records`에 기록을 제출하며 `measuredAt`에는 첫 저장 버튼 시각을 사용하며, 같은 제출의 명확한 실패 후 재시도에서도 제출 ID와 해당 시각을 유지합니다.

## 프로젝트 구조
```text
HeartGuard/
├── app/                      # Android Application: 진입점, UI, Navigation, ViewModel
│   └── src/
│       └── main/
│           ├── java/com/nativelap/heartguard/
│           │   ├── MainActivity.kt
│           │   ├── navigation/
│           │   ├── ui/theme/
│           │   ├── core/      # component / util
│           │   ├── viewmodel/
│           │   └── view/      # route / screen / component
│           ├── assets/licenses/pretendard/
│           └── res/
├── core/                     # Android Library: 공통 네트워크·세션·DI
│   └── src/main/java/com/nativelap/heartguard/core/
├── domain/                   # Android Library: 모델·Repository 계약·UseCase
│   └── src/main/java/com/nativelap/heartguard/domain/
├── data/                     # Android Library: API·Repository·로컬 저장 구현
│   └── src/main/java/com/nativelap/heartguard/data/
├── .agents/skills/           # 이 저장소 계열 공통 개발 규칙
├── AGENTS.md                 # 저장소 공통 에이전트 계약
├── gradle/libs.versions.toml  # 의존성 버전 카탈로그
└── settings.gradle.kts       # :app, :core, :domain, :data
```

## 시작하기
### 요구사항
- Gradle Daemon JDK 21 (`gradle/gradle-daemon-jvm.properties`), Java 소스·타깃 호환성 17
- Android Studio 버전: `확인 필요` (저장소에 명시된 값 없음)
- Android SDK: compileSdk/targetSdk 37, minSdk 34 (`app/build.gradle.kts` 기준)

### 설치 및 실행
```bash
git clone https://github.com/team-nativeLab/heatguard-Android.git
cd heatguard-Android
./gradlew assembleDebug
```

### 환경 설정
서버 기본 주소는 Gradle 속성으로 설정하며, `data/build.gradle.kts`에서 `com.nativelap.heartguard.data.BuildConfig.BASE_URL`을 생성합니다. Debug는 `HEARTGUARD_DEBUG_BASE_URL`을 지정할 수 있고, 지정하지 않으면 제공받은 테스트 서버 주소(`https://heatguard-temp.https.gsmsv.site/`)를 사용합니다. 다른 API 서버에 연결하려면 유효한 HTTPS 주소와 마지막 `/`를 포함해 설정하세요.

```properties
# ~/.gradle/gradle.properties 또는 -P Gradle 속성으로 설정
HEARTGUARD_DEBUG_BASE_URL=https://YOUR_DEBUG_API_HOST/
HEARTGUARD_RELEASE_BASE_URL=https://YOUR_RELEASE_API_HOST/
```

Release 빌드는 유효한 실제 HTTPS 서버 주소가 설정되지 않으면 `:data:validateReleaseServerConfiguration` 검증 단계에서 실패합니다. 앱의 Release 빌드도 이 검증에 의존합니다. 2026-10-01 테스트 서버와 테스트 계정으로 로그인·자동 로그인 복원, 홈 조회, 사진 촬영·업로드, 기록 내역 이어 받기, 알림 모두 읽음, 문의 목록, 긴급호출 생성·취소, 로그아웃 후 재로그인 시 초안 정리, 온도계 기록 저장을 에뮬레이터에서 확인했습니다. 휴식·작업 기록 저장 성공은 위 서버 기상값 문제로 확인하지 못했습니다. 앱 계정 신규 API는 배포 서버 접근이 없어 당시 명세 기반 단위 테스트까지만 검증했습니다. 해당 테스트 소스는 현재 제거되어 자동 검증을 다시 실행할 수 없습니다.

## 검증과 품질 검사
사용자 요청에 따라 네 모듈(`app`, `core`, `domain`, `data`)의 `src/test`, `src/androidTest`, `src/testFixtures` 테스트 소스와 테스트 전용 구성을 제거했습니다. 따라서 현재 단위·계측 테스트는 없으며 `testDebugUnitTest`, `connectedDebugAndroidTest`로 기능 회귀를 자동 확인할 수 없습니다. 인증·세션·권한 분리·기록 저장 안전성을 포함한 과거 테스트 보호도 더 이상 실행되지 않습니다.

| 목적 | 명령 |
|---|---|
| Debug 빌드 | `./gradlew assembleDebug` |
| Lint | `./gradlew lintDebug` |
| Kotlin·Gradle 포맷 검사 | `./gradlew spotlessCheck` |

### 과거 검증 기록
아래 결과는 테스트 코드가 저장소에 있던 시점의 기록이며, 현재 코드에서 재실행할 수 없습니다. 2026-10-01 `testDebugUnitTest` 167개와 `connectedDebugAndroidTest` 20개가 통과했고, `lintDebug`는 오류 0개(경고 46개), `assembleDebug`는 성공했습니다. 2026-10-04 기록된 시점에는 단위 테스트 276개와 계측 테스트 32개가 통과했고, 마지막 응답 검증 변경 뒤 단위 테스트·`assembleDebug`·`lintDebug`가 성공했습니다(Lint 오류 0개·경고 52개). 당시 알림·REST·긴급호출·문의·기록·인증/세션 및 UI 테스트가 API 계약, 실패 경로, 계정 전환과 저장 안전성 등을 다뤘습니다. 이는 과거 상태의 결과이며 현재 기능 회귀 방지를 보장하지 않습니다.

로그인·탈퇴 IME의 과거 계측 재검증과 당시 남은 서버·관리자 연동 검증 조건은 [2026-10-04 회귀 검증 보고서](docs/verification/2026-10-04-functional-regression.md)에 기록되어 있습니다. 서버 실검증과 관리자 웹 연동은 당시에도 완료되지 않은 범위였습니다.

자세한 과거 PASS·BLOCKED·NOT_RUN 범위와 설치 앱 캡처는 [2026-10-04 회귀 검증 보고서](docs/verification/2026-10-04-functional-regression.md)를 참고하세요. 기록·호출·UI 변경의 [PR #103](https://github.com/team-nativeLab/heatguard-Android/pull/103)·[PR #105](https://github.com/team-nativeLab/heatguard-Android/pull/105)·[PR #106](https://github.com/team-nativeLab/heatguard-Android/pull/106)은 의존 브랜치에 연결된 Draft이며 병합하지 않았습니다. 마지막 응답 검증과 보고서는 [PR #108](https://github.com/team-nativeLab/heatguard-Android/pull/108)에 Draft로 게시했으며 Issue #107을 `Refs #107`로 연결합니다. 선행 PR을 포함하고 기본 브랜치는 dev이며, 병합하지 않았습니다.

## 개발 워크플로
- 코드 작업 전 `AGENTS.md`와 `.agents/skills/**/SKILL.md`를 먼저 확인하고 규칙을 따릅니다.
- GitHub Issue·브랜치·커밋·PR 규칙은 [`AGENTS.md`](AGENTS.md) 6장과 [`.agents/skills/android-github-workflow/SKILL.md`](.agents/skills/android-github-workflow/SKILL.md)를 따릅니다.

## 문서
- [2026-10-04 기능 수정 및 회귀 검증](docs/verification/2026-10-04-functional-regression.md) — 테스트 소스 제거 전의 역사적 자동 검증 결과, 시각 증거, 당시 실서버·관리자 연동의 남은 조건
- [`AGENTS.md`](AGENTS.md) — 저장소 공통 에이전트 계약(계층 구조, 기술 계약, 스킬 라우팅)
- `.agents/skills/` — 영역별 개발 규칙(SKILL.md 모음: DI, 네트워크, UI, Navigation, 디자인 시스템 등)
- PRD 문서: 저장소에 없음. 사용자 확인에 따라 별도 생성하지 않음

## 라이선스
라이선스 파일이 저장소에 없어 확인 필요합니다.
