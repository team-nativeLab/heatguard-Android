---
name: android-layer-boundaries
description: Android MVVM 계층을 설계·리뷰·리팩터링하거나 Repository, UseCase, RemoteDataSource, Mapper의 책임이 헷갈리는 작업에서 사용한다. 계층 간 직접 참조, DTO 노출, 비즈니스 규칙 위치, cache 또는 데이터 소스 선택 책임을 판단할 때 적용한다.
---

# Android Layer Boundaries

## 가독성

- 계층 간 변환과 결과 분기(`when`)를 한 줄로 압축하지 않는다. 데이터 흐름과 실패 경로가 코드에서 바로 보이게 작성한다.

## 목적

각 계층이 무엇을 알고 무엇을 몰라야 하는지 기준을 세운다.
중복 책임과 계층 건너뛰기를 찾아 최소한의 구조 수정으로 바로잡는다.

## 실행 Hook

### `BeforeMutation`

- 새 타입의 계층과 입력·출력 타입, 의존 방향을 먼저 표로 정리한다.
- DataStore·Cipher·Room·Retrofit 기술 세부사항이 RepositoryImpl이나 Domain으로 새지 않을 경계를 확정한다.

### `AfterChange`

- `RepositoryImpl → RemoteDataSource → ApiExecutor → ApiService` 호출 경로를 확인한다.
- DTO·Retrofit 타입의 Presentation/Domain 노출, ApiService 직접 호출, 중복 오류 변환을 검사한다.

### `BeforeHandoff`

- 새 UseCase·Repository Interface·RemoteDataSource Interface/Impl·Hilt binding 누락을 확인한다.

## 책임 판단표

| 구성 요소 | 핵심 책임 | 하면 안 되는 일 |
|---|---|---|
| Route | ViewModel 획득, UiState 수집, callback 및 navigation 연결 | 화면 배치 집중, Repository 직접 호출 |
| Screen | 상태를 그리며 사용자 입력 callback 전달 | ViewModel·Repository·ApiService 참조, 네트워크 호출 |
| ViewModel | UI 이벤트 처리, UseCase 호출, UiState 갱신 | DTO·Response·ApiService 처리 |
| UseCase | 사용자 행동 하나와 업무 규칙 | Android Framework·DTO·Retrofit 의존 |
| Repository Interface | Domain 관점의 데이터 계약 | DTO·Response 노출 |
| RepositoryImpl | 데이터 소스 선택·조합·cache 정책·DTO→Domain 변환 | ApiService 직접 호출, UI 문자열 생성 |
| RemoteDataSource | 원격 호출과 네트워크 응답 추출 | Domain·UiModel 생성, 화면 상태 판단 |
| LocalDataSource / 기술 위임 컴포넌트 (TokenCipher, TokenStorage 등) | Room·DataStore·Cipher·KeyStore 등 특정 라이브러리 세부 동작 캡슐화 | 데이터 소스 선택, 조합, cache 정책 결정, Domain 모델 생성 |
| ApiService | HTTP 계약 정의 | 상태 저장, mapping, 업무 규칙 |
| Mapper | 타입 경계 변환 | 데이터 소스 선택, 네트워크 호출, 사용자 행동 규칙 |
| UiModel mapper | Domain을 화면 표시 구조로 변환 | 서버 호출, cache 정책 |

## 서버 통신 DataSource 경계

기능별 원격 서버 요청은 반드시 `RepositoryImpl → RemoteDataSource Interface → RemoteDataSourceImpl → ApiExecutor → ApiService` 경로로 처리한다.

- `ApiService` 의존성과 Retrofit 호출은 `RemoteDataSourceImpl`에만 둔다. 호출은 `ApiExecutor.execute { ... }`로 감싼다.
- RepositoryImpl은 RemoteDataSource를 통해 DTO 또는 공통 네트워크 결과를 받고, 데이터 소스 선택·조합 및 DTO → Domain 변환을 담당한다.
- UseCase, ViewModel, Route, Screen은 ApiService를 직접 주입·참조·호출하지 않는다.
- RemoteDataSource는 원격 데이터 접근과 응답 추출만 담당하며 Domain Model, UiModel, UiState, 화면 문구를 만들지 않는다.
- 토큰 갱신처럼 feature Repository에 속하지 않는 Core 네트워크 기술 인프라는 예외로 둘 수 있다. 다만 feature 계층에서 이를 우회 통로로 사용하지 않으며, 오류 처리와 수명 책임은 Core에 남긴다.

