package com.mockpack.android.dex

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.Adler32

/**
 * Mock APK용 최소 유효 DEX 파일을 생성한다.
 * 빈 클래스 하나만 포함하는 최소 구조로, DEX 파일 포맷 검증을 통과하는 것이 목적이다.
 *
 * DEX 파일 구조 (최소):
 * - DEX Header (112 bytes)
 * - String IDs
 * - Type IDs
 * - Proto IDs
 * - Class Defs
 * - Data Section
 *
 * @author 최진호
 * @since 2026-02-27
 */
object DexWriter {

    private val DEX_MAGIC = byteArrayOf(0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00)

    private const val HEADER_SIZE     = 112
    private const val ENDIAN_CONSTANT = 0x12345678

    /**
     * 최소 유효 DEX 파일을 생성한다.
     * 단일 빈 클래스(Lcom/mockpack/Mock;)를 정의한다.
     *
     * @param className 클래스명 (e.g. "Lcom/mockpack/Mock;")
     * @returns 유효한 DEX 파일 바이트 배열
     */
    fun generate(className: String = "Lcom/mockpack/Mock;"): ByteArray {
        val objectDescriptor = "Ljava/lang/Object;"

        val stringData = listOf(className, objectDescriptor)
        val stringDataBytes = stringData.map { encodeStringData(it) }

        val typeIds   = listOf(0, 1)
        val classDef  = 0
        val superType = 1

        val stringIdsOffset = HEADER_SIZE
        val stringIdsSize   = stringData.size * 4
        val typeIdsOffset   = stringIdsOffset + stringIdsSize
        val typeIdsSize     = typeIds.size * 4
        val classDefsOffset = typeIdsOffset + typeIdsSize
        val classDefsSize   = 32
        val dataOffset      = classDefsOffset + classDefsSize

        var currentDataOffset = dataOffset
        val stringDataOffsets = mutableListOf<Int>()
        for (bytes in stringDataBytes) {
            stringDataOffsets.add(currentDataOffset)
            currentDataOffset += bytes.size
        }

        val classDataOffset = currentDataOffset
        val classDataBytes  = encodeClassData()
        currentDataOffset += classDataBytes.size

        val fileSize  = currentDataOffset
        val mapOffset = 0

        val buf = ByteBuffer.allocate(fileSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.put(DEX_MAGIC)
        buf.putInt(0)
        val sha1Pos = buf.position()
        buf.put(ByteArray(20))
        buf.putInt(fileSize)
        buf.putInt(HEADER_SIZE)
        buf.putInt(ENDIAN_CONSTANT)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(mapOffset)
        buf.putInt(stringData.size)
        buf.putInt(stringIdsOffset)
        buf.putInt(typeIds.size)
        buf.putInt(typeIdsOffset)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(0)
        buf.putInt(1)
        buf.putInt(classDefsOffset)
        buf.putInt(currentDataOffset - dataOffset)
        buf.putInt(dataOffset)

        for (offset in stringDataOffsets) {
            buf.putInt(offset)
        }

        for (typeId in typeIds) {
            buf.putInt(typeId)
        }

        buf.putInt(classDef)
        buf.putInt(0x00000001)
        buf.putInt(superType)
        buf.putInt(0)
        buf.putInt(-1)
        buf.putInt(-1)
        buf.putInt(-1)
        buf.putInt(classDataOffset)

        for (bytes in stringDataBytes) {
            buf.put(bytes)
        }

        buf.put(classDataBytes)

        val data = buf.array()

        val sha1 = MessageDigest.getInstance("SHA-1").digest(data.copyOfRange(32, fileSize))
        System.arraycopy(sha1, 0, data, 12, 20)

        val adler32 = Adler32()
        adler32.update(data, 12, fileSize - 12)
        val checksum = adler32.value.toInt()
        ByteBuffer.wrap(data, 8, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(checksum)

        return data
    }

    private fun encodeStringData(str: String): ByteArray {
        val utf8  = str.toByteArray(Charsets.UTF_8)
        val uleb  = encodeUleb128(utf8.size)
        return uleb + utf8 + byteArrayOf(0)
    }

    private fun encodeClassData(): ByteArray {
        return byteArrayOf(0, 0, 0, 0)
    }

    private fun encodeUleb128(value: Int): ByteArray {
        val result = mutableListOf<Byte>()
        var remaining = value
        do {
            var byte = remaining and 0x7F
            remaining = remaining ushr 7
            if (remaining != 0) {
                byte = byte or 0x80
            }
            result.add(byte.toByte())
        } while (remaining != 0)
        return result.toByteArray()
    }
}
