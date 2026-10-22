---
name: android-network-environment
description: Android의 BuildConfig, product flavor, Retrofit, OkHttpClient, Interceptor, Authenticator, 서버 URL, WebSocket URL, timeout, 네트워크 로그, API key 환경 구성, 민감 정보(local.properties·.gitignore·서명 키) 관리를 추가하거나 수정할 때 사용한다.
---

# Android Network and Build Environment

## 실행 Hook

### `BeforeWork`

- flavor·BuildConfig·Retrofit·OkHttp·Interceptor·Authenticator의 현재 구성을 확인한다.
- 인증·비인증 client 분리와 token 저장·갱신·종료 책임의 기존 단일 경로를 확인한다.

### `BeforeMutation`

- URL·Header·timeout·logging·인증 방식 변경 전 환경별 영향과 민감 정보 노출 여부를 확인한다.
- Interceptor에서 DataStore·Repository·UseCase를 직접 호출하거나 `runBlocking`을 사용하지 않는 구조를 확정한다.

### `AfterChange`

- feature 하드코딩 URL/Header, 운영 body logging, 인증·비인증 Retrofit 오주입, token refresh 중복 정책을 검사한다.

### `BeforeHandoff`

- BuildConfig 환경별 값, Base URL trailing slash, Hilt provider, 인증 헤더와 운영 로그 설정을 검증한다.

## 가독성

- 네트워크 설정, 인증 분기, 객체 생성을 한 줄로 압축하지 않는다. 보안 관련 조건과 설정값은 독립된 줄에 명시한다.

## 환경 분리

개발, 스테이지, 운영 서버는 product flavor 또는 프로젝트의 기존 환경 방식으로 분리한다.
feature 코드에 주소와 환경 값을 직접 작성하지 않는다.
서버 base URL과 환경마다 달라지지만 빌드 시점에 확정되는 고정 설정값은 모듈의 `build.gradle.kts`에서 `buildConfigField`로 선언하고 `BuildConfig`를 통해 사용한다.
예를 들어 `BASE_URL`, `APP_ENV`, 로그 활성화 여부, timeout, 개발 전용 기능 플래그는 flavor 또는 build type별 `BuildConfig` 값으로 제공한다.
`local.properties`나 Gradle property에서 값을 주입받더라도 앱 코드에서는 해당 값을 직접 읽지 않고, 생성된 `BuildConfig` 필드를 단일 진입점으로 사용한다.

권장 BuildConfig 필드:

| 값 | 필드 | 용도 |
|---|---|---|
| 서버 기본 주소 | `BASE_URL` | Retrofit 기본 주소 |
| WebSocket 주소 | `WEB_SOCKET_URL` | 채팅·알림 연결 |
| 이미지 서버 주소 | `IMAGE_BASE_URL` | 이미지 기본 주소 |
| 앱 환경 | `APP_ENV` | dev, stage, prod 구분 |
| 로그 출력 여부 | `ENABLE_LOG` | 개발 네트워크 로그 제어 |
| 연결 timeout | `CONNECT_TIMEOUT_SECONDS` | OkHttp 연결 timeout |
| 기능 플래그 | `ENABLE_MOCK_LOGIN` | 개발 기능 제어 |
| 공개 클라이언트 키 | `LIBRARY_API_KEY` | 클라이언트 전달이 허용된 키 |

## 필수 보안 원칙

- 민감값을 Git에 커밋하지 않는다.
- 로컬은 `local.properties`, CI는 환경 변수(CI secret)에서 주입한다. 세부 규칙은 아래 "민감 정보와 local.properties"를 따른다.
- BuildConfig 값은 최종 APK에서 추출될 수 있다고 가정한다.
- 진짜 비밀키는 앱에 넣지 않고 backend proxy 또는 서버에 보관한다.
- 운영 빌드에서는 상세 HTTP body logging을 끈다.

## 민감 정보와 local.properties

### 저장 위치

- API 키, 서명 비밀번호, keystore 경로처럼 Git에 올리면 안 되는 값은 프로젝트 루트의 `local.properties`에만 둔다.
- `.env`, `secrets.properties` 같은 별도 비밀 파일을 새로 만들지 않는다. 비밀값 저장 위치를 `local.properties` 하나로 유지한다.
- 키 이름은 대문자 스네이크 케이스로 쓰고 값에 따옴표를 넣지 않는다. 예: `KAKAO_NATIVE_APP_KEY=abc123`, `RELEASE_STORE_PASSWORD=...`

### Git 제외 확인

- 작업 전 `.gitignore`에 `/local.properties`, `*.jks`, `*.keystore`가 있는지 확인하고, 없으면 추가한다.
- `git ls-files local.properties`로 이미 추적 중인지 확인한다. 추적 중이면 `git rm --cached local.properties`로 추적을 해제하고, 이미 올라간 키는 노출된 것으로 보고 교체가 필요하다고 사용자에게 알린다.
- 커밋 전 `git status`와 `git diff --cached`로 민감값이 포함되지 않았는지 다시 확인한다.