## Repository 구현 규칙

- Repository는 Domain 계층에 Interface를 먼저 정의하고, Data 계층의 `RepositoryImpl`이 이를 구현한다.
- Presentation과 UseCase는 항상 Repository Interface에 의존하며, `RepositoryImpl`을 직접 생성하거나 참조하지 않는다.
- 구현체 연결은 Hilt binding으로 관리해 구현 교체와 테스트 대역 적용이 가능해야 한다.

## RepositoryImpl 조합과 기술 위임의 경계

RepositoryImpl이 "여러 데이터 소스를 조합하는 코드"인지, 아니면 "DataStore·Cipher·Room 같은 특정 기술의 세부 동작"을 직접 다루는 코드인지 구분한다. 후자는 별도 컴포넌트(LocalDataSource, TokenCipher, TokenStorage 등)로 분리해 RepositoryImpl이 그 라이브러리를 몰라도 되게 만든다.

### 분리 신호 (다음 중 하나라도 해당하면 분리한다)

1. **서로 다른 데이터 소스를 2개 이상 사용한다.** 서버+Room, 서버+DataStore, Room+File처럼 복수 소스를 조합하는 순간 RepositoryImpl은 소스별 접근을 각 DataSource에 위임하고, 자신은 순서와 정책만 조정한다.
2. **Android/라이브러리 세부 API가 RepositoryImpl에 직접 등장한다.** `Cipher.getInstance`, `KeyStore.getInstance`, `GCMParameterSpec`, `context.dataStore.edit`, `dao.insert` 같은 코드나 `javax.crypto.*`, `androidx.datastore.*`, `androidx.room.*` import가 늘어나면 분리 신호다. RepositoryImpl은 이런 구현 기술을 몰라도 되어야 한다.
3. **동일한 세부 로직이 여러 Repository에서 재사용된다.** 토큰 암호화처럼 둘 이상의 RepositoryImpl이 같은 로직을 반복한다면 `TokenCipher`처럼 인터페이스로 뽑아 공유한다.
4. **Repository 메소드 하나가 여러 단계(캐시 확인·오류 변환·저장·보정 등)를 담당할 만큼 길어진다.** 단계가 늘어날수록 RepositoryImpl이 지나치게 많은 것을 알게 되므로 DataSource/CachePolicy 단위로 나눈다.
5. **RepositoryImpl 자체보다 세부 기능만 독립적으로 테스트해야 한다.** 예를 들어 암호화 알고리즘이나 DataStore 동작을 검증해야 한다면 그 부분을 인터페이스로 분리해 `FakeTokenCipher`처럼 대역을 주입할 수 있게 한다. RepositoryImpl 테스트는 "무엇을 호출했는가"만 확인하면 된다.

### 분리하지 않는 경우

로컬 데이터 소스가 하나뿐이고 단순 CRUD 위임 또는 얕은 매핑만 수행하며, 다른 Repository와 로직을 공유하지 않는다면 별도 LocalDataSource나 기술 위임 컴포넌트를 만들지 않는다. 예:

```kotlin
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao,
) : BookmarkRepository {
    override suspend fun getBookmarks(): List<Bookmark> {
        return bookmarkDao.getAll().map { it.toDomain() }
    }
}
```

이런 코드까지 `BookmarkLocalDataSource`로 감싸면 각 계층이 호출을 그대로 전달하는 껍데기가 되어 복잡도만 늘어난다.

단, 서버 호출은 이 예외와 무관하게 위 "서버 통신 DataSource 경계" 규칙에 따라 항상 RemoteDataSource를 거친다.

## 가장 중요한 구분

