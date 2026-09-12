---
name: android-oauth-security
description: >
  Android 및 백엔드의 OAuth/OIDC 보안 규칙을 적용한다.
  Google, Kakao, Apple 로그인, PKCE, 토큰 저장, Redirect URI,
  Access Token, Refresh Token, Client ID, Client Secret,
  Android 인증 코드를 구현하거나 검토할 때 사용한다.
---

# Android OAuth 보안 규칙

## 목적

OAuth/OIDC 로그인 코드를 생성하거나 검토할 때
항상 실제 서비스에서 사용할 수 있는 보안 구조를 우선한다.

기본 구조는 다음과 같다.

```text
Android
→ OAuth 제공자(Google/Kakao/Apple)
→ Authorization Code
→ 우리 백엔드
→ OAuth 제공자 Token Endpoint
→ 우리 백엔드의 AccessToken / RefreshToken 발급
→ Android
```

Google은 예외적으로 Credential Manager를 통한 ID Token 직접 검증 방식도 사용할 수 있다. 두 방식 중 무엇을 쓸지는 9번 Google 항목의 선택 기준을 따른다.

---

# 1. Client Secret 보안

Android 내부에는 비밀값을 절대 저장하지 않는다.

다음 값은 Android에 넣으면 안 된다.

- GOOGLE_CLIENT_SECRET
- KAKAO_CLIENT_SECRET
- APPLE_PRIVATE_KEY
- 백엔드 API Secret
- 데이터베이스 비밀번호
- 서버 인증용 Private Key

다음 위치에도 저장하지 않는다.

- Kotlin 코드
- BuildConfig
- strings.xml
- AndroidManifest.xml
- assets/
- res/raw/
- APK에 포함되는 설정 파일

APK에 포함되는 값은 사용자가 추출할 수 있다고 가정한다.

`client_id`는 식별자이므로 필요한 경우 Android에서 사용할 수 있다.

Apple은 client secret 자체가 ES256으로 서명한 JWT다. 서명에 쓰이는 `.p8` Private Key와 Team ID·Key ID는 반드시 백엔드에만 보관하고, JWT는 발급 시점 기준 최대 15,777,000초(약 6개월) 이내로 만료를 설정한 뒤 만료 전에 백엔드가 재발급한다. (Apple Developer Documentation, "Creating a client secret")

---

# 2. PKCE 사용

Android와 같은 Public Client에서는 반드시 Authorization Code + PKCE(RFC 7636) 방식을 사용한다. RFC 8252(OAuth 2.0 for Native Apps)는 동일한 커스텀 URI Scheme을 여러 앱이 등록할 수 있어 발생하는 authorization code 가로채기 공격을 막기 위해 Public Native App Client에 PKCE 사용을 요구한다.

로그인할 때마다 다음 순서를 따른다.

1. 로그인을 시작할 때마다 암호학적으로 안전한 난수로 새 `code_verifier`(43~128자, RFC 7636 기준 `[A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~"`)를 생성한다.
2. 이전 로그인에서 사용한 verifier를 재사용하지 않는다. `code_verifier`는 1회용이며 메모리에만 보관하고 디스크에 저장하지 않는다.
3. SHA-256으로 `code_challenge`를 생성하고, `code_challenge_method=S256`으로 Authorization 요청에 포함한다. 검증이 약한 `plain` 방식은 사용하지 않는다.

```text
code_verifier
    │
    │  SHA-256 해시 후 Base64URL 인코딩(패딩 없음)
    ▼
code_challenge  ──► Authorization 요청(code_challenge, code_challenge_method=S256)에 포함

(사용자 인증 완료, Authorization Code 발급)

code_verifier  ──► 우리 백엔드가 Token 교환 요청(code, code_verifier)에 포함
                     └─► OAuth 제공자가 저장해둔 code_challenge와 대조 검증
```

4. `code_verifier`와 Authorization Code의 Token 교환은 Android가 아니라 우리 백엔드에서 수행한다. Android는 `code_verifier`를 백엔드로 전달하고, 백엔드가 OAuth 제공자의 Token Endpoint에 `code`와 `code_verifier`를 함께 전송해 최종 토큰을 발급받는다.

---

# 3. State 파라미터와 CSRF 방지