### 읽는 위치와 전달 경로

- `local.properties`는 모듈의 `build.gradle.kts` 한 곳에서 `Properties()`로 읽는다. 앱 코드에서 파일을 직접 읽지 않는다.
- 앱 코드가 쓰는 값은 `buildConfigField` 또는 `manifestPlaceholders`로만 전달한다. 앱 코드는 `BuildConfig` 필드를 단일 진입점으로 사용한다.
- 서명 비밀번호처럼 앱 실행에 필요 없는 값은 `buildConfigField`로 노출하지 않고 `signingConfigs`에서만 사용한다.
- 값은 `local.properties` → 없으면 같은 이름의 환경 변수(`System.getenv`, CI secret) 순서로 읽는다. 둘 다 없으면 debug는 빈 값과 경고로 빌드하고, release는 빌드를 실패시키거나 서명 생략을 경고한다.
- 새 Gradle 플러그인(secrets-gradle-plugin 등)을 추가하지 않고 기본 `java.util.Properties`로 처리한다.

```kotlin
val localSecretProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { inputStream -> load(inputStream) }
    }
}

/** local.properties → 환경 변수(CI secret) 순서로 민감값을 읽는다. 둘 다 없으면 null. */
fun readSecret(secretKey: String): String? {
    return localSecretProperties.getProperty(secretKey)
        ?: System.getenv(secretKey)
}

android {
    defaultConfig {
        val kakaoNativeAppKey = readSecret("KAKAO_NATIVE_APP_KEY").orEmpty()
        if (kakaoNativeAppKey.isBlank()) {
            logger.warn("KAKAO_NATIVE_APP_KEY가 local.properties와 환경 변수에 없습니다.")
        }
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
    }
}
```

### 필요한 키 목록 공유

- 새 민감값을 추가하면 README의 "환경 설정" 절에 키 이름, 용도, 발급 위치를 값 없이 기록한다. 새 개발자가 이 목록을 보고 자신의 `local.properties`를 채운다.
- CI를 쓰면 같은 키 이름으로 CI secret을 등록하도록 README에 함께 적는다.
- 실제 값은 README, 이슈, PR, 커밋 메시지, 로그, 코드 주석 어디에도 쓰지 않는다.

## URL 규칙

- Retrofit `BASE_URL`은 `/`로 끝나야 한다.
- endpoint는 ApiService에서 상대 경로로 관리한다.
- WebSocket URL과 HTTP base URL을 역할별로 분리한다.
- 이미지 URL 조합 규칙이 반복되면 mapper 또는 전용 URL resolver를 사용한다.

## OkHttpClient

공통 네트워크 계층에서 다음을 설정한다.

- 인증 Header
- 공통 Header
- timeout
- logging
- Authenticator 또는 프로젝트의 단일 토큰 갱신 정책

기능별 Repository에서 Authorization Header를 반복 작성하지 않는다.

## Logging

`BuildConfig.ENABLE_LOG`가 true일 때만 logging interceptor를 활성화한다.
운영 환경에서 body 로그가 출력되지 않도록 level을 명시적으로 제어한다.

## 인증

- access token 부착은 공통 Interceptor에서 처리한다.
- access token 재발급은 Authenticator 또는 프로젝트가 선택한 한 가지 정책으로 처리한다.
- 인증 세션의 저장·조회·갱신·종료와 세션 상태 공개 책임은 `SessionManager` 한 곳에 둔다. 토큰 저장소를 기능별로 별도 생성하거나 UI가 `SharedPreferences`를 직접 관찰하지 않는다.
- 세션 변경 흐름은 아래와 같이 고정한다.

```text
Authenticator ─┐
Repository ─────┼──> SessionManager ──> 앱 최상위 Route ──> 로그인 화면
ViewModel ──────┘
```

