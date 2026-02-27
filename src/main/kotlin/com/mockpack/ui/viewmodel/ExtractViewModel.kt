package com.mockpack.ui.viewmodel

import com.mockpack.core.model.AndroidMetadata
import com.mockpack.core.model.AppMetadata
import com.mockpack.core.model.IosMetadata
import com.mockpack.extract.ExtractorFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path

/**
 * Extract 화면의 상태를 관리하는 ViewModel.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class ExtractViewModel {

    private val scope  = CoroutineScope(Dispatchers.IO)
    private val json   = Json { prettyPrint = true }

    private val _state = MutableStateFlow(ExtractState())
    val state: StateFlow<ExtractState> = _state.asStateFlow()

    /**
     * 파일을 드롭받아 메타데이터를 추출한다.
     *
     * @param filePath 드롭된 파일 경로
     */
    fun onFileDrop(filePath: Path) {
        _state.value = ExtractState(isLoading = true, filePath = filePath)

        scope.launch {
            try {
                val extractor = ExtractorFactory.create(filePath)
                val metadata  = extractor.extract(filePath)
                _state.value = ExtractState(
                    metadata = metadata,
                    filePath = filePath
                )
            } catch (e: Exception) {
                _state.value = ExtractState(
                    error    = e.message ?: "알 수 없는 오류",
                    filePath = filePath
                )
            }
        }
    }

    /**
     * 추출 결과를 JSON 문자열로 직렬화한다.
     *
     * @returns JSON 문자열. 메타데이터가 없으면 null.
     */
    fun toJson(): String? {
        val metadata = _state.value.metadata ?: return null
        return when (metadata) {
            is AndroidMetadata -> json.encodeToString(metadata)
            is IosMetadata     -> json.encodeToString(metadata)
        }
    }

    /** 상태 초기화 */
    fun reset() {
        _state.value = ExtractState()
    }
}

/**
 * Extract 화면 상태.
 */
data class ExtractState(
    val metadata:  AppMetadata? = null,
    val filePath:  Path?        = null,
    val isLoading: Boolean      = false,
    val error:     String?      = null
)
