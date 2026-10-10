package com.ezmanw.mathong.ui.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.ezmanw.mathong.BuildConfig
import com.ezmanw.mathong.data.BoardBackground
import com.ezmanw.mathong.data.Settings
import com.ezmanw.mathong.data.ThemeMode
import com.ezmanw.mathong.data.TileTheme
import com.ezmanw.mathong.notify.Reminders
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ezmanw.mathong.game.TileKind
import com.ezmanw.mathong.ui.common.TilePreview
import com.ezmanw.mathong.ui.common.repository
import com.ezmanw.mathong.ui.common.BackTopBar
import com.ezmanw.mathong.ui.game.boardBrush
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repo = repository()
    val s by repo.settings.collectAsState(initial = Settings())
    val scope = rememberCoroutineScope()
    fun update(t: (Settings) -> Settings) { scope.launch { repo.updateSettings(t) } }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = androidx.compose.ui.platform.LocalContext.current
    var pickTime by remember { mutableStateOf(false) }
    fun enableReminder(h: Int = s.reminderHour, m: Int = s.reminderMinute) {
        update { it.copy(dailyReminder = true, reminderHour = h, reminderMinute = m) }
        Reminders.schedule(context, h, m)
    }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) enableReminder() }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { BackTopBar("Settings", onBack, scroll) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                Section("Appearance")
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = s.themeMode == m,
                                onClick = { update { it.copy(themeMode = m) } },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            ) { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow("Dynamic color", "Use colors from your wallpaper", s.dynamicColor) { v -> update { it.copy(dynamicColor = v) } }
                }

                Text("Tile style", Modifier.padding(start = 16.dp, top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TileTheme.entries.forEach { t ->
                        val selected = s.tileTheme == t
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    BorderStroke(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable { update { it.copy(tileTheme = t) } }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            TilePreview(TileKind(31), t, 40.dp, showIndex = s.showIndices, simple = s.simpleFaces)
                            Spacer(Modifier.height(4.dp))
                            Text(t.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Text("Board background", Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.bodyLarge)
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BoardBackground.entries.forEach { b ->
                        FilterChip(
                            selected = s.background == b,
                            onClick = { update { it.copy(background = b) } },
                            label = { Text(b.label) },
                            leadingIcon = {
                                Box(Modifier.size(18.dp).clip(CircleShape).background(boardBrush(b)))
                            },
                        )
                    }
                }
                Text("Tile faces", Modifier.padding(start = 16.dp, top = 8.dp), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(false to "Traditional", true to "Simple").forEach { (simple, label) ->
                        val selected = s.simpleFaces == simple
                        Column(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable { update { it.copy(simpleFaces = simple) } }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                listOf(22, 27, 32).forEach { TilePreview(TileKind(it), s.tileTheme, 30.dp, simple = simple) }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                SwitchRow("Corner labels", "Show a small rank label on every tile", s.showIndices) { v -> update { it.copy(showIndices = v) } }

                Section("Gameplay")
                SwitchRow("Dim blocked tiles", "Make free tiles easy to spot", s.highlightFree) { v -> update { it.copy(highlightFree = v) } }
                SwitchRow("Show matching tiles", "Outline free tiles that match your selection", s.showMatches) { v -> update { it.copy(showMatches = v) } }
                SwitchRow("Smart hints", "Hints look ahead and avoid dead ends", s.smartHints) { v -> update { it.copy(smartHints = v) } }
                SwitchRow("Auto shuffle", "Shuffle automatically when no moves remain", s.autoShuffle) { v -> update { it.copy(autoShuffle = v) } }
                SwitchRow("Show timer", null, s.showTimer) { v -> update { it.copy(showTimer = v) } }

                Section("Daily challenge")
                SwitchRow("Daily reminder", "Get a nudge if you haven't played today's challenge", s.dailyReminder) { on ->
                    if (on) {
                        if (Reminders.canNotify(context)) enableReminder()
                        else if (Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        update { it.copy(dailyReminder = false) }
                        Reminders.cancel(context)
                    }
                }
                if (s.dailyReminder) {
                    ListItem(
                        modifier = Modifier.clickable { pickTime = true },
                        headlineContent = { Text("Reminder time") },
                        supportingContent = { Text("%02d:%02d".format(s.reminderHour, s.reminderMinute)) },
                    )
                }

                Section("Feedback")
                SwitchRow("Sound effects", null, s.sound) { v -> update { it.copy(sound = v) } }
                SwitchRow("Haptic feedback", null, s.haptics) { v -> update { it.copy(haptics = v) } }

                Section("About")
                ListItem(
                    headlineContent = { Text("Mathong") },
                    supportingContent = { Text("Version ${BuildConfig.VERSION_NAME} · Mahjong Solitaire made with Material 3") },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (pickTime) {
        TimeDialog(s.reminderHour, s.reminderMinute, onDismiss = { pickTime = false }) { h, m ->
            pickTime = false
            enableReminder(h, m)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onPick: (Int, Int) -> Unit) {
    val state = androidx.compose.material3.rememberTimePickerState(hour, minute)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time") },
        text = { androidx.compose.material3.TimePicker(state) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onPick(state.hour, state.minute) }) { Text("OK") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}
