# MockPack - Requirements & Architecture Plan

- 작성자: 최진호
- 작성일: 2026-02-27
- 버전: 2.0.0
- 기술 스택: Kotlin + Compose Desktop (JVM)

---

## 1. 프로젝트 개요

MockPack은 모바일 앱 패키지(APK/IPA) 메타데이터를 추출하고, 반대로 사용자가 입력한 메타데이터를 기반으로 테스트용 Mock APK/IPA를 빌드하는 데스크톱 도구이다.

### 1.1 핵심 기능

| 기능 | 설명 |
|------|------|
| 메타데이터 추출 (Extract) | APK 또는 IPA 파일을 입력받아 메타데이터를 파싱하여 표시 |
| Mock 빌드 (Build) | 사용자가 입력한 메타데이터로 유효한 구조의 Mock APK/IPA 생성 |

### 1.2 대상 사용자

- QA 엔지니어: MDM/EMM 정책 테스트, 앱 배포 파이프라인 검증
- 모바일 개발자: CI/CD 파이프라인 테스트, 서버 API 연동 테스트
- 보안 분석가: 앱 분석 도구 검증

### 1.3 배포 요구사항

- Windows / macOS 멀티플랫폼 지원
- 의존성 없는 단일 실행 파일 (사용자 PC에 JRE 설치 불필요)
- jpackage를 통한 네이티브 설치 패키지 생성 (Windows `.msi`, macOS `.dmg`)

---

## 2. 기능 상세 명세

### 2.1 메타데이터 추출 (Extract Mode)

#### Android (APK)

APK는 ZIP 아카이브이며, 내부의 `AndroidManifest.xml`(바이너리 XML 형식)에서 메타데이터를 추출한다.

| 추출 항목 | 소스 | 비고 |
|-----------|------|------|
| Package Name | `AndroidManifest.xml` → `manifest@package` | 필수 |
| Version Name | `AndroidManifest.xml` → `manifest@android:versionName` | 필수 |
| Version Code | `AndroidManifest.xml` → `manifest@android:versionCode` | 필수 |
| App Name | `resources.arsc` → `res/values/strings.xml@app_name` | APK 내 리소스 테이블 파싱 필요 |
| Min SDK Version | `AndroidManifest.xml` → `uses-sdk@android:minSdkVersion` | 필수 |
| Target SDK Version | `AndroidManifest.xml` → `uses-sdk@android:targetSdkVersion` | 필수 |
| Permissions | `AndroidManifest.xml` → `uses-permission` | 선택 (추후 확장) |
| 서명 정보 | `META-INF/*.RSA` / `*.SF` | 선택 (추후 확장) |

기술적 과제:
- AndroidManifest.xml은 바이너리 XML(AXML) 형식이므로 전용 파서가 필요하다. AOSP 소스가 Java이므로 직접 참조 가능.
- App Name은 리소스 테이블(`resources.arsc`)을 거쳐야 하는 경우가 많다. 직접 문자열이 아닌 리소스 참조(`@string/app_name`)일 수 있다.

#### iOS (IPA)

IPA는 ZIP 아카이브이며, 내부 `Payload/<AppName>.app/Info.plist`에서 메타데이터를 추출한다.

| 추출 항목 | 소스 키 | 비고 |
|-----------|---------|------|
| Bundle ID | `CFBundleIdentifier` | 필수 |
| Version Name | `CFBundleShortVersionString` | 필수 |
| Build Number | `CFBundleVersion` | 필수 |
| App Name | `CFBundleDisplayName` 또는 `CFBundleName` | 필수, fallback 로직 필요 |
| Minimum OS Version | `MinimumOSVersion` | 선택 |
| Supported Platforms | `CFBundleSupportedPlatforms` | 선택 |

기술적 과제:
- Info.plist는 바이너리 plist 형식일 수 있으므로 바이너리/XML 양쪽 모두 파싱 가능해야 한다.

### 2.2 Mock 빌드 (Build Mode)

#### Android Mock APK 빌드

사용자 입력값으로 유효한 APK 구조를 생성한다.

사용자 입력 필드:

| 필드 | 타입 | 필수 | 기본값 |
|------|------|------|--------|
| Package Name | String | Y | - |
| Version Name | String | Y | `1.0.0` |
| Version Code | Int | Y | `1` |
| App Name | String | Y | `MockApp` |
| Min SDK Version | Int | N | `21` |
| Target SDK Version | Int | N | `34` |

