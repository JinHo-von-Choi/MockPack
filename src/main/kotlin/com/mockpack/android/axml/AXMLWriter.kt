package com.mockpack.android.axml

import com.mockpack.core.model.AndroidMetadata
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AndroidMetadata를 기반으로 최소 AndroidManifest.xml의 바이너리 AXML을 생성한다.
 *
 * 생성되는 XML 구조:
 * ```xml
 * <manifest xmlns:android="..."
 *     package="com.example.app"
 *     android:versionCode="1"
 *     android:versionName="1.0.0">
 *   <uses-sdk android:minSdkVersion="21" android:targetSdkVersion="34" />
 *   <application android:label="MyApp" />
 * </manifest>
 * ```
 *
 * @author 최진호
 * @since 2026-02-27
 */
object AXMLWriter {

    /**
     * AndroidMetadata를 바이너리 AXML 바이트 배열로 변환한다.
     *
     * @param metadata Android 메타데이터
     * @returns 바이너리 AXML 바이트 배열
     */
    fun generate(metadata: AndroidMetadata): ByteArray {
        val strings = buildStringPool(metadata)
        val stringIndexMap = strings.withIndex().associate { (i, s) -> s to i }

        val resourceIds = buildResourceIdMap()

        val stringPoolChunk   = encodeStringPool(strings)
        val resourceMapChunk  = encodeResourceIdMap(resourceIds)
        val xmlBodyChunks     = encodeXmlBody(metadata, stringIndexMap)

        val totalSize = 8 + stringPoolChunk.size + resourceMapChunk.size + xmlBodyChunks.size

        val result = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        result.putShort(AXMLConstants.CHUNK_TYPE_XML.toShort())
        result.putShort(8.toShort())
        result.putInt(totalSize)
        result.put(stringPoolChunk)
        result.put(resourceMapChunk)
        result.put(xmlBodyChunks)

        return result.array()
    }

    private fun buildStringPool(metadata: AndroidMetadata): List<String> {
        val strings = mutableListOf<String>()

        strings.add("")
        strings.add(AXMLConstants.ANDROID_NS_URI)
        strings.add(AXMLConstants.ANDROID_NS_PREFIX)
        strings.add("manifest")
        strings.add("uses-sdk")
        strings.add("application")
        strings.add("package")
        strings.add("versionCode")
        strings.add("versionName")
        strings.add("minSdkVersion")
        strings.add("targetSdkVersion")
        strings.add("label")
        strings.add(metadata.packageId)
        strings.add(metadata.versionName)
        strings.add(metadata.appName)

        return strings
    }

    private fun buildResourceIdMap(): List<Int> {
        return listOf(
            0,
            0,
            0,
            0,
            0,
            0,
            AXMLConstants.ResourceIds.ATTR_PACKAGE,
            AXMLConstants.ResourceIds.ATTR_VERSION_CODE,
            AXMLConstants.ResourceIds.ATTR_VERSION_NAME,
            AXMLConstants.ResourceIds.ATTR_MIN_SDK_VERSION,
            AXMLConstants.ResourceIds.ATTR_TARGET_SDK_VERSION,
            AXMLConstants.ResourceIds.ATTR_LABEL,
            0,
            0,
            0
        )
    }