### UseCase와 RepositoryImpl

UseCase는 "사용자가 무엇을 하려는가"와 그 행동에 필요한 업무 규칙을 표현한다.

예:

- 로그인 시 빈 아이디를 거부한다.
- 프로젝트 참여 전에 모집 상태를 검사한다.
- 즐겨찾기 토글 후 분석 이벤트를 기록한다.

RepositoryImpl은 "필요한 데이터를 어디에서 어떻게 가져오고 저장할 것인가"를 결정한다.

예:

- cache가 유효하면 local을 사용하고 아니면 remote를 호출한다.
- 두 RemoteDataSource 결과를 조합한다.
- 원격 성공 후 Room 또는 Store를 갱신한다.
- DTO를 Domain으로 변환한다.

### Mapper와 RepositoryImpl

Mapper는 타입 변환만 담당한다.
RepositoryImpl은 mapper를 사용하면서 데이터 흐름을 조정한다.

```text
RemoteDataSource에서 DTO 수신
→ RepositoryImpl이 성공 여부와 cache 정책 판단
→ mapper로 DTO를 Domain으로 변환
→ 필요하면 local 저장소 갱신
→ Domain 결과 반환
```

Mapper가 있어도 RepositoryImpl은 필요하다.
Mapper는 "모양 변환"만 하며, 데이터 소스 선택과 저장 순서와 cache 정책을 결정하지 않는다.

### `@SerializedName`과 Mapper

`@SerializedName("user_name")`은 JSON 키와 DTO 프로퍼티를 연결한다.
DTO를 Domain으로 바꾸거나 서버 변경을 Domain에서 격리하지는 않는다.

```text
JSON user_name
→ @SerializedName으로 UserResponseDto.userName에 역직렬화
→ mapper로 UserResponseDto를 User Domain Model로 변환
```

## 계층 의존 방향

```text
Presentation → Domain ← Data
                  ↑
             Repository Interface
```

- Presentation은 Domain Model, UseCase만 안다.
- Domain은 Android Framework와 Data 구현을 모른다.
- Data는 Domain의 Repository Interface를 구현한다.
- Core Network는 공통 통신 인프라를 제공한다.

## 리뷰 절차

1. 각 파일의 입력 타입과 출력 타입을 확인한다.
2. DTO 또는 Retrofit 타입이 Presentation 또는 Domain에 새는지 찾는다.
3. RepositoryImpl이 ApiService를 직접 호출하는지 확인한다.
4. Repository Interface가 Domain 계층에 있고, RepositoryImpl이 Data 계층에서 이를 구현하는지 확인한다.
5. Presentation 또는 UseCase가 RepositoryImpl을 직접 참조하거나 생성하는지 확인한다.
6. RemoteDataSource가 Domain 또는 UiState를 만드는지 확인한다.
7. UseCase가 Android 타입이나 화면 문자열에 의존하는지 확인한다.
8. mapper가 데이터 조회나 상태 저장까지 수행하는지 확인한다.
9. 동일 오류 또는 동일 mapping이 여러 계층에 중복되는지 확인한다.
10. ApiService 호출이 RemoteDataSourceImpl 밖에 있지 않고, 모든 feature 호출이 `ApiExecutor`를 경유하는지 확인한다.
11. 불필요한 계층이 아니라 책임 누락인지 판단한 뒤 수정한다.
12. RepositoryImpl에 Cipher/KeyStore/DataStore/Room 같은 라이브러리 세부 API가 직접 등장하는지 확인하고, 등장한다면 "RepositoryImpl 조합과 기술 위임의 경계" 기준에 따라 분리가 필요한지 판단한다.

## 구조를 단순화해도 되는 경우

기존 코드베이스가 이미 다음을 제공한다면 중복 생성하지 않는다.

- 공통 `BaseRemoteDataSource`
- 공통 API 응답 unwrap 함수
- 공통 exception mapper
- 공통 UI mapper 규약

단, 공통 타입이 실제 책임을 수행하는지 코드로 확인한다.
이름만 비슷하다는 이유로 생략하지 않는다.
