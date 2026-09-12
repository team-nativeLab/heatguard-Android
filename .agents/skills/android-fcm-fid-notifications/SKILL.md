---
name: android-fcm-fid-notifications
description: Android 앱에서 Firebase Cloud Messaging(FCM) 알림·리마인더를 Firebase Installation ID(FID) 기반으로 등록, 서버 동기화, 수신 처리할 때 사용한다. `FirebaseMessaging.register()`, `FirebaseMessagingService.onRegistered(fid)`, FID 변경·삭제, Android 13 알림 권한, 알림 채널, 서버의 개별 FID 전송을 새로 구현하거나 기존 FCM 등록 토큰 방식에서 전환할 때 적용한다.
---

# Android FCM FID Notifications

## 기준

- FCM 개별 앱 설치 식별에는 FID 기반 등록을 사용한다. 등록 토큰 중심의 `getToken()`·`onNewToken()` 구현을 새 기능의 기준으로 만들지 않는다.
- FID는 Firebase가 생성·유지한다. UUID, Android ID, 광고 ID를 FID 대용으로 만들지 않는다.
- FID는 물리 기기 식별자가 아니라 **앱 설치 식별자**다. 재설치, 앱 데이터 삭제, Firebase Installations 삭제 시 바뀔 수 있으므로 사용자 계정과 1:1로 고정하지 않는다.
- FCM 발송 권한·서비스 계정 키·OAuth access token은 Android 앱에 넣지 않는다. 앱은 FID를 인증된 자체 서버에 등록하고, 서버만 Firebase Admin SDK 또는 FCM HTTP v1로 전송한다.

## 실행 Hook

### `BeforeWork`

- Firebase BoM·서비스·Manifest·권한·서버 등록·딥링크의 현재 상태를 확인한다.
- `FirebaseMessagingService`나 장기 수명 객체를 추가하면 `android-memory-safety`를 먼저 적용한다.

### `BeforeMutation`

- FID 등록·삭제의 서버 계약, 인증 상태, WorkManager 재시도 정책, 로그아웃 해제 정책을 확정한다.
- Android 앱에 서비스 계정 키·OAuth token·FID 원문 로그가 들어가지 않는지 확인한다.

### `AfterChange`

- 등록 callback에서 서버 UseCase 흐름을 우회하지 않는지와 서비스가 네트워크를 직접 호출하지 않는지 점검한다.
- 권한 거부·FID 변경·중복 등록·stale 설치·알림 탭 실패 경로를 확인한다.

### `BeforeHandoff`

- Android 버전별 권한·채널·포그라운드/백그라운드·딥링크 검증 결과와 미실행 항목을 기록한다.

## 구현 전 확인

1. Firebase Android BoM과 `firebase-messaging` 의존성, `google-services.json`, Google Services Gradle Plugin 적용 여부를 확인한다.
2. 서버가 FID를 받는 등록·삭제 API와 사용자 로그인 상태를 확인한다. 서버 API를 새로 만들거나 고치면 `$android-server-feature`를 적용한다.
3. Android 13 이상 `POST_NOTIFICATIONS` 런타임 권한, 알림 채널, 딥링크 목적지를 확인한다.
4. `FirebaseMessagingService`가 이미 있는지 찾아 하나의 서비스로 통합한다. 서비스가 장기 수명 의존성을 가질 경우 `$android-memory-safety`를 적용한다.

## FID 등록과 서버 동기화

`AndroidManifest.xml`에 FID 등록을 활성화한다.

```xml
<application>
    <meta-data
        android:name="firebase_messaging_installation_id_enabled"
        android:value="true" />
</application>
```

`FirebaseMessagingService`에서 `onRegistered(fid)`를 구현한다. 자동 초기화가 켜진 경우 최초 등록, 정기 동기화, FID 변경에 따라 호출될 수 있다. 콜백마다 현재 FID와 동기화 시각을 서버에 업로드한다.

```kotlin
class MoilFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        installationRegistrationScheduler.enqueue(installationId)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        notificationHandler.handle(remoteMessage)
    }
}
```