생성되는 APK 내부 구조:

```
mock-app.apk (ZIP)
├── AndroidManifest.xml    (바이너리 AXML 형식)
├── classes.dex            (최소 유효 DEX - no-op)
├── resources.arsc         (최소 리소스 테이블 - app_name 포함)
├── res/
│   └── (최소 리소스 디렉토리)
└── META-INF/
    ├── MANIFEST.MF
    ├── CERT.SF
    └── CERT.RSA            (debug 서명)
```

핵심 기술 요구사항:
- 바이너리 AXML 생성: AndroidManifest.xml을 AXML 바이너리로 인코딩.
- 최소 DEX 생성: `classes.dex`는 DEX 헤더와 최소 구조만 갖춘 유효한 파일.
- 리소스 테이블 생성: `resources.arsc`에 `app_name` 문자열 리소스 포함.
- APK 서명: v1 JAR 서명(debug keystore). Google 공식 `apksig` 라이브러리 활용.
- zipalign: 4바이트 정렬 (선택사항).

#### iOS Mock IPA 빌드

사용자 입력값으로 유효한 IPA 구조를 생성한다.

사용자 입력 필드:

| 필드 | 타입 | 필수 | 기본값 |
|------|------|------|--------|
| Bundle ID | String | Y | - |
| Version Name | String | Y | `1.0.0` |
| Build Number | String | Y | `1` |
| App Name | String | Y | `MockApp` |
| Minimum OS Version | String | N | `15.0` |

생성되는 IPA 내부 구조:

```
mock-app.ipa (ZIP)
└── Payload/
    └── MockApp.app/
        ├── Info.plist         (바이너리 plist 형식)
        ├── PkgInfo            ("APPL????")
        ├── MockApp            (최소 Mach-O 바이너리 stub)
        └── _CodeSignature/
            └── CodeResources  (빈 placeholder)
```

핵심 기술 요구사항:
- Info.plist 생성: 바이너리 plist 형식으로 인코딩. `dd-plist` 라이브러리 활용.
- PkgInfo: 8바이트 고정 문자열 `APPL????`
- 실행 바이너리 stub: 사전 빌드된 최소 Mach-O arm64 바이너리를 리소스로 내장.
- 코드서명: 실제 Apple 인증서 없이는 불가. `_CodeSignature`는 빈 placeholder.

---

## 3. 기술 스택

### 3.1 언어 & 프레임워크

| 구분 | 선택 | 근거 |
|------|------|------|
| 언어 | Kotlin (JVM) | null safety, coroutines, AOSP 생태계 직접 활용 |
| UI | Compose Desktop | JetBrains 공식, 선언적 UI, Windows/macOS 네이티브 배포 |
| 빌드 | Gradle (Kotlin DSL) | Compose Desktop 공식 빌드 시스템 |
| 배포 | jpackage (Gradle compose plugin) | JRE 번들링, 단일 설치 패키지 생성 |

### 3.2 핵심 라이브러리

| 라이브러리 | GroupId:ArtifactId | 용도 |
|-----------|-------------------|------|
| Compose Desktop | `org.jetbrains.compose` | UI 프레임워크 |
| Material 3 | `compose.material3` | Material Design 3 컴포넌트 |
| dd-plist | `com.googlecode.plist:dd-plist` | iOS 바이너리/XML plist 파싱 및 생성 |
| apksig | `com.android.tools.build:apksig` | Google 공식 APK 서명 (v1/v2/v3) |
| Bouncy Castle | `org.bouncycastle:bcprov-jdk18on` | 암호화, 키 생성 (APK 서명용) |
| Kotlinx Coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 비동기 파일 I/O |
| Kotlinx Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 메타데이터 JSON 직렬화 |
| JUnit 5 | `org.junit.jupiter:junit-jupiter` | 단위 테스트 |

### 3.3 Java/Kotlin 생태계 활용 이점

APK 바이너리 포맷이 Android/Java 생태계의 산물이므로 다음 이점이 있다:

- AOSP `AndroidManifest.xml` 바이너리 파서 로직을 Kotlin으로 직접 포팅/참조 가능
- `apksig`: Google이 직접 관리하는 APK 서명 라이브러리. v1/v2/v3 서명 모두 지원.
- `java.util.zip.ZipFile` / `ZipOutputStream`: JDK 내장 ZIP 처리. 외부 의존성 불필요.
- `java.nio.ByteBuffer`: 바이너리 데이터 읽기/쓰기에 최적화된 JDK 내장 API.
- DEX 파일 포맷도 Dalvik/ART 스펙이 Java 기반이므로 참조 자료가 풍부.

### 3.4 빌드 & 배포 설정

```kotlin
compose.desktop {
    application {
        mainClass = "com.mockpack.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)

            packageName    = "MockPack"
            packageVersion = "1.0.0"
            description    = "Mobile App Package Metadata Extractor & Mock Builder"
            vendor         = "MockPack"

            macOS {
                iconFile.set(project.file("icons/icon.icns"))
                bundleID = "com.mockpack.app"
            }

            windows {
                iconFile.set(project.file("icons/icon.ico"))
                menuGroup   = "MockPack"
                upgradeUuid = "unique-uuid-here"
            }
        }
    }
}
```

빌드 명령:

```bash
./gradlew packageDistributionForCurrentOS
```

산출물: `build/compose/binaries/main/` 하위에 `.dmg` (macOS) 또는 `.msi` (Windows) 생성.

---

## 4. 아키텍처 설계

### 4.1 디렉토리 구조

```
MockPack/
├── build.gradle.kts                        # 프로젝트 빌드 설정
├── settings.gradle.kts                     # 프로젝트명, 플러그인 관리
├── gradle.properties                       # Gradle/JVM 설정
├── gradlew / gradlew.bat                   # Gradle Wrapper
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── Requirements.md
├── README.md
│
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/mockpack/
│   │   │       │
│   │   │       ├── Main.kt                        # 애플리케이션 진입점
│   │   │       │
│   │   │       ├── core/                           # 핵심 도메인 모델 & 공통
│   │   │       │   ├── model/
│   │   │       │   │   ├── AppMetadata.kt          # sealed interface + data class
│   │   │       │   │   ├── AndroidMetadata.kt
│   │   │       │   │   └── IosMetadata.kt
│   │   │       │   ├── error/
│   │   │       │   │   └── MockPackException.kt    # 커스텀 예외 계층
│   │   │       │   └── constant/
│   │   │       │       └── Constants.kt            # 공통 상수
│   │   │       │
│   │   │       ├── extract/                        # 메타데이터 추출 모듈
│   │   │       │   ├── MetadataExtractor.kt        # 추출기 인터페이스
│   │   │       │   ├── ExtractorFactory.kt         # 파일 확장자 기반 분기
│   │   │       │   ├── ApkExtractor.kt
│   │   │       │   └── IpaExtractor.kt
│   │   │       │
│   │   │       ├── build/                          # Mock 빌드 모듈
│   │   │       │   ├── MockBuilder.kt              # 빌더 인터페이스
│   │   │       │   ├── BuilderFactory.kt           # 플랫폼 기반 분기
│   │   │       │   ├── ApkBuilder.kt
│   │   │       │   └── IpaBuilder.kt
│   │   │       │
│   │   │       ├── android/                        # Android 바이너리 처리
│   │   │       │   ├── axml/
│   │   │       │   │   ├── AXMLParser.kt           # 바이너리 XML 파서
│   │   │       │   │   ├── AXMLWriter.kt           # 바이너리 XML 생성기
│   │   │       │   │   └── AXMLConstants.kt        # AXML 청크 타입 상수
│   │   │       │   ├── resource/
│   │   │       │   │   ├── ResourceTableParser.kt  # resources.arsc 파서
│   │   │       │   │   └── ResourceTableWriter.kt  # resources.arsc 최소 생성기
│   │   │       │   ├── dex/
│   │   │       │   │   └── DexWriter.kt            # 최소 DEX 파일 생성기
│   │   │       │   └── sign/
│   │   │       │       └── ApkSigner.kt            # APK 서명 (apksig 래퍼)
│   │   │       │
│   │   │       ├── ios/                            # iOS 바이너리 처리
│   │   │       │   ├── plist/
│   │   │       │   │   └── PlistHandler.kt         # plist 파싱/생성 (dd-plist 래퍼)
│   │   │       │   └── stub/
│   │   │       │       └── MachOStub.kt            # 최소 Mach-O 바이너리 로더
│   │   │       │
│   │   │       ├── util/                           # 공통 유틸리티
│   │   │       │   ├── ZipUtils.kt                 # ZIP 읽기/쓰기 래퍼
│   │   │       │   ├── FileUtils.kt                # 파일 I/O 헬퍼
│   │   │       │   └── ValidationUtils.kt          # 입력값 검증
│   │   │       │
│   │   │       └── ui/                             # Compose Desktop UI
│   │   │           ├── App.kt                      # 루트 Composable
│   │   │           ├── theme/
│   │   │           │   ├── Theme.kt                # Material 3 테마 정의
│   │   │           │   ├── Color.kt                # 컬러 팔레트
│   │   │           │   └── Typography.kt           # 타이포그래피
│   │   │           ├── navigation/
│   │   │           │   └── NavigationHost.kt       # 화면 전환 관리
│   │   │           ├── screen/
│   │   │           │   ├── ExtractScreen.kt        # 추출 화면
│   │   │           │   └── BuildScreen.kt          # 빌드 화면
│   │   │           ├── component/
│   │   │           │   ├── FileDropZone.kt         # 파일 드래그앤드롭 영역
│   │   │           │   ├── MetadataTable.kt        # 메타데이터 표시 테이블
│   │   │           │   ├── MetadataForm.kt         # 메타데이터 입력 폼
│   │   │           │   └── PlatformSelector.kt     # Android/iOS 선택
│   │   │           └── viewmodel/
│   │   │               ├── ExtractViewModel.kt     # 추출 화면 상태 관리
│   │   │               └── BuildViewModel.kt       # 빌드 화면 상태 관리
│   │   │
│   │   └── resources/
│   │       ├── templates/
│   │       │   ├── minimal.dex                     # 최소 DEX 바이너리 (사전 빌드)
│   │       │   └── minimal-arm64                   # 최소 Mach-O arm64 stub (사전 빌드)
│   │       └── icons/
│   │           ├── icon.icns                       # macOS 앱 아이콘
│   │           └── icon.ico                        # Windows 앱 아이콘
│   │
│   └── test/
│       └── kotlin/
│           └── com/mockpack/
│               ├── extract/
│               │   ├── ApkExtractorTest.kt
│               │   └── IpaExtractorTest.kt
│               ├── build/
│               │   ├── ApkBuilderTest.kt
│               │   └── IpaBuilderTest.kt
│               ├── android/
│               │   ├── axml/
│               │   │   ├── AXMLParserTest.kt
│               │   │   └── AXMLWriterTest.kt
│               │   └── dex/
│               │       └── DexWriterTest.kt
│               └── ios/
│                   └── plist/
│                       └── PlistHandlerTest.kt
```