- Authenticator는 refresh 실패 또는 재시도 한도 초과 시 `SessionManager.expireSession()`으로 세션을 종료한다. `expireSession()`은 토큰을 삭제하고 `SessionEvent.Expired`를 발생시킨다.
- Repository는 로그인·가입 성공 시 `SessionManager`에 토큰을 저장하고, 화면과 무관한 탈퇴 같은 세션 종료 정책을 처리한다.
- ViewModel은 사용자 로그아웃처럼 화면 의도가 세션 종료를 확정하는 경우 `SessionManager.expireSession()`을 한 번만 호출한다. Route·Screen은 Repository나 저장소를 직접 호출하지 않는다.
- 앱 최상위 Route는 `SessionManager`가 공개한 `StateFlow<SessionState>`와 세션 종료 이벤트를 lifecycle-aware 방식으로 수집한다. `Unauthenticated` 상태 또는 `SessionEvent.Expired`를 받으면 back stack을 로그인 화면으로 교체한다.
- Interceptor에서 `runBlocking`으로 DataStore·Repository·UseCase를 직접 호출하지 않는다.
- Interceptor와 Authenticator는 `SessionManager`만 사용한다.
- `SessionManager`는 Interceptor가 즉시 읽을 수 있는 안전한 access token 조회 경로와, Authenticator가 단일 정책으로 갱신·저장할 수 있는 API를 제공한다.
- 토큰 갱신에 네트워크 호출이 필요하면 Authenticator의 동기 실행 제약과 기존 동시성 제어 정책을 고려해 구현하며, UI·Repository 계층의 suspend 흐름을 Interceptor에 끌어오지 않는다.
- Repository별로 401 재시도 코드를 복제하지 않는다.
- 동시 401에서 중복 refresh가 발생하지 않도록 기존 동기화 정책을 확인한다.
- refresh 실패 시 토큰 정리와 로그아웃 상태 전달 경로를 분명히 한다.
- Authenticator는 401 응답 요청에 붙은 세션 스냅샷이 현재 세션과 같을 때만 세션을 만료한다(`expireSessionForRequest`). 이미 바뀐 세션의 늦은 401이 새 로그인을 지우지 않게 한다.
- 인증 클라이언트는 Retrofit `callFactory`로 요청 생성 시점의 세션 스냅샷을 태그로 붙이고, Interceptor는 그 스냅샷의 토큰을 붙인다. 전송 직전 세션이 바뀌었으면 `IOException` 하위 예외로 중단하고 `ApiError`에서 네트워크 오류와 구분한다.

## Retrofit과 직렬화

- 프로젝트가 사용하는 Gson 또는 Kotlin Serialization을 그대로 따른다.
- 한 기능 안에서 converter를 임의로 혼용하지 않는다.
- ApiService 생성은 Hilt `@Provides`에서 처리한다.
- ApiService는 HTTP 계약만 정의한다.

## 인증·비인증 클라이언트 분리

- `AuthorizationInterceptor`와 `Authenticator`가 붙은 인증용 `OkHttpClient`와, 둘 다 없는 비인증용 `OkHttpClient`를 각각 만든다. 공통 logging·timeout 설정은 두 클라이언트에 동일하게 적용한다.
- 각 `OkHttpClient`로 인증용·비인증용 Retrofit을 별도로 만들고, Hilt `@Named` qualifier로 주입 대상을 명확히 구분한다.
- 인증이 필요 없는 endpoint는 반드시 비인증 Retrofit으로 만든 ApiService를 사용한다. 로그인·회원가입·비밀번호 재설정·refresh·logout처럼 access token이 필요 없는 API도 비인증 경로에 둔다.
- 인증이 필요한 endpoint는 인증 Retrofit으로 만든 ApiService를 사용한다. 공개 조회와 인증 작업이 한 feature에 섞이면 ApiService를 공개용·인증용으로 분리하고 RemoteDataSource에 각각 주입한다.
- 선택 인증 endpoint는 로그인 상태의 추가 정보가 필요할 때만 인증 클라이언트를 사용한다. 토큰이 없을 때도 요청 가능한지와 401 처리 정책을 함께 확인한다.
- feature·Repository·ViewModel에서 Header를 직접 붙이거나 Retrofit을 선택하지 않는다.

## timeout과 상수

- timeout은 의미 있는 BuildConfig 또는 Core 상수로 관리한다.
- 숫자를 OkHttp Builder에 반복 하드코딩하지 않는다.
- 연결, 읽기, 쓰기 timeout이 다르면 각각 이름을 분리한다.

## 작업 절차

1. 현재 flavor와 BuildConfig 생성 설정을 확인한다.
2. 값이 source control에 포함되어도 되는지 분류한다.
3. Retrofit base URL과 endpoint 조합을 검증한다.
4. OkHttp Interceptor·Authenticator 순서와 중복을 확인한다.
5. logging이 환경 flag를 따르는지 확인한다.
6. Hilt `@Provides` graph를 연결한다.
7. dev와 prod 빌드 구성이 서로 올바른 값을 갖는지 검증한다.

## 완료 체크

- feature 코드에 URL과 token header가 하드코딩되지 않았다.
- Retrofit base URL과 환경별 고정 설정값을 `BuildConfig`에서 읽으며, feature 코드나 Kotlin 상수에 중복 선언하지 않았다.
- Base URL이 `/`로 끝난다.
- 운영 로그가 비활성화된다.
- 비밀키가 APK에 안전하다고 오해하는 설명이 없다.
- 민감값이 `local.properties` 또는 CI secret에만 있고, `local.properties`·keystore 파일이 `.gitignore`에 있어 Git에 추적되지 않는다.
- 새로 추가한 민감값의 키 이름과 용도가 README "환경 설정" 절에 값 없이 기록되어 있다.
- Retrofit, OkHttp, ApiService의 Hilt provider가 중복되지 않는다.
- 인증 필요 여부에 따라 ApiService가 올바른 Retrofit을 주입받고, 공개 endpoint에 Authorization Header가 붙지 않는다.
- token refresh 정책이 한 곳에 있다.
- Interceptor에 `runBlocking` 기반 토큰 조회·갱신 코드가 없고, 토큰 책임이 전용 클래스에 분리되어 있다.