PKCE는 authorization code 탈취를 막지만, 공격자가 자신의 로그인 결과를 피해자의 세션에 주입하는 CSRF(Cross-Site Request Forgery) 공격은 별도로 막아야 한다. RFC 6749 §10.12는 이를 위해 `state` 파라미터 사용을 규정한다.

- 로그인을 시작할 때마다 암호학적으로 안전한 난수로 새 `state` 값을 생성해 Authorization 요청에 포함한다.
- `state`는 `code_verifier`와 함께 이번 로그인 시도 단위로만 저장하고, 콜백을 처리한 뒤 즉시 폐기한다.
- 콜백으로 돌아온 `state`가 요청 시 저장해둔 값과 정확히 일치하는지 반드시 검증하고, 값이 없거나 다르면 로그인을 실패 처리한다.
- state 검증 없이 콜백의 `code`만으로 로그인을 진행하지 않는다.

---

# 4. ID Token 검증 (백엔드)

Google처럼 OIDC를 지원하는 제공자의 ID Token은 Android가 자체적으로 파싱해 로그인 성공 여부를 판단하지 않는다. Android는 ID Token(또는 Authorization Code)을 백엔드로 그대로 전달하고, 신뢰 판단은 백엔드가 내린다.

백엔드는 ID Token을 신뢰하기 전에 다음을 모두 검증한다. (Google for Developers, "Verify the Google ID token on your server side")

- **서명**: 제공자의 공개키(JWK)로 서명을 검증한다. 공개키는 주기적으로 회전되므로 응답의 `Cache-Control` 헤더를 기준으로 캐시를 갱신한다.
- **`iss`(발급자)**: Google이면 `https://accounts.google.com`처럼 제공자가 문서화한 값과 정확히 일치하는지 확인한다.
- **`aud`(대상)**: 우리 서비스의 client_id와 일치하는지 확인한다. 이 검사가 없으면 다른 앱에 발급된 ID Token이 우리 백엔드에서도 통과할 수 있다.
- **`exp`(만료)**: 만료 시각이 지나지 않았는지 확인한다.
- **`nonce`**: Authorization 요청에 nonce를 포함시켰다면, 로그인 시작 시 생성해 보관한 값과 ID Token의 `nonce` claim이 일치하는지 확인해 재생(replay) 공격을 막는다.

디버깅용 tokeninfo 엔드포인트는 요청 제한이 걸릴 수 있어 운영 트래픽 검증에는 사용하지 않는다.

---

# 5. 외부 User-Agent 사용 (WebView 금지)

RFC 8252는 네이티브 앱이 로그인 화면을 렌더링할 때 앱에 내장된 WebView가 아니라 기기의 외부 User-Agent를 사용하도록 요구한다. 내장 WebView는 호스트 앱이 로그인 폼에 입력되는 모든 키 입력을 가로챌 수 있기 때문이다.

- Android에서는 `WebView` 기반 로그인 대신 Chrome Custom Tabs(AndroidX Browser)를 사용한다.
- Google 로그인은 백엔드 구조에 따라 (A) Credential Manager로 Google ID Token을 바로 받아 검증하는 방식과 (B) Custom Tabs로 Google Authorization 화면을 열고 Authorization Code를 백엔드가 PKCE로 교환하는 방식 중 하나를 선택한다(9번의 선택 기준 참고). 두 방식 모두 Google Sign-In SDK(`GoogleSignInClient`)는 사용하지 않는다 — Google이 신규 기능을 추가하지 않는 레거시 SDK다. Kakao는 카카오톡 앱 또는 Custom Tabs 기반 SDK, Apple은 공식 Sign in with Apple SDK처럼 각 제공자가 배포하는 공식 SDK를 우선 사용한다.
- 아이디·비밀번호 입력을 앱이 직접 렌더링하는 WebView 로그인 폼은 만들지 않는다.

---

# 6. Redirect URI 검증과 인터셉트 방지

