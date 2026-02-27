package com.mockpack.core.model

import kotlinx.serialization.Serializable

/**
 * 모바일 앱 패키지의 공통 메타데이터를 정의하는 sealed interface.
 * Android(APK)와 iOS(IPA) 모두가 공유하는 필드를 선언한다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
sealed interface AppMetadata {
    /** 패키지 식별자 (Android: Package Name, iOS: Bundle ID) */
    val packageId: String

    /** 사용자에게 표시되는 버전 문자열 */
    val versionName: String

    /** 내부 빌드 번호 (Android: Version Code, iOS: CFBundleVersion) */
    val buildNumber: String

    /** 앱 표시 이름 */
    val appName: String
}

/**
 * Android APK 메타데이터.
 *
 * @param packageId        패키지명 (e.g. com.example.app)
 * @param versionName      버전명 (e.g. 1.0.0)
 * @param buildNumber      버전 코드 (e.g. 1)
 * @param appName          앱 이름 (e.g. MyApp)
 * @param minSdkVersion    최소 SDK 버전 (e.g. 21)
 * @param targetSdkVersion 타겟 SDK 버전 (e.g. 34)
 * @param permissions      요청 퍼미션 목록
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Serializable
data class AndroidMetadata(
    override val packageId:   String,
    override val versionName: String,
    override val buildNumber: String,
    override val appName:     String,
    val minSdkVersion:        Int              = 21,
    val targetSdkVersion:     Int              = 34,
    val permissions:          List<String>     = emptyList()
) : AppMetadata

/**
 * iOS IPA 메타데이터.
 *
 * @param packageId          Bundle ID (e.g. com.example.app)
 * @param versionName        CFBundleShortVersionString (e.g. 1.0.0)
 * @param buildNumber        CFBundleVersion (e.g. 123)
 * @param appName            CFBundleDisplayName (e.g. MyApp)
 * @param minimumOSVersion   최소 iOS 버전 (e.g. 15.0)
 * @param supportedPlatforms 지원 플랫폼 목록
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Serializable
data class IosMetadata(
    override val packageId:   String,
    override val versionName: String,
    override val buildNumber: String,
    override val appName:     String,
    val minimumOSVersion:     String       = "15.0",
    val supportedPlatforms:   List<String> = listOf("iPhoneOS")
) : AppMetadata