### 4.2 핵심 인터페이스 설계

```kotlin
/** 공통 앱 메타데이터 */
sealed interface AppMetadata {
    val packageId:   String
    val versionName: String
    val buildNumber: String
    val appName:     String
}

/** Android 전용 메타데이터 */
data class AndroidMetadata(
    override val packageId:   String,
    override val versionName: String,
    override val buildNumber: String,
    override val appName:     String,
    val minSdkVersion:        Int,
    val targetSdkVersion:     Int,
    val permissions:          List<String> = emptyList()
) : AppMetadata

/** iOS 전용 메타데이터 */
data class IosMetadata(
    override val packageId:   String,
    override val versionName: String,
    override val buildNumber: String,
    override val appName:     String,
    val minimumOSVersion:     String = "15.0",
    val supportedPlatforms:   List<String> = listOf("iPhoneOS")
) : AppMetadata

/** 추출기 인터페이스 */
interface MetadataExtractor<T : AppMetadata> {
    suspend fun extract(filePath: Path): T
}

/** 빌더 인터페이스 */
interface MockBuilder<T : AppMetadata> {
    suspend fun build(metadata: T, outputPath: Path): Path
}
```

### 4.3 데이터 흐름

```
[Extract Mode]
  파일 드래그앤드롭 → ExtractViewModel → ExtractorFactory.create(extension)
    → APK: ApkExtractor → AXMLParser + ResourceTableParser → AndroidMetadata
    → IPA: IpaExtractor → PlistHandler → IosMetadata
  → MetadataTable에 결과 표시

[Build Mode]
  MetadataForm 입력 → BuildViewModel → ValidationUtils.validate()
    → APK: ApkBuilder → AXMLWriter + DexWriter + ResourceTableWriter + ApkSigner → ZipUtils
    → IPA: IpaBuilder → PlistHandler + MachOStub → ZipUtils
  → 파일 저장 다이얼로그 → .apk / .ipa 출력
```

