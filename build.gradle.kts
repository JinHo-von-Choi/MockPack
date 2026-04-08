import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
}

group   = "com.mockpack"
version = "1.0.1"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    /** iOS plist 파싱/생성 */
    implementation("com.googlecode.plist:dd-plist:1.28")

    /** APK 서명 (Google 공식) */
    implementation("com.android.tools.build:apksig:8.7.3")

    /** 암호화 / 키 생성 */
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.79")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")

    /** 외부 APK 파서 호환성 테스트용 */
    testImplementation("net.dongliu:apk-parser:2.6.10")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "com.mockpack.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi)

            packageName    = "MockPack"
            packageVersion = "1.0.1"
            description    = "Mobile App Package Metadata Extractor & Mock Builder"
            vendor         = "MockPack"

            macOS {
                bundleID = "com.mockpack.app"
            }

            windows {
                menuGroup   = "MockPack"
                upgradeUuid = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
            }
        }
    }
}
