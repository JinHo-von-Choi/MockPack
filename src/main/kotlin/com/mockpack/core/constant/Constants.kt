package com.mockpack.core.constant

/**
 * MockPack 애플리케이션 전역 상수 정의.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object Constants {

    /** APK 파일 확장자 */
    const val APK_EXTENSION = ".apk"

    /** IPA 파일 확장자 */
    const val IPA_EXTENSION = ".ipa"

    /** APK 내 AndroidManifest.xml 엔트리 경로 */
    const val ANDROID_MANIFEST_ENTRY = "AndroidManifest.xml"

    /** APK 내 리소스 테이블 엔트리 경로 */
    const val RESOURCES_ARSC_ENTRY = "resources.arsc"

    /** APK 내 DEX 파일 엔트리 경로 */
    const val CLASSES_DEX_ENTRY = "classes.dex"

    /** IPA 내 Payload 디렉토리 접두사 */
    const val IPA_PAYLOAD_PREFIX = "Payload/"

    /** iOS Info.plist 파일명 */
    const val INFO_PLIST = "Info.plist"

    /** iOS PkgInfo 파일 내용 (8바이트 고정) */
    const val PKG_INFO_CONTENT = "APPL????"

    /** Android 기본값 */
    object AndroidDefaults {
        const val MIN_SDK_VERSION    = 21
        const val TARGET_SDK_VERSION = 34
        const val VERSION_NAME       = "1.0.0"
        const val VERSION_CODE       = "1"
        const val APP_NAME           = "MockApp"
    }

    /** iOS 기본값 */
    object IosDefaults {
        const val MINIMUM_OS_VERSION = "15.0"
        const val VERSION_NAME       = "1.0.0"
        const val BUILD_NUMBER       = "1"
        const val APP_NAME           = "MockApp"
    }
}
