package com.mockpack.android.resource

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Mock APK용 최소 resources.arsc를 생성한다.
 * app_name 문자열 리소스 하나만 포함하는 최소 구조.
 *
 * 생성되는 리소스 테이블 구조:
 * - Table Header (12 bytes)
 * - Global String Pool (app_name 값 포함)
 * - Package (id=0x7F)
 *   - Type String Pool ("string")
 *   - Key String Pool ("app_name")
 *   - TypeSpec (string, 1 entry)
 *   - Type (string, default config, 1 entry -> Global String Pool[0])
 *
 * @author 최진호
 * @since 2026-02-27
 */
object ResourceTableWriter {

    private const val CHUNK_TYPE_TABLE: Short       = 0x0002
    private const val CHUNK_TYPE_STRING_POOL: Short = 0x0001
    private const val CHUNK_TYPE_PACKAGE: Short     = 0x0200.toShort()
    private const val CHUNK_TYPE_TYPE_SPEC: Short   = 0x0202.toShort()
    private const val CHUNK_TYPE_TYPE: Short        = 0x0201.toShort()
    private const val STRING_POOL_FLAG_UTF8: Int    = 0x00000100
    private const val PACKAGE_HEADER_SIZE           = 288

    /**
     * app_name만 포함하는 최소 resources.arsc를 생성한다.
     *
     * @param packageName 패키지명 (e.g. "com.example.app")
     * @param appName     앱 이름 (e.g. "MyApp")
     * @returns resources.arsc 바이트 배열
     */
    fun generate(packageName: String, appName: String): ByteArray {
        val globalStringPool = buildStringPool(listOf(appName))
        val typeStringPool   = buildStringPool(listOf("string"))
        val keyStringPool    = buildStringPool(listOf("app_name"))
        val typeSpecChunk    = buildTypeSpecChunk(typeId = 1, entryCount = 1)
        val typeChunk        = buildTypeChunk(typeId = 1, entryCount = 1, globalStringIndex = 0)

        val packageChunk = buildPackageChunk(
            packageName        = packageName,
            typeStringPool     = typeStringPool,
            keyStringPool      = keyStringPool,
            typeSpecChunk      = typeSpecChunk,
            typeChunk          = typeChunk
        )

        val tableBodySize = globalStringPool.size + packageChunk.size
        val tableSize     = 12 + tableBodySize

        val buf = ByteBuffer.allocate(tableSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(CHUNK_TYPE_TABLE)
        buf.putShort(12)
        buf.putInt(tableSize)
        buf.putInt(1)
        buf.put(globalStringPool)
        buf.put(packageChunk)

        return buf.array()
    }

    private fun buildStringPool(strings: List<String>): ByteArray {
        val utf8Bytes = strings.map { it.toByteArray(Charsets.UTF_8) }

        var stringsDataSize = 0
        for (bytes in utf8Bytes) {
            val charLen = String(bytes, Charsets.UTF_8).length
            stringsDataSize += encodedLengthSize(charLen) + encodedLengthSize(bytes.size) + bytes.size + 1
        }

        val headerSize   = 28
        val offsetsSize  = strings.size * 4
        val stringsStart = headerSize + offsetsSize
        val paddedSize   = ((stringsDataSize + 3) / 4) * 4
        val chunkSize    = stringsStart + paddedSize

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.putShort(CHUNK_TYPE_STRING_POOL)
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.putInt(strings.size)
        buf.putInt(0)
        buf.putInt(STRING_POOL_FLAG_UTF8)
        buf.putInt(stringsStart)
        buf.putInt(0)

        var offset = 0
        for (bytes in utf8Bytes) {
            buf.putInt(offset)
            val charLen = String(bytes, Charsets.UTF_8).length
            offset += encodedLengthSize(charLen) + encodedLengthSize(bytes.size) + bytes.size + 1
        }

        for (bytes in utf8Bytes) {
            val charLen = String(bytes, Charsets.UTF_8).length
            encodeUtf8Length(buf, charLen)
            encodeUtf8Length(buf, bytes.size)
            buf.put(bytes)
            buf.put(0)
        }

        return buf.array()
    }

    private fun buildTypeSpecChunk(typeId: Int, entryCount: Int): ByteArray {
        val headerSize = 16
        val chunkSize  = headerSize + entryCount * 4

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(CHUNK_TYPE_TYPE_SPEC)
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.put(typeId.toByte())
        buf.put(0)
        buf.putShort(0)
        buf.putInt(entryCount)
        for (i in 0 until entryCount) {
            buf.putInt(0)
        }
        return buf.array()
    }

    private fun buildTypeChunk(typeId: Int, entryCount: Int, globalStringIndex: Int): ByteArray {
        val configSize      = 56
        val headerSize      = 8 + 4 + 4 + 4 + configSize
        val entryOffsetSize = entryCount * 4
        val entryDataSize   = 8 + 8

        val chunkSize = headerSize + entryOffsetSize + entryDataSize

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.putShort(CHUNK_TYPE_TYPE)
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.put(typeId.toByte())
        buf.put(0)
        buf.putShort(0)
        buf.putInt(entryCount)
        buf.putInt(headerSize + entryOffsetSize)

        buf.putInt(configSize)
        repeat(configSize - 4) { buf.put(0) }

        buf.putInt(0)

        buf.putShort(8)
        buf.putShort(0)
        buf.putInt(0)

        buf.putShort(8)
        buf.put(0)
        buf.put(0x03)
        buf.putInt(globalStringIndex)

        return buf.array()
    }

    /**
     * Package 청크를 올바른 오프셋으로 조립한다.
     *
     * ResTable_package 헤더 (288 bytes):
     *   ResChunk_header (8) + id (4) + name (256) +
     *   typeStrings (4) + lastPublicType (4) +
     *   keyStrings (4) + lastPublicKey (4) + typeIdOffset (4)
     *
     * typeStrings: Package 청크 시작부터 Type String Pool까지의 오프셋
     * keyStrings:  Package 청크 시작부터 Key String Pool까지의 오프셋
     */
    private fun buildPackageChunk(
        packageName: String,
        typeStringPool: ByteArray,
        keyStringPool: ByteArray,
        typeSpecChunk: ByteArray,
        typeChunk: ByteArray
    ): ByteArray {
        val typeStringsOffset = PACKAGE_HEADER_SIZE
        val keyStringsOffset  = PACKAGE_HEADER_SIZE + typeStringPool.size
        val bodySize          = typeStringPool.size + keyStringPool.size + typeSpecChunk.size + typeChunk.size
        val chunkSize         = PACKAGE_HEADER_SIZE + bodySize

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.putShort(CHUNK_TYPE_PACKAGE)
        buf.putShort(PACKAGE_HEADER_SIZE.toShort())
        buf.putInt(chunkSize)
        buf.putInt(0x7F)

        val nameChars = packageName.toCharArray()
        for (i in 0 until 128) {
            buf.putChar(if (i < nameChars.size) nameChars[i] else '\u0000')
        }

        buf.putInt(typeStringsOffset)
        buf.putInt(0)
        buf.putInt(keyStringsOffset)
        buf.putInt(0)
        buf.putInt(0)

        buf.put(typeStringPool)
        buf.put(keyStringPool)
        buf.put(typeSpecChunk)
        buf.put(typeChunk)

        return buf.array()
    }

    private fun encodedLengthSize(len: Int): Int = if (len >= 128) 2 else 1

    private fun encodeUtf8Length(buf: ByteBuffer, len: Int) {
        if (len >= 128) {
            buf.put(((len shr 8) or 0x80).toByte())
            buf.put((len and 0xFF).toByte())
        } else {
            buf.put(len.toByte())
        }
    }
}
