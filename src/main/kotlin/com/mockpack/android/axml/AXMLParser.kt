package com.mockpack.android.axml

import com.mockpack.core.error.ExtractionException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android 바이너리 XML (AXML) 파서.
 * APK 내 AndroidManifest.xml의 바이너리 형식을 파싱하여
 * 태그명, 속성명, 속성값을 추출한다.
 *
 * AOSP ResourceTypes.h 스펙을 기반으로 구현.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class AXMLParser(data: ByteArray) {

    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    /** 파싱된 문자열 풀 */
    private var stringPool: List<String> = emptyList()

    /** 리소스 ID 맵 */
    private var resourceIdMap: List<Int> = emptyList()

    /** 파싱된 XML 이벤트 */
    private val events: MutableList<AXMLEvent> = mutableListOf()

    /**
     * 바이너리 XML을 파싱하여 이벤트 목록을 생성한다.
     *
     * @returns 파싱된 AXML 이벤트 리스트
     * @throws ExtractionException 파싱 실패 시
     */
    fun parse(): List<AXMLEvent> {
        try {
            val fileType = buffer.getShort().toInt() and 0xFFFF
            if (fileType != AXMLConstants.CHUNK_TYPE_XML) {
                throw ExtractionException("AXML 파일이 아닙니다 (type=0x${fileType.toString(16)})")
            }
            buffer.getShort()
            buffer.getInt()

            while (buffer.hasRemaining()) {
                val chunkStart = buffer.position()
                val chunkType  = buffer.getShort().toInt() and 0xFFFF
                val headerSize = buffer.getShort().toInt() and 0xFFFF
                val chunkSize  = buffer.getInt()

                when (chunkType) {
                    AXMLConstants.CHUNK_TYPE_STRING_POOL   -> parseStringPool(chunkStart, chunkSize)
                    AXMLConstants.CHUNK_TYPE_RESOURCE_MAP  -> parseResourceIdMap(chunkStart, headerSize, chunkSize)
                    AXMLConstants.CHUNK_TYPE_START_NAMESPACE -> parseStartNamespace()
                    AXMLConstants.CHUNK_TYPE_END_NAMESPACE   -> parseEndNamespace()
                    AXMLConstants.CHUNK_TYPE_START_ELEMENT   -> parseStartElement()
                    AXMLConstants.CHUNK_TYPE_END_ELEMENT     -> parseEndElement()
                }

                buffer.position(chunkStart + chunkSize)
            }

            return events.toList()
        } catch (e: ExtractionException) {
            throw e
        } catch (e: Exception) {
            throw ExtractionException("AXML 파싱 실패", e)
        }
    }

    /**
     * 문자열 풀에서 지정된 인덱스의 문자열을 반환한다.
     *
     * @param index 문자열 풀 인덱스 (-1이면 빈 문자열)
     * @returns 해당 인덱스의 문자열
     */
    fun getString(index: Int): String {
        if (index < 0 || index >= stringPool.size) return ""
        return stringPool[index]
    }

    /**
     * 파싱 결과에서 특정 태그의 특정 속성값을 문자열로 찾는다.
     *
     * @param tagName  태그명 (e.g. "manifest")
     * @param attrName 속성명 (e.g. "package")
     * @param namespace 네임스페이스 URI (null이면 무시)
     * @returns 속성값 문자열. 없으면 null.
     */
    fun findAttributeValue(tagName: String, attrName: String, namespace: String? = null): String? {
        for (event in events) {
            if (event !is AXMLEvent.StartElement) continue
            if (getString(event.nameIndex) != tagName) continue

            for (attr in event.attributes) {
                val name = getString(attr.nameIndex)
                if (name != attrName) continue
                if (namespace != null) {
                    val ns = getString(attr.namespaceIndex)
                    if (ns != namespace) continue
                }
                return resolveAttributeValue(attr)
            }
        }
        return null
    }

    /**
     * 파싱 결과에서 특정 태그의 특정 속성의 정수값을 찾는다.
     *
     * @param tagName  태그명
     * @param attrName 속성명
     * @param namespace 네임스페이스 URI
     * @returns 정수 속성값. 없으면 null.
     */
    fun findAttributeIntValue(tagName: String, attrName: String, namespace: String? = null): Int? {
        for (event in events) {
            if (event !is AXMLEvent.StartElement) continue
            if (getString(event.nameIndex) != tagName) continue

            for (attr in event.attributes) {
                val name = getString(attr.nameIndex)
                if (name != attrName) continue
                if (namespace != null) {
                    val ns = getString(attr.namespaceIndex)
                    if (ns != namespace) continue
                }
                if (attr.valueType == AXMLConstants.TYPE_INT_DEC
                    || attr.valueType == AXMLConstants.TYPE_INT_HEX) {
                    return attr.valueData
                }
            }
        }
        return null
    }

    private fun resolveAttributeValue(attr: AXMLAttribute): String {
        return when (attr.valueType) {
            AXMLConstants.TYPE_STRING    -> getString(attr.valueData)
            AXMLConstants.TYPE_INT_DEC   -> attr.valueData.toString()
            AXMLConstants.TYPE_INT_HEX   -> "0x${attr.valueData.toString(16)}"
            AXMLConstants.TYPE_INT_BOOLEAN -> if (attr.valueData != 0) "true" else "false"
            AXMLConstants.TYPE_REFERENCE -> "@0x${attr.valueData.toString(16)}"
            else -> {
                if (attr.rawValueIndex >= 0) getString(attr.rawValueIndex)
                else attr.valueData.toString()
            }
        }
    }

    private fun parseStringPool(chunkStart: Int, chunkSize: Int) {
        val stringCount = buffer.getInt()
        val styleCount  = buffer.getInt()
        val flags       = buffer.getInt()
        val stringsStart = buffer.getInt()
        val stylesStart  = buffer.getInt()

        val isUtf8 = (flags and AXMLConstants.STRING_POOL_FLAG_UTF8) != 0

        val offsets = IntArray(stringCount) { buffer.getInt() }

        val dataStart = chunkStart + stringsStart
        val strings   = mutableListOf<String>()

        for (i in 0 until stringCount) {
            val pos = dataStart + offsets[i]
            buffer.position(pos)

            if (isUtf8) {
                strings.add(readUtf8String())
            } else {
                strings.add(readUtf16String())
            }
        }

        stringPool = strings
    }

    private fun readUtf8String(): String {
        val charLen = readUtf8Length()
        val byteLen = readUtf8Length()
        val bytes   = ByteArray(byteLen)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf8Length(): Int {
        val first = buffer.get().toInt() and 0xFF
        return if ((first and 0x80) != 0) {
            ((first and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
        } else {
            first
        }
    }

    private fun readUtf16String(): String {
        val charLen = readUtf16Length()
        val chars   = CharArray(charLen) {
            (buffer.getShort().toInt() and 0xFFFF).toChar()
        }
        buffer.getShort()
        return String(chars)
    }

    private fun readUtf16Length(): Int {
        val first = buffer.getShort().toInt() and 0xFFFF
        return if ((first and 0x8000) != 0) {
            ((first and 0x7FFF) shl 16) or (buffer.getShort().toInt() and 0xFFFF)
        } else {
            first
        }
    }

    private fun parseResourceIdMap(chunkStart: Int, headerSize: Int, chunkSize: Int) {
        val count = (chunkSize - headerSize) / 4
        val ids   = mutableListOf<Int>()
        for (i in 0 until count) {
            ids.add(buffer.getInt())
        }
        resourceIdMap = ids
    }

    private fun parseStartNamespace() {
        val lineNumber    = buffer.getInt()
        val comment       = buffer.getInt()
        val prefixIndex   = buffer.getInt()
        val uriIndex      = buffer.getInt()

        events.add(AXMLEvent.StartNamespace(prefixIndex, uriIndex, lineNumber))
    }

    private fun parseEndNamespace() {
        val lineNumber  = buffer.getInt()
        val comment     = buffer.getInt()
        val prefixIndex = buffer.getInt()
        val uriIndex    = buffer.getInt()

        events.add(AXMLEvent.EndNamespace(prefixIndex, uriIndex, lineNumber))
    }

    private fun parseStartElement() {
        val lineNumber      = buffer.getInt()
        val comment         = buffer.getInt()
        val namespaceIndex  = buffer.getInt()
        val nameIndex       = buffer.getInt()
        val attrStart       = buffer.getShort().toInt() and 0xFFFF
        val attrSize        = buffer.getShort().toInt() and 0xFFFF
        val attrCount       = buffer.getShort().toInt() and 0xFFFF
        val idIndex         = buffer.getShort().toInt() and 0xFFFF
        val classIndex      = buffer.getShort().toInt() and 0xFFFF
        val styleIndex      = buffer.getShort().toInt() and 0xFFFF

        val attributes = mutableListOf<AXMLAttribute>()
        for (i in 0 until attrCount) {
            val attrNsIndex   = buffer.getInt()
            val attrNameIndex = buffer.getInt()
            val rawValueIndex = buffer.getInt()
            val valueSize     = buffer.getShort().toInt() and 0xFFFF
            buffer.get()
            val valueType     = buffer.get().toInt() and 0xFF
            val valueData     = buffer.getInt()

            attributes.add(AXMLAttribute(
                namespaceIndex = attrNsIndex,
                nameIndex      = attrNameIndex,
                rawValueIndex  = rawValueIndex,
                valueType      = valueType,
                valueData      = valueData
            ))
        }

        events.add(AXMLEvent.StartElement(namespaceIndex, nameIndex, attributes, lineNumber))
    }

    private fun parseEndElement() {
        val lineNumber     = buffer.getInt()
        val comment        = buffer.getInt()
        val namespaceIndex = buffer.getInt()
        val nameIndex      = buffer.getInt()

        events.add(AXMLEvent.EndElement(namespaceIndex, nameIndex, lineNumber))
    }
}

/**
 * AXML 파싱 이벤트.
 */
sealed interface AXMLEvent {
    data class StartNamespace(val prefixIndex: Int, val uriIndex: Int, val lineNumber: Int) : AXMLEvent
    data class EndNamespace(val prefixIndex: Int, val uriIndex: Int, val lineNumber: Int) : AXMLEvent
    data class StartElement(
        val namespaceIndex: Int,
        val nameIndex:      Int,
        val attributes:     List<AXMLAttribute>,
        val lineNumber:     Int
    ) : AXMLEvent
    data class EndElement(val namespaceIndex: Int, val nameIndex: Int, val lineNumber: Int) : AXMLEvent
}

/**
 * AXML 속성 데이터.
 */
data class AXMLAttribute(
    val namespaceIndex: Int,
    val nameIndex:      Int,
    val rawValueIndex:  Int,
    val valueType:      Int,
    val valueData:      Int
)
