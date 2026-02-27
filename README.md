# MockPack

모바일 앱 패키지(APK/IPA)의 메타데이터를 추출하거나, 원하는 메타데이터로 Mock 패키지를 생성하는 데스크톱 도구.

QA 및 테스트 업무에서 다양한 메타데이터 조합의 APK/IPA 파일이 필요할 때, 실제 앱을 빌드하지 않고도 즉시 Mock 패키지를 만들 수 있도록 제작했다.


## 주요 기능

### 메타데이터 추출 (Extract)

APK 또는 IPA 파일을 드래그 앤 드롭하면 다음 메타데이터를 추출한다.

| 항목 | Android (APK) | iOS (IPA) |
|------|---------------|-----------|
| 패키지 식별자 | Package Name | Bundle ID |
| 버전명 | Version Name | Version (Short) |
| 빌드 번호 | Version Code | Build Number |
| 앱 이름 | App Name (Label) | App Name (Display) |
| SDK 버전 | Min / Target SDK | - |

추출 결과는 JSON으로 내보낼 수 있다.

### Mock 패키지 빌드 (Build)

플랫폼(Android/iOS)을 선택하고 메타데이터를 입력하면 해당 값이 반영된 Mock 패키지 파일을 생성한다.

- APK: AndroidManifest.xml(바이너리 AXML), classes.dex, resources.arsc 포함, v1 서명 적용
- IPA: Info.plist, PkgInfo, Mach-O stub 바이너리 포함


## 빌드 환경

- JDK 21 이상
- Gradle 9.x (Wrapper 포함)

### macOS (.dmg)

```bash
./gradlew packageDistributionForCurrentOS
```

결과물: `build/compose/binaries/main/dmg/MockPack-1.0.0.dmg`

### Windows (.msi)

```bash
./gradlew packageDistributionForCurrentOS
```

결과물: `build/compose/binaries/main/msi/MockPack-1.0.0.msi`

Windows 빌드 시 [WiX Toolset v3](https://wixtoolset.org/)가 PATH에 필요하다.

### 개발 모드 실행

```bash
./gradlew run
```


## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Kotlin 2.0 |
| UI | Compose Desktop (Material 3) |
| 빌드 | Gradle Kotlin DSL |
| APK 서명 | apksig (Google 공식) |
| iOS plist | dd-plist |
| 암호화 | Bouncy Castle |
| 배포 | jpackage (DMG / MSI) |


## 프로젝트 구조

```
src/main/kotlin/com/mockpack/
  android/          APK 바이너리 처리 (AXML, DEX, resources.arsc, 서명)
  ios/              IPA 처리 (plist, Mach-O stub)
  build/            Mock 패키지 빌더
  extract/          메타데이터 추출기
  core/             모델, 상수, 예외
  util/             ZIP, 파일, 유효성 검사 유틸리티
  ui/               Compose Desktop UI (화면, 컴포넌트, 테마, ViewModel)
```


## 라이선스

[MIT License](LICENSE)


<p align="center">
  Made by <a href="mailto:jinho.von.choi@nerdvana.kr">Jinho Choi</a> &nbsp;|&nbsp;
  <a href="https://buymeacoffee.com/jinho.von.choi">Buy me a coffee</a>
</p>
