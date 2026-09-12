---
name: android-hilt-di
description: Android 프로젝트에서 Hilt 주입 구조를 추가하거나 수정할 때 사용한다. `@Inject constructor`, `@Binds`, `@Provides`, `@Singleton`, Activity scope, qualifier, ViewModel 주입, binding 누락 오류를 다루는 작업에 적용한다.
---

# Android Hilt Dependency Injection

## 가독성

- 생성자 주입, binding, provider 선언을 한 줄로 압축하지 않는다. 의존성이 둘 이상이면 매개변수마다 줄을 나눈다.

## 실행 Hook

### `BeforeMutation`

- 새 의존성의 생성 방식이 `@Inject constructor`, `@Binds`, `@Provides` 중 무엇인지 결정한다.
- scope·Context qualifier·동일 타입 binding 충돌 여부를 확인한 뒤 Module을 수정한다.

### `AfterChange`

- Interface와 구현체의 모든 binding, 주입 지점 qualifier, Singleton의 참조 대상을 점검한다.
- ViewModel을 Module에서 직접 제공하지 않았는지 확인한다.

### `BeforeHandoff`

- 실제 Hilt/KSP compile을 실행해 graph 누락·중복 binding을 확인하고 결과를 기록한다.

## 기본 원칙

- 직접 생성 가능한 프로젝트 클래스는 `@Inject constructor`를 우선한다.
- Interface → 구현체 연결은 `@Binds`를 우선한다.
- Retrofit, OkHttpClient, ApiService, DataStore, 외부 Builder처럼 직접 생성 규칙이 필요한 타입은 `@Provides`를 사용한다.
- 앱 전체에서 하나만 유지해야 하는 객체에만 `@Singleton`을 사용한다.
- ViewModel은 `@HiltViewModel`과 `@Inject constructor`를 사용한다.
- 임의의 Service Locator 또는 Kotlin `object`로 DI를 우회하지 않는다.

## 선택 기준

### `@Inject constructor`

다음 조건이면 우선 사용한다.

- 생성자를 직접 제어할 수 있는 프로젝트 클래스
- 생성에 특별한 factory 코드가 필요하지 않음
- 구현 타입 자체를 직접 주입해도 됨

```kotlin
class GetProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
)
```

### `@Binds`

Interface와 구현체를 연결할 때 사용한다.
Module은 `abstract class` 또는 `interface`로 만들고 함수는 abstract로 선언한다.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectBindingModule {

    @Binds
    abstract fun bindProjectRepository(
        implementation: ProjectRepositoryImpl,
    ): ProjectRepository
}
```

다음 binding을 확인한다.

- Repository Interface → RepositoryImpl
- RemoteDataSource Interface → RemoteDataSourceImpl
- 필요하면 local data source Interface → Impl

### `@Provides`

다음 경우에 사용한다.

- 외부 라이브러리 타입
- Builder를 호출해야 하는 객체
- 생성자에 `@Inject`를 붙일 수 없는 타입
- BuildConfig나 qualifier를 이용한 생성 규칙이 필요함

예:

- OkHttpClient
- Retrofit
- ApiService
- Json / Gson
- Room database

## Scope 규칙

Scope는 객체의 실제 필요 수명과 일치시킨다.

| 필요 수명 | 일반적인 선택 |
|---|---|
| 앱 전체에서 하나 | `@Singleton`, `SingletonComponent` |
| Activity 생명주기 | `@ActivityScoped`, `ActivityComponent` |
| Activity-retained | `@ActivityRetainedScoped` |
| ViewModel | `@ViewModelScoped` |
| 별도 공유 필요 없음 | scope 없이 생성 |

`@Singleton`은 편의를 위한 기본값이 아니다.
상태 공유, 연결 유지, cache, 비용이 큰 생성 등 앱 단일 인스턴스 이유가 있어야 한다.

## Context와 qualifier

- Application 수명 객체는 `@ApplicationContext`만 보관한다.
- Activity UI 작업이 필요한 객체는 Activity 범위와 `@ActivityContext`를 사용한다.
- Activity Context를 Singleton에 주입하지 않는다.
- 같은 타입의 여러 객체가 있으면 의미 있는 qualifier를 사용한다.

## 여러 구현체 구분: Qualifier

같은 인터페이스에 대해 서로 다른 구현체나 설정을 여러 개 등록해야 하면(예: 카카오페이·토스페이처럼 결제 수단별 구현체, 서버 환경별로 다른 설정을 쓰는 클라이언트), Hilt는 타입만으로 어느 것을 주입할지 고를 수 없어 컴파일 시점에 binding 충돌로 실패한다. 이 경우 반드시 커스텀 `@Qualifier` 어노테이션으로 구분한다.

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class KakaoPay

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TossPay
```

`@Binds`/`@Provides` 각각에 대응하는 qualifier를 붙인다.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentBindingModule {

    @KakaoPay
    @Binds
    abstract fun bindKakaoPayGateway(impl: KakaoPayGatewayImpl): PaymentGateway

    @TossPay
    @Binds
    abstract fun bindTossPayGateway(impl: TossPayGatewayImpl): PaymentGateway
}
```

주입 지점(생성자 파라미터, `@Provides` 파라미터, field injection)에도 동일한 qualifier를 붙인다.

```kotlin
class CheckoutUseCase @Inject constructor(
    @KakaoPay private val kakaoPayGateway: PaymentGateway,
    @TossPay private val tossPayGateway: PaymentGateway,
)
```

- 타입 하나에 qualifier를 붙였다면, 그 타입을 제공하는 모든 경로에 빠짐없이 qualifier를 붙인다. qualifier 없는 기본 binding을 하나 남겨두면 Hilt가 의도와 다른 구현체를 주입할 수 있다.
- 간단한 문자열 라벨로 충분하고 이미 프로젝트에서 쓰는 패턴(예: `$android-network-environment`의 인증·비인증 OkHttpClient 구분)에는 `@Named("auth")` 같은 문자열 qualifier도 허용한다. 다만 여러 구현체가 존재하고 리팩터링 시 이름 오타로 인한 실수를 막아야 하면 커스텀 `@Qualifier` 어노테이션을 우선한다.

## ViewModel

```kotlin
@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val getProjectsUseCase: GetProjectsUseCase,
) : ViewModel()
```

- ViewModel을 Module에서 직접 provide하지 않는다.
- Compose Route에서는 `hiltViewModel()`을 사용한다.
- ViewModel에 Activity, View, NavController, Composable lambda를 저장하지 않는다.

## 작업 절차

1. 주입 대상의 생성자를 제어할 수 있는지 확인한다.
2. Interface binding인지 외부 타입 생성인지 구분한다.
3. 가장 짧은 적절한 scope를 정한다.
4. Context가 있다면 qualifier와 수명 일치를 검사한다.
5. Module의 `@InstallIn` component를 확인한다.
6. 동일 타입 binding 충돌이나 qualifier 누락을 확인한다.
7. Hilt compile을 실행해 graph를 검증한다.

## 완료 체크

- 모든 Interface에 binding이 있다.
- RepositoryImpl과 RemoteDataSourceImpl은 `@Inject constructor`로 생성 가능하다.
- Retrofit 계열은 `@Provides`로 한 곳에서 구성된다.
- 불필요한 `@Singleton`이 없다.
- 동일 타입에 여러 구현체가 있으면 모든 제공·주입 지점에 qualifier가 빠짐없이 붙어 있다.
- Application 수명 객체가 화면 객체를 참조하지 않는다.
- ViewModel 생성은 Hilt가 담당한다.
