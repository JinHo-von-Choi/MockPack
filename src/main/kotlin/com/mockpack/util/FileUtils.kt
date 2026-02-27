package com.mockpack.util

import com.mockpack.core.constant.Constants
import com.mockpack.core.error.InvalidFileFormatException
import java.nio.file.Files
import java.nio.file.Path

/**
 * 파일 I/O 관련 공통 유틸리티.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object FileUtils {

    /**
     * 파일이 존재하고 읽기 가능한지 검증한다.
     *
     * @param path 검증할 파일 경로
     * @throws InvalidFileFormatException 파일이 존재하지 않거나 읽을 수 없는 경우
     */
    fun validateFileExists(path: Path) {
        if (!Files.exists(path)) {
            throw InvalidFileFormatException("파일이 존재하지 않음: $path")
        }
        if (!Files.isReadable(path)) {
            throw InvalidFileFormatException("파일을 읽을 수 없음: $path")
        }
        if (Files.isDirectory(path)) {
            throw InvalidFileFormatException("디렉토리는 지원하지 않음: $path")
        }
    }

    /**
     * 파일 확장자를 기반으로 지원되는 패키지 형식인지 확인한다.
     *
     * @param path 확인할 파일 경로
     * @returns true: APK 또는 IPA 파일
     */
    fun isSupportedFormat(path: Path): Boolean {
        val name = path.fileName.toString().lowercase()
        return name.endsWith(Constants.APK_EXTENSION) || name.endsWith(Constants.IPA_EXTENSION)
    }

    /**
     * 파일이 APK인지 확인한다.
     *
     * @param path 확인할 파일 경로
     * @returns true: APK 파일
     */
    fun isApk(path: Path): Boolean {
        return path.fileName.toString().lowercase().endsWith(Constants.APK_EXTENSION)
    }

    /**
     * 파일이 IPA인지 확인한다.
     *
     * @param path 확인할 파일 경로
     * @returns true: IPA 파일
     */
    fun isIpa(path: Path): Boolean {
        return path.fileName.toString().lowercase().endsWith(Constants.IPA_EXTENSION)
    }

    /**
     * 출력 경로의 부모 디렉토리가 존재하지 않으면 생성한다.
     *
     * @param path 출력 파일 경로
     */
    fun ensureParentDirectoryExists(path: Path) {
        val parent = path.parent ?: return
        if (!Files.exists(parent)) {
            Files.createDirectories(parent)
        }
    }
}