    private fun encodeStringPool(strings: List<String>): ByteArray {
        val utf8Bytes = strings.map { it.toByteArray(Charsets.UTF_8) }

        var stringsDataSize = 0
        for (bytes in utf8Bytes) {
            val charLen = countUtf16Chars(bytes)
            val byteLen = bytes.size
            stringsDataSize += encodedLengthSize(charLen) + encodedLengthSize(byteLen) + byteLen + 1
        }

        val offsetsSize  = strings.size * 4
        val headerSize   = 28
        val stringsStart = headerSize + offsetsSize

        val paddedStringsDataSize = ((stringsDataSize + 3) / 4) * 4
        val chunkSize = stringsStart + paddedStringsDataSize

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.putShort(AXMLConstants.CHUNK_TYPE_STRING_POOL.toShort())
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.putInt(strings.size)
        buf.putInt(0)
        buf.putInt(AXMLConstants.STRING_POOL_FLAG_UTF8)
        buf.putInt(stringsStart)
        buf.putInt(0)

        var offset = 0
        for (bytes in utf8Bytes) {
            buf.putInt(offset)
            val charLen  = countUtf16Chars(bytes)
            val byteLen  = bytes.size
            offset += encodedLengthSize(charLen) + encodedLengthSize(byteLen) + byteLen + 1
        }

        for (bytes in utf8Bytes) {
            val charLen = countUtf16Chars(bytes)
            encodeUtf8Length(buf, charLen)
            encodeUtf8Length(buf, bytes.size)
            buf.put(bytes)
            buf.put(0)
        }

        return buf.array()
    }

    private fun countUtf16Chars(utf8Bytes: ByteArray): Int {
        return String(utf8Bytes, Charsets.UTF_8).length
    }

    private fun encodedLengthSize(len: Int): Int {
        return if (len >= 128) 2 else 1
    }

    private fun encodeUtf8Length(buf: ByteBuffer, len: Int) {
        if (len >= 128) {
            buf.put(((len shr 8) or 0x80).toByte())
            buf.put((len and 0xFF).toByte())
        } else {
            buf.put(len.toByte())
        }
    }

    private fun encodeResourceIdMap(ids: List<Int>): ByteArray {
        val headerSize = 8
        val chunkSize  = headerSize + ids.size * 4
        val buf        = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.putShort(AXMLConstants.CHUNK_TYPE_RESOURCE_MAP.toShort())
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        for (id in ids) {
            buf.putInt(id)
        }

        return buf.array()
    }

    private fun encodeXmlBody(
        metadata: AndroidMetadata,
        idx: Map<String, Int>
    ): ByteArray {
        val out = ByteArrayOutputStream()

        out.write(encodeStartNamespace(idx))
        out.write(encodeManifestStartElement(metadata, idx))
        out.write(encodeUsesSdkStartElement(metadata, idx))
        out.write(encodeEndElement(idx, "uses-sdk"))
        out.write(encodeApplicationStartElement(metadata, idx))
        out.write(encodeEndElement(idx, "application"))
        out.write(encodeEndElement(idx, "manifest"))
        out.write(encodeEndNamespace(idx))

        return out.toByteArray()
    }

    private fun encodeStartNamespace(idx: Map<String, Int>): ByteArray {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(AXMLConstants.CHUNK_TYPE_START_NAMESPACE.toShort())
        buf.putShort(16.toShort())
        buf.putInt(24)
        buf.putInt(1)
        buf.putInt(-1)
        buf.putInt(idx[AXMLConstants.ANDROID_NS_PREFIX] ?: -1)
        buf.putInt(idx[AXMLConstants.ANDROID_NS_URI] ?: -1)
        return buf.array()
    }

    private fun encodeEndNamespace(idx: Map<String, Int>): ByteArray {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(AXMLConstants.CHUNK_TYPE_END_NAMESPACE.toShort())
        buf.putShort(16.toShort())
        buf.putInt(24)
        buf.putInt(1)
        buf.putInt(-1)
        buf.putInt(idx[AXMLConstants.ANDROID_NS_PREFIX] ?: -1)
        buf.putInt(idx[AXMLConstants.ANDROID_NS_URI] ?: -1)
        return buf.array()
    }

    private fun encodeManifestStartElement(metadata: AndroidMetadata, idx: Map<String, Int>): ByteArray {
        val attrs = listOf(
            attrEntry(idx, "", "package", metadata.packageId, AXMLConstants.TYPE_STRING, idx[metadata.packageId] ?: 0),
            attrEntry(idx, AXMLConstants.ANDROID_NS_URI, "versionCode", null, AXMLConstants.TYPE_INT_DEC, metadata.buildNumber.toInt()),
            attrEntry(idx, AXMLConstants.ANDROID_NS_URI, "versionName", metadata.versionName, AXMLConstants.TYPE_STRING, idx[metadata.versionName] ?: 0)
        )
        return encodeStartElement(idx, -1, "manifest", attrs)
    }

