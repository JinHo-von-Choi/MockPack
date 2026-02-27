package com.mockpack.core.error

/**
 * MockPack 애플리케이션 전용 예외 계층의 최상위 클래스.
 * 모든 도메인 예외는 이 클래스를 상속한다.
 *
 * @param message 예외 메시지
 * @param cause   원인 예외
 *
 * @author 최진호
 * @since 2026-02-27
 */
sealed class MockPackException(
    message: String,
    cause:   Throwable? = null
) : RuntimeException(message, cause)

/**
 * 파일 포맷이 올바르지 않을 때 발생하는 예외.
 * APK/IPA 파일 구조가 손상되었거나 필수 엔트리가 누락된 경우.
 *
 * @param message 예외 메시지
 * @param cause   원인 예외
 *
 * @author 최진호
 * @since 2026-02-27
 */
class InvalidFileFormatException(
    message: String,
    cause:   Throwable? = null
) : MockPackException(message, cause)

/**
 * 메타데이터 추출 과정에서 발생하는 예외.
 * 바이너리 XML 파싱 실패, plist 읽기 실패 등.
 *
 * @param message 예외 메시지
 * @param cause   원인 예외
 *
 * @author 최진호
 * @since 2026-02-27
 */
class ExtractionException(
    message: String,
    cause:   Throwable? = null
) : MockPackException(message, cause)

/**
 * Mock 패키지 빌드 과정에서 발생하는 예외.
 * 바이너리 생성 실패, 서명 실패 등.
 *
 * @param message 예외 메시지
 * @param cause   원인 예외
 *
 * @author 최진호
 * @since 2026-02-27
 */
class BuildException(
    message: String,
    cause:   Throwable? = null
) : MockPackException(message, cause)

/**
 * 사용자 입력값 검증 실패 시 발생하는 예외.
 *
 * @param field   검증 실패한 필드명
 * @param message 예외 메시지
 *
 * @author 최진호
 * @since 2026-02-27
 */
class ValidationException(
    val field: String,
    message:   String
) : MockPackException("[$field] $message")