- 서비스에서 네트워크 요청을 직접 수행하지 않는다. 서비스는 신뢰성 있는 WorkManager 작업 또는 기존 앱의 등록 UseCase를 트리거한다.
- 업로드 API에는 `fid`, 플랫폼, 앱 버전, 마지막 동기화 시각만 보낸다. 현재 로그인한 사용자는 서버가 인증 정보에서 결정한다.
- 자동 초기화를 끈 경우에만 앱 시작 시 `FirebaseMessaging.getInstance().register()`를 호출하여 등록을 시작하고 `onRegistered(fid)`에서 서버 동기화를 처리한다.
- `FirebaseInstallations.getInstance().getId()`를 임의의 기기 ID 생성이나 `onRegistered` 대체 수단으로 사용하지 않는다. FCM 전달 대상 동기화의 기준은 FCM 등록 콜백이다.

## 서버 전송 계약

- 서버는 사용자 한 명에 여러 활성 FID를 보관할 수 있게 모델링한다. `userId + fid`를 설치 레코드의 식별자로 두고 `updatedAt`을 매 업로드마다 갱신한다.
- FCM HTTP v1 또는 Firebase Admin SDK로 각 활성 FID에 개별 메시지를 보낸다. 전송 오류가 등록 무효·미등록을 뜻하면 해당 FID 레코드를 삭제하거나 비활성화한다.
- 404로 삭제된 등록은 앱이 다음 시작 또는 `onRegistered` 콜백에서 다시 등록하도록 한다. 서버에서 FID를 재생성하지 않는다.
- 한 달 이상 동기화되지 않은 설치는 stale로 관리하고, Android에서 장기간 비활성화된 등록은 만료될 수 있음을 고려해 정리 정책을 둔다.

## 알림 표시와 사용자 행동

- Android 13 이상에서는 권한이 허용되기 전에는 알림 표시를 기대하지 않는다. 권한 요청 시점은 기능 가치를 설명할 수 있는 사용자 행동 뒤로 둔다.
- Android 8 이상에서는 알림 채널을 앱 시작 시 생성하고, 알람·일반 공지를 채널과 중요도별로 분리한다. 채널 이름·설명은 string resource로 둔다.
- data 메시지는 `onMessageReceived`에서 유효성을 검사하고, 앱 상태와 사용자 설정을 반영해 알림을 표시한다. `notification` payload의 백그라운드 자동 표시 동작에 핵심 업무 로직을 의존하지 않는다.
- PendingIntent는 명시적 Intent와 최소 권한 플래그를 사용한다. 외부 payload를 그대로 화면 이동 경로나 명령으로 실행하지 말고 허용된 딥링크·식별자만 처리한다.
- 알림 탭, 삭제, 권한 거부, 알림 설정 해제 상태를 UI 상태와 분석 정책에 맞게 처리한다.

## 계층과 오류 처리

- FID 등록 API 호출은 `ViewModel → UseCase → Repository → RemoteDataSource → ApiService` 경로를 따른다.
- 등록 실패는 네트워크·인증·일시 오류를 구분하고 재시도 가능한 작업만 WorkManager로 예약한다. FID 원문을 일반 로그, Crash 보고, UI에 노출하지 않는다.
- 로그아웃·계정 변경 시 서버의 `userId + fid` 연결을 해제한다. Firebase Installations 삭제나 FCM unregister는 실제 설치 데이터 삭제가 필요한 명시적 사용자 흐름에서만 수행하고, 다른 Firebase 기능 영향 여부를 먼저 검토한다.

## 완료 전 검증

1. 새 설치에서 `onRegistered(fid)`가 호출되고 서버에 FID가 등록되는지 확인한다.
2. FID 변경·서버 등록 삭제 뒤 앱 재실행 시 최신 FID가 다시 업로드되는지 확인한다.
3. 서버가 등록된 FID에 보낸 data 알림이 포그라운드·백그라운드에서 의도대로 처리되는지 확인한다.
4. Android 13 이상 권한 거부·허용, Android 8 이상 채널, 알림 탭 딥링크를 확인한다.
5. 빌드·테스트·lint와 리소스·메모리 안전성 검증에는 `$android-change-verification`을 적용한다.