    private fun encodeUsesSdkStartElement(metadata: AndroidMetadata, idx: Map<String, Int>): ByteArray {
        val attrs = listOf(
            attrEntry(idx, AXMLConstants.ANDROID_NS_URI, "minSdkVersion", null, AXMLConstants.TYPE_INT_DEC, metadata.minSdkVersion),
            attrEntry(idx, AXMLConstants.ANDROID_NS_URI, "targetSdkVersion", null, AXMLConstants.TYPE_INT_DEC, metadata.targetSdkVersion)
        )
        return encodeStartElement(idx, -1, "uses-sdk", attrs)
    }

    private fun encodeApplicationStartElement(metadata: AndroidMetadata, idx: Map<String, Int>): ByteArray {
        val attrs = listOf(
            attrEntry(idx, AXMLConstants.ANDROID_NS_URI, "label", metadata.appName, AXMLConstants.TYPE_STRING, idx[metadata.appName] ?: 0)
        )
        return encodeStartElement(idx, -1, "application", attrs)
    }

    /**
     * StartElement 청크를 인코딩한다.
     *
     * ResXMLTree_node 헤더 (16 bytes):
     *   ResChunk_header(8) + lineNumber(4) + comment(4)
     *
     * ResXMLTree_attrExt body (20 bytes):
     *   ns(4) + name(4) + attrStart(2) + attrSize(2) +
     *   attrCount(2) + idIndex(2) + classIndex(2) + styleIndex(2)
     *
     * 속성 데이터: attrCount * 20 bytes
     */
    private fun encodeStartElement(
        idx: Map<String, Int>,
        nsIndex: Int,
        tagName: String,
        attrs: List<ByteArray>
    ): ByteArray {
        val attrCount   = attrs.size
        val nodeHeader  = 16
        val attrExtSize = 20
        val attrBytes   = attrCount * 20
        val chunkSize   = nodeHeader + attrExtSize + attrBytes

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)

        buf.putShort(AXMLConstants.CHUNK_TYPE_START_ELEMENT.toShort())
        buf.putShort(nodeHeader.toShort())
        buf.putInt(chunkSize)
        buf.putInt(1)
        buf.putInt(-1)

        buf.putInt(nsIndex)
        buf.putInt(idx[tagName] ?: -1)
        buf.putShort(0x14.toShort())
        buf.putShort(0x14.toShort())
        buf.putShort(attrCount.toShort())
        buf.putShort(0)
        buf.putShort(0)
        buf.putShort(0)

        for (attr in attrs) {
            buf.put(attr)
        }

        return buf.array()
    }

    private fun encodeEndElement(idx: Map<String, Int>, tagName: String): ByteArray {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(AXMLConstants.CHUNK_TYPE_END_ELEMENT.toShort())
        buf.putShort(16.toShort())
        buf.putInt(24)
        buf.putInt(1)
        buf.putInt(-1)
        buf.putInt(-1)
        buf.putInt(idx[tagName] ?: -1)
        return buf.array()
    }

    private fun attrEntry(
        idx: Map<String, Int>,
        namespace: String,
        name: String,
        rawString: String?,
        valueType: Int,
        valueData: Int
    ): ByteArray {
        val buf = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)

        val nsIdx   = if (namespace.isEmpty()) -1 else (idx[namespace] ?: -1)
        val nameIdx = idx[name] ?: -1
        val rawIdx  = if (rawString != null && valueType == AXMLConstants.TYPE_STRING) (idx[rawString] ?: -1) else -1

        buf.putInt(nsIdx)
        buf.putInt(nameIdx)
        buf.putInt(rawIdx)
        buf.putShort(0x08.toShort())
        buf.put(0)
        buf.put(valueType.toByte())
        buf.putInt(valueData)

        return buf.array()
    }
}