### 4.4 UI 화면 구성

```
┌─────────────────────────────────────────────────┐
│  MockPack                              [─] [□] [×] │
├────────────┬────────────────────────────────────┤
│            │                                     │
│  Extract   │   [파일 드래그앤드롭 영역]            │
│            │   ┌───────────────────────────┐     │
│  Build     │   │                           │     │
│            │   │   APK 또는 IPA 파일을      │     │
│            │   │   여기에 드롭하세요         │     │
│            │   │                           │     │
│            │   └───────────────────────────┘     │
│            │                                     │
│            │   ┌─ 추출 결과 ──────────────────┐  │
│            │   │ Package Name  com.example.app│  │
│            │   │ Version Name  1.0.0          │  │
│            │   │ Version Code  1              │  │
│            │   │ App Name      MyApp          │  │
│            │   │ Min SDK       21             │  │
│            │   │ Target SDK    33             │  │
│            │   └──────────────────────────────┘  │
│            │                     [JSON 내보내기]  │
├────────────┴────────────────────────────────────┤
│                                                  │
└──────────────────────────────────────────────────┘
```

---

## 5. 기술적 난이도 분석

### 5.1 난이도 매트릭스

| 컴포넌트 | 난이도 | 설명 |
|----------|--------|------|
| ZIP 처리 | 낮음 | `java.util.zip` JDK 내장 |
| iOS plist 파싱/생성 | 낮음 | `dd-plist` 라이브러리로 해결 |
| IPA 구조 생성 | 낮음 | 디렉토리 구조 + plist 생성 |
| Android 바이너리 XML 파싱 | 중상 | AXML 바이너리 포맷. AOSP 소스 참조 가능하여 난이도 하향 |
| Android 바이너리 XML 생성 | 중상 | StringPool, ResourceMap 등. 최소 구조만 생성하면 단순화 |
| resources.arsc 파싱 | 중상 | 복잡한 테이블 구조. 단, app_name만 추출하면 단순화 가능 |
| resources.arsc 생성 | 중간 | 최소 테이블(app_name만)이면 단순화 가능 |
| 최소 DEX 생성 | 중간 | DEX 헤더 + 빈 클래스, Adler32/SHA1 계산 |
| APK 서명 | 낮음 | `apksig` (Google 공식) 사용으로 난이도 대폭 하향 |
| Mach-O stub | 낮음 | 사전 빌드된 바이너리를 리소스로 내장 |
| Compose Desktop UI | 중간 | 파일 드래그앤드롭, 폼, 테이블 등 |
| jpackage 배포 | 낮음 | Gradle compose 플러그인이 자동 처리 |

### 5.2 리스크 & 완화 전략

| 리스크 | 영향 | 완화 전략 |
|--------|------|-----------|
| AXML 파서 구현 복잡도 | 중상 | AOSP `AXMLParser.java` 직접 참조/포팅. apktool 소스 참고. |
| Mock APK가 특정 도구에서 인식 안됨 | 중간 | APK Analyzer(Android Studio) 기준 유효성 검증 |
| apksig 버전 호환성 | 낮음 | Maven Central 최신 안정 버전 고정 |
| jpackage 결과물 크기 (50-100MB) | 중간 | 수용 가능. jlink로 모듈 최소화 적용 가능. |
| Compose Desktop 파일 드래그앤드롭 | 낮음 | AWT `DropTarget` + Compose 통합으로 해결 |

---

## 6. 구현 로드맵

### Phase 1: 프로젝트 셋업 & 핵심 로직

목표: Gradle 프로젝트 구성, 핵심 추출/빌드 로직 완성 (UI 없이 단위 테스트로 검증)