- Redirect URI는 OAuth 제공자 콘솔에 등록한 값과 정확히 일치해야 하며, 와일드카드나 접두어 매칭을 허용하지 않는다.
- Android 커스텀 스킴은 다른 앱과 충돌하지 않도록 역도메인(reverse domain) 형식을 사용한다. 예: `com.example.app://oauth`.
- Kakao는 `kakao${NATIVE_APP_KEY}://oauth` 형식의 전용 스킴을 사용하며, `AndroidManifest.xml`에 `com.kakao.sdk.auth.AuthCodeHandlerActivity`를 등록하고 Android 12 이상에서는 `exported="true"`로 명시한다. (Kakao Developers, "Kakao Login > Android")
- 커스텀 스킴은 다른 앱도 동일한 scheme을 선점 등록해 Redirect를 가로챌 수 있다(Deep Link Hijacking). 가능하면 커스텀 스킴 대신 OS가 소유권을 검증하는 Android App Links를 사용한다. `AndroidManifest.xml` intent-filter에 `android:autoVerify="true"`를 설정하고, `https://<도메인>/.well-known/assetlinks.json`에 우리 앱의 패키지명과 서명 인증서 SHA-256 지문을 정확히 등록해야 시스템이 도메인 소유권을 검증하고 다른 앱이 같은 링크를 가로채지 못한다. `assetlinks.json`은 리다이렉트 없이 HTTPS로 접근 가능해야 한다. (Android Developers, "Add Intent filters for App Links")
- 커스텀 스킴을 계속 써야 하는 경우에도, Redirect로 받은 `code`와 `state`를 곧바로 신뢰하지 않고 3번의 `state` 일치 검증을 항상 통과시킨 뒤에만 로그인을 진행한다.
- Android 앱이 Public Client로 등록되어 있는지, OAuth 제공자가 client secret 기반 인증을 요구하지 않는지 확인한다. RFC 8252는 여러 기기에 배포된 정적 비밀값은 기밀로 취급할 수 없으므로 Public Native App Client에 shared secret 인증을 요구하지 말 것을 명시한다.

---

# 7. Access Token / Refresh Token 저장

- 우리 백엔드가 발급한 AccessToken/RefreshToken만 Android에 저장한다. OAuth 제공자(Google/Kakao/Apple)의 원본 토큰을 Android에 장기 보관하지 않는다.
- 토큰은 Android Keystore로 보호되는 저장소에 암호화해 저장한다. (Android Developers, "Security best practices") 프로젝트가 이미 `EncryptedSharedPreferences`나 DataStore + Keystore 조합을 표준으로 쓰고 있다면 그 정책을 따르되, 새로 구성하는 경우 Keystore 기반 암호화를 우선한다.
- AccessToken은 짧은 수명으로 발급하고, 장기 세션 유지는 RefreshToken 재발급으로 처리한다. 저장·조회·갱신·만료 처리는 이 프로젝트가 이미 갖춘 세션 관리 단일 진입점(`SessionManager` 등) 규칙을 그대로 따르고 새 저장소를 중복 생성하지 않는다.
- 토큰 값을 로그, 크래시 리포트, 분석 이벤트에 남기지 않는다.
- 로그아웃 또는 세션 만료 시 로컬에 저장된 토큰을 즉시 삭제한다. Provider 토큰까지 폐기하는 절차는 8번을 따른다.

---

# 8. 로그아웃과 Provider Token Revoke

로그아웃·탈퇴는 다음 순서로 처리한다.

1. Android 로컬에 저장된 우리 서비스 AccessToken/RefreshToken을 즉시 삭제한다.
2. 백엔드가 해당 세션/RefreshToken을 무효화한다.
3. 회원 탈퇴처럼 계정 연결 자체를 끊어야 하는 경우, 백엔드가 서버-to-서버로 OAuth 제공자의 토큰까지 폐기한다.

단순 로그아웃(재로그인 예정)과 회원 탈퇴(완전 연동 해제)를 구분해서, 탈퇴 시에만 아래의 연동 해제 API를 호출한다.

Provider별 로그아웃/연동 해제 엔드포인트(모두 백엔드에서 호출한다):

- **Google**: `POST https://oauth2.googleapis.com/revoke` — access token 또는 refresh token을 만료 전에 무효화한다.
- **Kakao**: 단순 로그아웃은 `POST https://kapi.kakao.com/v1/user/logout`, 회원 탈퇴처럼 완전한 연동 해제가 필요하면 `POST https://kapi.kakao.com/v1/user/unlink`를 호출한다. (Kakao Developers, "Kakao Login > REST API")
- **Apple**: Sign in with Apple REST API의 revoke tokens endpoint(`/auth/revoke`)에 refresh token 또는 access token을 담아 호출한다. 회원 탈퇴 시 반드시 호출해야 하며, 호출하지 않으면 사용자가 Apple ID 설정에서 앱 연결을 끊어도 우리 서비스에 발급된 refresh token이 계속 유효할 수 있다. (Apple Developer Documentation, "Revoke tokens" / TN3194)

