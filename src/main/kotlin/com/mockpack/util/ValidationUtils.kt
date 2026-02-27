package com.mockpack.util

import com.mockpack.core.error.ValidationException
import com.mockpack.core.model.AndroidMetadata
import com.mockpack.core.model.AppMetadata
import com.mockpack.core.model.IosMetadata

/**
 * 메타데이터 입력값 검증 유틸리티.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object ValidationUtils {

    /** 패키지명/Bundle ID 유효성 패턴 (역 도메인 표기법) */
    private val PACKAGE_ID_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

    /** 버전명 유효성 패턴 (semver 기본 형식) */
    private val VERSION_NAME_PATTERN = Regex("^\\d+(\\.\\d+)*([-.+][a-zA-Z0-9]+)*$")

    /**
     * AppMetadata의 유효성을 검증한다.
     * 타입에 따라 Android/iOS 전용 검증도 수행한다.
     *
     * @param metadata 검증할 메타데이터
     * @throws ValidationException 검증 실패 시
     */
    fun validate(metadata: AppMetadata) {
        validatePackageId(metadata.packageId)
        validateVersionName(metadata.versionName)
        validateNotBlank("appName", metadata.appName)
        validateNotBlank("buildNumber", metadata.buildNumber)

        when (metadata) {
            is AndroidMetadata -> validateAndroid(metadata)
            is IosMetadata     -> validateIos(metadata)
        }
    }

    private fun validateAndroid(metadata: AndroidMetadata) {
        if (metadata.minSdkVersion < 1) {
            throw ValidationException("minSdkVersion", "1 이상이어야 합니다: ${metadata.minSdkVersion}")
        }
        if (metadata.targetSdkVersion < metadata.minSdkVersion) {
            throw ValidationException(
                "targetSdkVersion",
                "minSdkVersion(${metadata.minSdkVersion}) 이상이어야 합니다: ${metadata.targetSdkVersion}"
            )
        }
        val versionCode = metadata.buildNumber.toIntOrNull()
        if (versionCode == null || versionCode < 1) {
            throw ValidationException("buildNumber", "양의 정수여야 합니다: ${metadata.buildNumber}")
        }
    }

    private fun validateIos(metadata: IosMetadata) {
        if (metadata.minimumOSVersion.isBlank()) {
            throw ValidationException("minimumOSVersion", "비어 있을 수 없습니다")
        }
    }

    private fun validatePackageId(packageId: String) {
        if (packageId.isBlank()) {
            throw ValidationException("packageId", "비어 있을 수 없습니다")
        }
        if (!PACKAGE_ID_PATTERN.matches(packageId)) {
            throw ValidationException(
                "packageId",
                "역 도메인 표기법이어야 합니다 (e.g. com.example.app): $packageId"
            )
        }
    }

    private fun validateVersionName(versionName: String) {
        if (versionName.isBlank()) {
            throw ValidationException("versionName", "비어 있을 수 없습니다")
        }
        if (!VERSION_NAME_PATTERN.matches(versionName)) {
            throw ValidationException(
                "versionName",
                "유효한 버전 형식이어야 합니다 (e.g. 1.0.0): $versionName"
            )
        }
    }

    private fun validateNotBlank(field: String, value: String) {
        if (value.isBlank()) {
            throw ValidationException(field, "비어 있을 수 없습니다")
        }
    }
}