| 단계 | 작업 | 산출물 | 검증 기준 |
|------|------|--------|-----------|
| 1-1 | Gradle 프로젝트 초기 설정 | build.gradle.kts, 디렉토리 구조 | `./gradlew build` 성공 |
| 1-2 | 도메인 모델 정의 | AppMetadata, AndroidMetadata, IosMetadata | 컴파일 성공 |
| 1-3 | ZIP/파일 유틸리티 구현 | ZipUtils, FileUtils | 단위 테스트 통과 |
| 1-4 | iOS plist 처리 | PlistHandler (dd-plist 래퍼) | 바이너리/XML plist 양방향 변환 테스트 |
| 1-5 | IPA Extractor | IpaExtractor | 실제 IPA에서 메타데이터 추출 |
| 1-6 | IPA Builder | IpaBuilder | build → extract 라운드트립 일치 |
| 1-7 | AXML 파서 | AXMLParser | 실제 APK AndroidManifest.xml 파싱 |
| 1-8 | AXML 생성기 | AXMLWriter | 생성 → 파싱 라운드트립 검증 |
| 1-9 | resources.arsc 처리 | ResourceTableParser, ResourceTableWriter | app_name 추출/삽입 |
| 1-10 | 최소 DEX 생성 | DexWriter | dexdump 유효성 확인 |
| 1-11 | APK 서명 | ApkSigner (apksig 래퍼) | apksigner verify 통과 |
| 1-12 | APK Extractor | ApkExtractor | 실제 APK 메타데이터 추출 |
| 1-13 | APK Builder | ApkBuilder | build → extract 라운드트립 일치 |
| 1-14 | 통합 테스트 | E2E 테스트 | 전체 라운드트립 검증 |

### Phase 2: Compose Desktop GUI

목표: 드래그앤드롭 기반 데스크톱 앱 완성

| 단계 | 작업 | 검증 기준 |
|------|------|-----------|
| 2-1 | Compose Desktop 셋업, 테마/컬러 정의 | 빈 윈도우 실행 |
| 2-2 | 네비게이션 구조 (Extract / Build 탭) | 탭 전환 동작 |
| 2-3 | FileDropZone 컴포넌트 | 파일 드래그앤드롭 인식 |
| 2-4 | Extract 화면 완성 | 파일 드롭 → 메타데이터 테이블 표시 |
| 2-5 | Build 화면 - 메타데이터 폼 | 입력값 검증, 기본값 표시 |
| 2-6 | Build 화면 - 파일 생성 & 저장 | 파일 저장 다이얼로그 → APK/IPA 출력 |
| 2-7 | JSON 내보내기 기능 | 추출 결과 JSON 파일 저장 |
| 2-8 | jpackage 배포 설정 | `.dmg` (macOS), `.msi` (Windows) 생성 |

### Phase 3: 확장 기능

| 기능 | 설명 |
|------|------|
| APK v2/v3 서명 | apksig가 이미 지원. 옵션으로 활성화. |
| 권한 커스터마이징 | Mock APK에 임의 퍼미션 추가 |
| 아이콘 삽입 | 사용자 제공 아이콘을 Mock 앱에 포함 |
| 프로비저닝 프로파일 | IPA에 mock provisioning profile 포함 |
| 배치 빌드 | JSON 파일로 다수 앱 메타데이터를 입력받아 일괄 빌드 |
| 다크 모드 | Compose Material 3 다크 테마 |

---

## 7. 성공 기준

| 기준 | 검증 방법 |
|------|-----------|
| APK 메타데이터 추출 정확성 | aapt2 dump 결과와 비교 |
| IPA 메타데이터 추출 정확성 | plutil 결과와 비교 |
| Mock APK 구조 유효성 | Android Studio APK Analyzer로 열림 확인 |
| Mock IPA 구조 유효성 | unzip 후 Info.plist 정상 파싱 확인 |
| 라운드트립 일관성 | build → extract 결과가 입력 메타데이터와 100% 일치 |
| 배포 패키지 독립성 | JRE 미설치 PC에서 설치 및 실행 확인 |
| 크로스 플랫폼 | Windows 10+, macOS 12+ 에서 정상 동작 확인 |

---

## 8. 제약 사항 & 면책

- Mock APK/IPA는 테스트 목적 전용이다. 실제 디바이스에서의 설치/실행을 보장하지 않는다.
- APK 서명은 debug 키로만 수행한다. Google Play 업로드용이 아니다.
- IPA는 Apple 코드서명이 없으므로 실제 iOS 기기 설치는 불가하다.
- 악용 방지: 이 도구는 앱 위변조가 아닌, 메타데이터 레벨의 테스트 패키지 생성이 목적이다.
- jpackage 배포 시 앱 크기는 50~100MB 수준이다 (번들 JRE 포함).