---

# 9. Provider별 참고

- **Google**: 다음 두 방식 중 백엔드 구조에 맞는 쪽을 선택한다. 이미 Kakao/Apple과 동일한 Authorization Code 교환 구조를 갖췄거나 백엔드가 사용자 대신 Google API를 호출해야 한다면 (B)를, 로그인 여부 확인만 필요하고 별도 교환 구조를 두지 않으려면 (A)를 사용한다.

  **(A) Credential Manager + ID Token 직접 검증**: Android는 Credential Manager(`GetGoogleIdOption`)로 Google ID Token을 받아 백엔드로 그대로 전달한다. Authorization Code 교환 없이 ID Token 검증(4번)만으로 로그인을 신뢰한다.

  **(B) Authorization Code + PKCE(백엔드 교환)**: 1번의 일반 흐름을 그대로 따르며, 역할은 다음과 같다.
  - Android: 로그인 버튼 → Custom Tabs로 Google 로그인 화면 열기 → 최종적으로 우리 서비스 로그인 결과만 수신한다(Authorization Code를 직접 다루지 않는다).
  - 백엔드: `state` 생성 → `code_verifier` 생성·저장, `code_challenge` 생성 → Google 콜백(Redirect URI를 백엔드 또는 App Link로 설정)에서 `authorizationCode` 수신 → `code_verifier`와 함께 Google Token Endpoint와 교환 → `id_token` 검증(4번) → `sub`로 기존 사용자 식별 → 우리 서비스 AccessToken/RefreshToken 발급.
  - Google: 실제 Google 계정 로그인 처리 → PKCE(`code_challenge`) 검증 후 Authorization Code 발급 → 백엔드의 교환 요청에 `id_token`/`access_token` 발급.

  두 방식 모두 Google Sign-In SDK는 사용하지 않는다. `GOOGLE_CLIENT_SECRET`은 (B)의 Token Endpoint 교환, 또는 백엔드가 수행하는 다른 Google API 호출에만 사용한다.
- **Kakao**: 카카오 SDK가 발급하는 토큰을 그대로 앱 세션으로 쓰지 않고, Authorization Code 또는 Kakao Access Token을 백엔드로 전달해 백엔드가 카카오 서버에 검증 후 우리 서비스 전용 토큰을 재발급한다.
- **Apple**: `APPLE_PRIVATE_KEY`(.p8)와 Team ID, Key ID는 백엔드에만 보관한다. 백엔드는 ES256 JWT를 client secret으로 생성해 Apple Token Endpoint와 통신하며, 이 JWT는 최대 6개월 만료로 발급하고 만료 전 재생성한다.

---

# 완료 체크

- Android 코드, BuildConfig, 리소스, 설정 파일 어디에도 Client Secret·Private Key가 없다.
- 로그인마다 새 `code_verifier`를 생성하고, `code_challenge_method=S256`을 사용한다.
- `code_verifier` ↔ Authorization Code 교환이 Android가 아닌 백엔드에서 이뤄진다.
- 로그인 시도마다 새 `state`를 생성하고, 콜백에서 정확히 일치하는지 검증한 뒤에만 로그인을 진행한다.
- 백엔드가 ID Token의 서명·`iss`·`aud`·`exp`(nonce 사용 시 `nonce`까지)를 모두 검증한 뒤에만 로그인을 신뢰한다.
- 로그인 화면이 WebView가 아닌 Custom Tabs 또는 공식 제공자 SDK로 렌더링된다.
- Redirect URI가 제공자 콘솔 등록값과 정확히 일치하고, 와일드카드 매칭을 쓰지 않는다.
- Android App Links를 쓰는 경우 `autoVerify="true"`와 올바른 패키지명·서명 지문의 `assetlinks.json`이 배포되어 있다.
- Android가 Public Client로 등록되어 있고, OAuth 제공자가 shared secret 인증을 요구하지 않는다.
- 우리 백엔드 발급 토큰만 Android에 저장하며, Keystore 기반 암호화 저장소를 사용한다.
- 토큰이 로그·크래시 리포트·분석 이벤트에 노출되지 않는다.
- 로그아웃/탈퇴 흐름이 로컬 토큰 삭제 → 백엔드 세션 종료 → (탈퇴 시) Provider revoke/unlink 순서로 연결되어 있다.
