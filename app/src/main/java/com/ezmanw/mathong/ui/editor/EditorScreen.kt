package com.ezmanw.mathong.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ezmanw.mathong.game.Layouts

private const val ASPECT = 1.32f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    vm: EditorViewModel = viewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(s.message) {
        s.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.id == null) "New layout" else "Edit layout") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = vm::undo, enabled = s.undo.isNotEmpty()) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                    IconButton(onClick = { vm.save {} }, enabled = !s.busy) { Icon(Icons.Filled.Check, "Save") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Start from a layout…") }, onClick = { menu = false; templates = true })
                            DropdownMenuItem(text = { Text("Clear this layer") }, onClick = { menu = false; vm.clearLayer() })
                            DropdownMenuItem(text = { Text("Clear everything") }, onClick = { menu = false; vm.clearAll() })
                            if (s.id != null) {
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Delete layout") }, onClick = { menu = false; confirmDelete = true })
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = s.name,
                    onValueChange = vm::setName,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.count} tiles", style = MaterialTheme.typography.titleMedium)
                    Text(
                        s.quickProblem ?: "${s.layers} layer${if (s.layers == 1) "" else "s"} · ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (s.quickProblem != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Layers, "Layer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                for (z in 0 until MAX_LAYERS) {
                    val n = s.slots.count { it.z == z }
                    FilterChip(
                        selected = s.layer == z,
                        onClick = { vm.setLayer(z) },
                        label = { Text(if (n > 0) "${z + 1} ($n)" else "${z + 1}") },
                    )
                }
            }
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = s.mirror, onClick = vm::toggleMirror, label = { Text("Mirror") }, leadingIcon = { Icon(Icons.Filled.Flip, null, Modifier.size(18.dp)) })
                FilterChip(selected = s.halfStep, onClick = vm::toggleHalf, label = { Text("Half steps") })
            }
            EditorCanvas(s, vm::tap, Modifier.weight(1f).fillMaxWidth().padding(12.dp))
            Text(
                "Tap to place or remove tiles on the selected layer. Lower layers are shown faded.",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { vm.save(onPlay) },
                enabled = !s.busy && s.quickProblem == null,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (s.busy) "Checking…" else "Save & play")
            }
        }
    }

    if (templates) {
        AlertDialog(
            onDismissRequest = { templates = false },
            title = { Text("Start from a layout") },
            text = {
                Column(Modifier.horizontalScroll(rememberScrollState())) {
                    Layouts.all.chunked(2).forEach { row ->
                        Row {
                            row.forEach { l ->
                                TextButton(onClick = { templates = false; vm.loadTemplate(l) }) { Text(l.name) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { templates = false }) { Text("Cancel") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${s.name}\"?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun EditorCanvas(s: EditorState, onTap: (Float, Float) -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val tileW = minOf(maxW / (GRID_W / 2f), maxH / (GRID_H / 2f * ASPECT))
        val uw = tileW / 2
        val uh = tileW * ASPECT / 2
        val density = androidx.compose.ui.platform.LocalDensity.current
        Box(
            Modifier
                .size(with(density) { (uw * GRID_W).toDp() }, with(density) { (uh * GRID_H).toDp() })
                .background(cs.surfaceContainerLow, MaterialTheme.shapes.medium)
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(uw, uh) {
                        detectTapGestures { p -> onTap(p.x / uw, p.y / uh) }
                    },
            ) {
                // Half-unit dot grid.
                for (gx in 0..GRID_W step 2) for (gy in 0..GRID_H step 2) {
                    drawCircle(cs.outlineVariant, 1.5f, Offset(gx * uw, gy * uh))
                }
                if (s.mirror) {
                    drawLine(
                        cs.tertiary.copy(alpha = 0.6f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height),
                        strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    )
                }
                val sorted = s.slots.sortedWith(compareBy({ it.z }, { it.x }, { it.y }))
                val inset = tileW * 0.05f
                for (t in sorted) {
                    val o = Offset(t.x * uw + inset, t.y * uh + inset)
                    val sz = Size(tileW - inset * 2, tileW * ASPECT - inset * 2)
                    val r = CornerRadius(tileW * 0.15f)
                    when {
                        t.z < s.layer -> {
                            drawRoundRect(cs.secondaryContainer.copy(alpha = 0.55f), o, sz, r)
                            drawRoundRect(cs.outline.copy(alpha = 0.4f), o, sz, r, style = Stroke(1.5f))
                        }
                        t.z == s.layer -> {
                            drawRoundRect(cs.primary.copy(alpha = 0.35f), o + Offset(tileW * 0.06f, tileW * 0.06f), sz, r)
                            drawRoundRect(cs.primaryContainer, o, sz, r)
                            drawRoundRect(cs.primary, o, sz, r, style = Stroke(2.5f))
                        }
                        else -> drawRoundRect(cs.outline.copy(alpha = 0.25f), o, sz, r, style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                    }
                }
            }
        }
    }
}
