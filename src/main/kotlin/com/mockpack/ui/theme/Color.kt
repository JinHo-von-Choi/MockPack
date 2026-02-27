package com.mockpack.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * MockPack 컬러 팔레트.
 * 메인/서브 컬러는 추후 Coolors.co 등으로 확정 시 교체 예정.
 * 현재는 Material 3 기본 톤 기반 중립 팔레트로 구성.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object MockPackColors {
    val Background      = Color(0xFFF8F9FA)
    val Surface         = Color(0xFFFFFFFF)
    val SurfaceVariant  = Color(0xFFF1F3F5)
    val OnBackground    = Color(0xFF1A1A1A)
    val OnSurface       = Color(0xFF1A1A1A)
    val TextSecondary   = Color(0xFF6B7280)
    val Border          = Color(0xFFE5E7EB)
    val DividerColor    = Color(0xFFE5E7EB)

    val Primary         = Color(0xFF3B82F6)
    val OnPrimary       = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFDBEAFE)

    val Secondary       = Color(0xFF6366F1)
    val OnSecondary     = Color(0xFFFFFFFF)

    val Error           = Color(0xFFEF4444)
    val OnError         = Color(0xFFFFFFFF)

    val Success         = Color(0xFF22C55E)
}
