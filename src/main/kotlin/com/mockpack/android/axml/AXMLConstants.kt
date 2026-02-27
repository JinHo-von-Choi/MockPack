package com.mockpack.android.axml

/**
 * Android 바이너리 XML (AXML) 포맷 상수.
 * AOSP의 ResourceTypes.h 기반.
 *
 * AXML 파일 구조:
 * ┌─────────────────────────┐
 * │ ResChunk_header (XML)   │  type=0x0003
 * ├─────────────────────────┤
 * │ StringPool chunk        │  type=0x0001
 * ├─────────────────────────┤
 * │ ResourceId Map chunk    │  type=0x0180
 * ├─────────────────────────┤
 * │ StartNamespace chunk    │  type=0x0100
 * ├─────────────────────────┤
 * │ StartElement chunks ... │  type=0x0102
 * │ EndElement chunks ...   │  type=0x0103
 * ├─────────────────────────┤
 * │ EndNamespace chunk      │  type=0x0101
 * └─────────────────────────┘
 *
 * @author 최진호
 * @since 2026-02-27
 */
object AXMLConstants {

    /** 청크 타입: String Pool */
    const val CHUNK_TYPE_STRING_POOL: Int     = 0x0001

    /** 청크 타입: XML Document */
    const val CHUNK_TYPE_XML: Int             = 0x0003

    /** 청크 타입: Start Namespace */
    const val CHUNK_TYPE_START_NAMESPACE: Int = 0x0100

    /** 청크 타입: End Namespace */
    const val CHUNK_TYPE_END_NAMESPACE: Int   = 0x0101

    /** 청크 타입: Start Element */
    const val CHUNK_TYPE_START_ELEMENT: Int   = 0x0102

    /** 청크 타입: End Element */
    const val CHUNK_TYPE_END_ELEMENT: Int     = 0x0103

    /** 청크 타입: Resource ID Map */
    const val CHUNK_TYPE_RESOURCE_MAP: Int    = 0x0180

    /** 속성 값 타입: null */
    const val TYPE_NULL: Int         = 0x00

    /** 속성 값 타입: 리소스 참조 (e.g. @string/app_name) */
    const val TYPE_REFERENCE: Int    = 0x01

    /** 속성 값 타입: 문자열 */
    const val TYPE_STRING: Int       = 0x03

    /** 속성 값 타입: 정수 (10진) */
    const val TYPE_INT_DEC: Int      = 0x10

    /** 속성 값 타입: 정수 (16진) */
    const val TYPE_INT_HEX: Int      = 0x11

    /** 속성 값 타입: 부울 */
    const val TYPE_INT_BOOLEAN: Int  = 0x12

    /** android 네임스페이스 URI */
    const val ANDROID_NS_URI = "http://schemas.android.com/apk/res/android"

    /** android 네임스페이스 접두사 */
    const val ANDROID_NS_PREFIX = "android"

    /** String Pool 플래그: UTF-8 인코딩 */
    const val STRING_POOL_FLAG_UTF8: Int = 0x00000100

    /** String Pool 플래그: 정렬됨 */
    const val STRING_POOL_FLAG_SORTED: Int = 0x00000001

    /** android: 접두사 속성의 리소스 ID 매핑 */
    object ResourceIds {
        const val ATTR_PACKAGE           = 0x0101021B
        const val ATTR_VERSION_CODE      = 0x0101021C
        const val ATTR_VERSION_NAME      = 0x0101021D
        const val ATTR_MIN_SDK_VERSION   = 0x0101020C
        const val ATTR_TARGET_SDK_VERSION = 0x01010270
        const val ATTR_NAME              = 0x01010003
        const val ATTR_LABEL             = 0x01010001
    }
}
