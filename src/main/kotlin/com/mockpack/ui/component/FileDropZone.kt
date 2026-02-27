package com.mockpack.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mockpack.ui.theme.MockPackColors
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import java.nio.file.Path
import javax.swing.JFrame

/**
 * 파일 드래그앤드롭 수신 영역.
 * AWT DropTarget을 Compose Desktop의 내부 컴포넌트에 등록하여 파일 드롭 이벤트를 처리한다.
 * Compose Desktop의 SkiaLayer가 Window 레벨 DropTarget을 가로채므로,
 * JFrame.contentPane의 하위 컴포넌트에 직접 등록한다.
 *
 * @param window     AWT Window (JFrame, DropTarget 등록 대상)
 * @param onFileDrop 파일 드롭 시 호출되는 콜백
 * @param modifier   Modifier
 *
 * @author 최진호
 * @since 2026-02-27
 */
@Composable
fun FileDropZone(
    window: java.awt.Window,
    onFileDrop: (Path) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    DisposableEffect(window) {
        val adapter = object : DropTargetAdapter() {
            override fun dragEnter(dtde: DropTargetDragEvent) {
                isDragging = true
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            }

            override fun dragOver(dtde: DropTargetDragEvent) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            }

            override fun dragExit(dte: DropTargetEvent) {
                isDragging = false
            }

            override fun drop(event: DropTargetDropEvent) {
                isDragging = false
                event.acceptDrop(DnDConstants.ACTION_COPY)

                val transferable = event.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    val target = files.firstOrNull { file ->
                        val ext = file.extension.lowercase()
                        ext == "apk" || ext == "ipa"
                    }
                    target?.let { onFileDrop(it.toPath()) }
                }

                event.dropComplete(true)
            }
        }

        val targets = mutableListOf<Pair<java.awt.Component, DropTarget?>>()

        fun registerDropTarget(component: java.awt.Component) {
            val oldTarget = component.dropTarget
            targets.add(component to oldTarget)
            component.dropTarget = DropTarget(component, DnDConstants.ACTION_COPY, adapter, true)
        }

        if (window is JFrame) {
            registerDropTarget(window.contentPane)
            for (comp in window.contentPane.components) {
                registerDropTarget(comp)
                if (comp is java.awt.Container) {
                    for (child in comp.components) {
                        registerDropTarget(child)
                    }
                }
            }
        }
        registerDropTarget(window)

        onDispose {
            for ((comp, oldTarget) in targets) {
                comp.dropTarget = oldTarget
            }
        }
    }

    val borderColor = if (isDragging) MockPackColors.Primary else MockPackColors.Border
    val bgColor     = if (isDragging) MockPackColors.PrimaryContainer.copy(alpha = 0.3f) else Color.Transparent
    val shape       = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .border(BorderStroke(2.dp, borderColor), shape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text  = if (isDragging) "+" else "^",
                style = MaterialTheme.typography.headlineLarge,
                color = if (isDragging) MockPackColors.Primary else MockPackColors.TextSecondary
            )
            Text(
                text  = if (isDragging) "여기에 놓으세요" else "APK 또는 IPA 파일을 드래그하세요",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDragging) MockPackColors.Primary else MockPackColors.TextSecondary
            )
            Text(
                text  = ".apk, .ipa 파일 지원",
                style = MaterialTheme.typography.bodySmall,
                color = MockPackColors.TextSecondary
            )
        }
    }
}
