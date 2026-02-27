package com.mockpack.android.resource

import com.mockpack.core.error.ExtractionException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android resources.arsc에서 app_name 문자열 리소스를 추출하는 파서.
 * 전체 리소스 테이블이 아닌 app_name 값만 필요하므로,
 * Global String Pool에서 문자열을 읽고 리소스 참조를 해석한다.
 *
 * resources.arsc 구조:
 * - Table Header (type=0x0002)
 * - Global String Pool
 * - Package chunks (각각 Type String Pool, Key String Pool, Type Spec, Type chunks 포함)
 *
 * @author 최진호
 * @since 2026-02-27
 */
object ResourceTableParser {

    private const val CHUNK_TYPE_TABLE: Int        = 0x0002
    private const val CHUNK_TYPE_STRING_POOL: Int  = 0x0001
    private const val CHUNK_TYPE_PACKAGE: Int      = 0x0200
    private const val CHUNK_TYPE_TYPE_SPEC: Int    = 0x0202
    private const val CHUNK_TYPE_TYPE: Int         = 0x0201

    private const val STRING_POOL_FLAG_UTF8: Int   = 0x00000100

    /**
     * resources.arsc에서 리소스 참조 ID에 해당하는 문자열 값을 찾는다.
     * 주로 app_name(@string/app_name)의 실제 문자열을 가져오는 데 사용.
     *
     * @param arscData resources.arsc 바이트 배열
     * @param resourceId 찾을 리소스 ID (e.g. 0x7F040001)
     * @returns 해당 리소스의 문자열 값. 없으면 null.
     */
    fun findStringByResourceId(arscData: ByteArray, resourceId: Int): String? {
        try {
            val buf = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)

            val tableType = buf.getShort().toInt() and 0xFFFF
            if (tableType != CHUNK_TYPE_TABLE) return null
            val tableHeaderSize = buf.getShort().toInt() and 0xFFFF
            val tableSize       = buf.getInt()
            val packageCount    = buf.getInt()

            val globalStringPool = parseStringPool(buf)

            val targetPackageId = (resourceId ushr 24) and 0xFF
            val targetTypeId    = (resourceId ushr 16) and 0xFF
            val targetEntryId   = resourceId and 0xFFFF

            while (buf.hasRemaining()) {
                val chunkStart = buf.position()
                val chunkType  = buf.getShort().toInt() and 0xFFFF
                val headerSize = buf.getShort().toInt() and 0xFFFF
                val chunkSize  = buf.getInt()

                if (chunkType == CHUNK_TYPE_PACKAGE) {
                    val result = searchInPackage(buf, chunkStart, headerSize, chunkSize,
                        globalStringPool, targetTypeId, targetEntryId)
                    if (result != null) return result
                }

                buf.position(chunkStart + chunkSize)
            }

            return null
        } catch (e: Exception) {
            throw ExtractionException("resources.arsc 파싱 실패", e)
        }
    }

    /**
     * resources.arsc Global String Pool에서 인덱스로 문자열을 가져온다.
     *
     * @param arscData resources.arsc 바이트 배열
     * @param stringIndex 문자열 풀 인덱스
     * @returns 해당 인덱스의 문자열. 범위 초과 시 null.
     */
    fun getGlobalString(arscData: ByteArray, stringIndex: Int): String? {
        try {
            val buf = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)

            val tableType = buf.getShort().toInt() and 0xFFFF
            if (tableType != CHUNK_TYPE_TABLE) return null
            buf.getShort()
            buf.getInt()
            buf.getInt()

            val pool = parseStringPool(buf)
            if (stringIndex < 0 || stringIndex >= pool.size) return null
            return pool[stringIndex]
        } catch (e: Exception) {
            return null
        }
    }

    private fun searchInPackage(
        buf: ByteBuffer,
        packageStart: Int,
        packageHeaderSize: Int,
        packageSize: Int,
        globalStringPool: List<String>,
        targetTypeId: Int,
        targetEntryId: Int
    ): String? {
        val packageEnd = packageStart + packageSize

        buf.position(packageStart + 8)
        val packageId = buf.getInt()

        buf.position(packageStart + packageHeaderSize)

        while (buf.position() < packageEnd && buf.hasRemaining()) {
            val chunkStart = buf.position()
            if (buf.remaining() < 8) break

            val chunkType  = buf.getShort().toInt() and 0xFFFF
            val headerSize = buf.getShort().toInt() and 0xFFFF
            val chunkSize  = buf.getInt()

            if (chunkType == CHUNK_TYPE_STRING_POOL) {
                buf.position(chunkStart)
                parseStringPool(buf)
            } else if (chunkType == CHUNK_TYPE_TYPE) {
                val typeId = buf.get().toInt() and 0xFF
                if (typeId == targetTypeId) {
                    buf.position(chunkStart)
                    val result = searchInTypeChunk(buf, headerSize, chunkSize,
                        globalStringPool, targetEntryId)
                    if (result != null) return result
                }
            }

            buf.position(chunkStart + chunkSize)
        }

        return null
    }

    private fun searchInTypeChunk(
        buf: ByteBuffer,
        headerSize: Int,
        chunkSize: Int,
        globalStringPool: List<String>,
        targetEntryId: Int
    ): String? {
        val chunkStart = buf.position()

        buf.position(chunkStart + headerSize)

        val entryOffsets = mutableListOf<Int>()
        val entriesStart = chunkStart + headerSize

        buf.position(chunkStart + 8)
        val typeId    = buf.get().toInt() and 0xFF
        val res0      = buf.get()
        val res1      = buf.getShort()
        val entryCount = buf.getInt()

        buf.position(chunkStart + headerSize)

        return null
    }

    private fun parseStringPool(buf: ByteBuffer): List<String> {
        val poolStart    = buf.position()
        val type         = buf.getShort().toInt() and 0xFFFF
        val headerSize   = buf.getShort().toInt() and 0xFFFF
        val chunkSize    = buf.getInt()
        val stringCount  = buf.getInt()
        val styleCount   = buf.getInt()
        val flags        = buf.getInt()
        val stringsStart = buf.getInt()
        val stylesStart  = buf.getInt()

        val isUtf8  = (flags and STRING_POOL_FLAG_UTF8) != 0
        val offsets = IntArray(stringCount) { buf.getInt() }

        val dataStart = poolStart + stringsStart
        val strings   = mutableListOf<String>()

        for (i in 0 until stringCount) {
            buf.position(dataStart + offsets[i])
            strings.add(if (isUtf8) readUtf8String(buf) else readUtf16String(buf))
        }

        buf.position(poolStart + chunkSize)
        return strings
    }

    private fun readUtf8String(buf: ByteBuffer): String {
        val charLen = readUtf8Len(buf)
        val byteLen = readUtf8Len(buf)
        val bytes   = ByteArray(byteLen)
        buf.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf8Len(buf: ByteBuffer): Int {
        val first = buf.get().toInt() and 0xFF
        return if ((first and 0x80) != 0) {
            ((first and 0x7F) shl 8) or (buf.get().toInt() and 0xFF)
        } else {
            first
        }
    }

    private fun readUtf16String(buf: ByteBuffer): String {
        val charLen = readUtf16Len(buf)
        val chars   = CharArray(charLen) { (buf.getShort().toInt() and 0xFFFF).toChar() }
        buf.getShort()
        return String(chars)
    }

    private fun readUtf16Len(buf: ByteBuffer): Int {
        val first = buf.getShort().toInt() and 0xFFFF
        return if ((first and 0x8000) != 0) {
            ((first and 0x7FFF) shl 16) or (buf.getShort().toInt() and 0xFFFF)
        } else {
            first
        }
    }
}
