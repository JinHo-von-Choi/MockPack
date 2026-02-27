package com.mockpack.ui.viewmodel

import com.mockpack.build.BuilderFactory
import com.mockpack.core.model.AndroidMetadata
import com.mockpack.core.model.AppMetadata
import com.mockpack.core.model.IosMetadata
import com.mockpack.util.ValidationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.file.Path

/**
 * Build 화면의 상태를 관리하는 ViewModel.
 *
 * @author 최진호
 * @since 2026-02-27
 */
class BuildViewModel {

    private val scope  = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(BuildState())
    val state: StateFlow<BuildState> = _state.asStateFlow()

    /**
     * 메타데이터를 기반으로 Mock 패키지를 빌드한다.
     *
     * @param metadata   빌드할 메타데이터
     * @param outputPath 출력 파일 경로
     */
    fun build(metadata: AppMetadata, outputPath: Path) {
        _state.value = _state.value.copy(isBuilding = true, error = null, outputPath = null)

        scope.launch {
            try {
                ValidationUtils.validate(metadata)

                val builder = BuilderFactory.create(metadata)

                @Suppress("UNCHECKED_CAST")
                when (metadata) {
                    is AndroidMetadata -> (builder as com.mockpack.build.MockBuilder<AndroidMetadata>).build(metadata, outputPath)
                    is IosMetadata     -> (builder as com.mockpack.build.MockBuilder<IosMetadata>).build(metadata, outputPath)
                }

                _state.value = _state.value.copy(
                    isBuilding = false,
                    outputPath = outputPath,
                    error      = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isBuilding = false,
                    error      = e.message ?: "빌드 실패"
                )
            }
        }
    }

    /** 상태 초기화 */
    fun reset() {
        _state.value = BuildState()
    }
}

/**
 * Build 화면 상태.
 */
data class BuildState(
    val isBuilding: Boolean = false,
    val outputPath: Path?   = null,
    val error:      String? = null
)
