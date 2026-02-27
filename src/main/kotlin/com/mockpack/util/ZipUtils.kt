package com.mockpack.util

import com.mockpack.core.error.InvalidFileFormatException
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * ZIP 아카이브 읽기/쓰기 유틸리티.
 * APK와 IPA 모두 ZIP 기반이므로 공통으로 사용한다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object ZipUtils {

    /**
     * ZIP 파일에서 특정 엔트리의 바이트 데이터를 읽는다.
     *
     * @param zipPath   ZIP 파일 경로
     * @param entryName 읽을 엔트리 이름 (e.g. "AndroidManifest.xml")
     * @returns 엔트리의 바이트 배열
     * @throws InvalidFileFormatException ZIP 파일이 아니거나 엔트리가 없는 경우
     */
    fun readEntry(zipPath: Path, entryName: String): ByteArray {
        try {
            ZipFile(zipPath.toFile()).use { zip ->
                val entry = zip.getEntry(entryName)
                    ?: throw InvalidFileFormatException("ZIP 엔트리를 찾을 수 없음: $entryName")

                zip.getInputStream(entry).use { stream ->
                    return stream.readBytes()
                }
            }
        } catch (e: InvalidFileFormatException) {
            throw e
        } catch (e: Exception) {
            throw InvalidFileFormatException("ZIP 파일 읽기 실패: ${zipPath.fileName}", e)
        }
    }

    /**
     * ZIP 파일에서 특정 접두사로 시작하는 엔트리 이름 목록을 반환한다.
     *
     * @param zipPath ZIP 파일 경로
     * @param prefix  검색할 접두사 (e.g. "Payload/")
     * @returns 매칭된 엔트리 이름 목록
     */
    fun listEntries(zipPath: Path, prefix: String = ""): List<String> {
        try {
            ZipFile(zipPath.toFile()).use { zip ->
                return zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith(prefix) }
                    .toList()
            }
        } catch (e: Exception) {
            throw InvalidFileFormatException("ZIP 파일 읽기 실패: ${zipPath.fileName}", e)
        }
    }

    /**
     * ZIP 파일에 특정 엔트리가 존재하는지 확인한다.
     *
     * @param zipPath   ZIP 파일 경로
     * @param entryName 확인할 엔트리 이름
     * @returns 엔트리 존재 여부
     */
    fun hasEntry(zipPath: Path, entryName: String): Boolean {
        try {
            ZipFile(zipPath.toFile()).use { zip ->
                return zip.getEntry(entryName) != null
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 여러 엔트리를 포함하는 ZIP 파일을 생성한다.
     *
     * @param outputPath 출력 ZIP 파일 경로
     * @param entries    엔트리명과 데이터의 Map (entryName -> byteArray)
     * @returns 생성된 파일 경로
     */
    fun createZip(outputPath: Path, entries: Map<String, ByteArray>): Path {
        Files.newOutputStream(outputPath).use { fos ->
            ZipOutputStream(fos).use { zos ->
                for ((name, data) in entries) {
                    val entry = ZipEntry(name)
                    zos.putNextEntry(entry)
                    zos.write(data)
                    zos.closeEntry()
                }
            }
        }
        return outputPath
    }

    /**
     * ZIP 파일에서 특정 접두사 패턴과 매칭되는 첫 번째 엔트리를 찾아 바이트로 읽는다.
     * IPA에서 Payload/XXX.app/Info.plist 경로를 동적으로 찾을 때 사용.
     *
     * @param zipPath ZIP 파일 경로
     * @param pattern 엔트리 이름이 만족해야 하는 조건
     * @returns 매칭된 엔트리의 (이름, 바이트) Pair. 없으면 null.
     */
    fun findAndReadEntry(zipPath: Path, pattern: (String) -> Boolean): Pair<String, ByteArray>? {
        try {
            ZipFile(zipPath.toFile()).use { zip ->
                val matchedEntry = zip.entries().asSequence()
                    .firstOrNull { pattern(it.name) }
                    ?: return null

                val data = zip.getInputStream(matchedEntry).use { it.readBytes() }
                return matchedEntry.name to data
            }
        } catch (e: Exception) {
            throw InvalidFileFormatException("ZIP 파일 읽기 실패: ${zipPath.fileName}", e)
        }
    }
}
