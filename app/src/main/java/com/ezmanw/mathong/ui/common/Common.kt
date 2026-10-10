package com.ezmanw.mathong.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.MathongApp
import com.ezmanw.mathong.data.Repository
import com.ezmanw.mathong.data.TileTheme
import com.ezmanw.mathong.game.Geometry
import com.ezmanw.mathong.game.Layout
import com.ezmanw.mathong.game.TileKind
import com.ezmanw.mathong.ui.tiles.GlyphCache
import com.ezmanw.mathong.ui.tiles.drawTile
import com.ezmanw.mathong.ui.tiles.palette

@Composable
fun repository(): Repository = (LocalContext.current.applicationContext as MathongApp).repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    LargeTopAppBar(
        title = { Text(title) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        scrollBehavior = scrollBehavior,
        actions = actions,
    )
}

/** A single decorative tile. */
@Composable
fun TilePreview(kind: TileKind, theme: TileTheme, width: Dp, modifier: Modifier = Modifier, showIndex: Boolean = false, simple: Boolean = false) {
    val measurer = rememberTextMeasurer()
    val glyphs = remember(measurer) { GlyphCache(measurer) }
    Canvas(modifier.size(width * 1.15f, width * 1.32f * 1.12f)) {
        val w = size.width / 1.15f
        val h = w * 1.32f
        drawTile(kind, Offset.Zero, Size(w, h), w * 0.13f, theme.palette(), glyphs, showIndex, simple = simple)
    }
}

/** Miniature top-down rendering of a layout. */
@Composable
fun LayoutThumbnail(layout: Layout, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val geo = remember(layout) { Geometry.of(layout) }
    Canvas(modifier) {
        val uw = (layout.maxX - layout.minX) / 2f
        val uh = (layout.maxY - layout.minY) / 2f
        val d = 0.12f
        val tw = minOf(size.width / (uw + d * (layout.maxZ + 1)), size.height / (uh * 1.3f + d * (layout.maxZ + 1)))
        val th = tw * 1.3f
        val ox = (size.width - uw * tw) / 2 + tw * d * layout.maxZ / 2
        val oy = (size.height - uh * th) / 2 + tw * d * layout.maxZ / 2
        val order = layout.slots.indices.sortedWith(compareBy({ layout.slots[it].z }, { layout.slots[it].x }, { layout.slots[it].y }))
        for (i in order) {
            val s = layout.slots[i]
            val o = Offset(ox + (s.x - layout.minX) * tw / 2 - s.z * tw * d, oy + (s.y - layout.minY) * th / 2 - s.z * tw * d)
            val t = if (layout.maxZ == 0) 0f else s.z.toFloat() / layout.maxZ
            val face = androidx.compose.ui.graphics.lerp(cs.secondaryContainer, cs.primary, t * 0.75f)
            drawRoundRect(cs.onSurface.copy(alpha = 0.25f), o + Offset(tw * d, tw * d), Size(tw * 0.96f, th * 0.96f), CornerRadius(tw * 0.15f))
            drawRoundRect(face, o, Size(tw * 0.96f, th * 0.96f), CornerRadius(tw * 0.15f))
            if (geo.above[i].isEmpty()) {
                drawRoundRect(cs.surface.copy(alpha = 0.35f), o, Size(tw * 0.96f, th * 0.3f), CornerRadius(tw * 0.15f))
            }
        }
    }
}

val SmallGap = 8.dp
