package com.mockpack.ios.stub

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 최소 Mach-O arm64 바이너리 스텁을 생성한다.
 * 실제 실행은 불가능하지만 IPA 구조의 유효성을 위해 최소 헤더만 포함한다.
 *
 * Mach-O 64-bit 헤더 구조 (28 bytes):
 * - magic:      0xFEEDFACF (64-bit)
 * - cputype:    0x0100000C (ARM64)
 * - cpusubtype: 0x00000000 (ALL)
 * - filetype:   0x00000002 (MH_EXECUTE)
 * - ncmds:      0
 * - sizeofcmds: 0
 * - flags:      0x00000085 (MH_NOUNDEFS | MH_DYLDLINK | MH_PIE)
 * - reserved:   0
 *
 * @author 최진호
 * @since 2026-02-27
 */
object MachOStub {

    private const val MH_MAGIC_64   = 0xFEEDFACFu
    private const val CPU_TYPE_ARM64 = 0x0100000Cu
    private const val MH_EXECUTE    = 2u
    private const val MH_FLAGS      = 0x00000085u

    /**
     * 최소 Mach-O arm64 바이너리를 바이트 배열로 생성한다.
     *
     * @returns 32바이트 최소 Mach-O 헤더
     */
    fun generate(): ByteArray {
        val buffer = ByteBuffer.allocate(32)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        buffer.putInt(MH_MAGIC_64.toInt())
        buffer.putInt(CPU_TYPE_ARM64.toInt())
        buffer.putInt(0)
        buffer.putInt(MH_EXECUTE.toInt())
        buffer.putInt(0)
        buffer.putInt(0)
        buffer.putInt(MH_FLAGS.toInt())
        buffer.putInt(0)

        return buffer.array()
    }
}
