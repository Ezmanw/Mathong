package com.ezmanw.mathong.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.ezmanw.mathong.game.Layout
import com.ezmanw.mathong.game.LayoutRegistry
import com.ezmanw.mathong.game.Slot

private val Context.dataStore by preferencesDataStore(name = "mathong")

class Repository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val store get() = context.dataStore

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val tileTheme = stringPreferencesKey("tile_theme")
        val background = stringPreferencesKey("background")
        val showIndices = booleanPreferencesKey("show_indices")
        val highlightFree = booleanPreferencesKey("highlight_free")
        val sound = booleanPreferencesKey("sound")
        val haptics = booleanPreferencesKey("haptics")
        val showTimer = booleanPreferencesKey("show_timer")
        val smartHints = booleanPreferencesKey("smart_hints")
        val autoShuffle = booleanPreferencesKey("auto_shuffle")
        val showMatches = booleanPreferencesKey("show_matches")
        val simpleFaces = booleanPreferencesKey("simple_faces")
        val dailyReminder = booleanPreferencesKey("daily_reminder")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val stats = stringPreferencesKey("stats")
        val savedGame = stringPreferencesKey("saved_game")
        val lastLayout = stringPreferencesKey("last_layout")
        val seenTutorial = booleanPreferencesKey("seen_tutorial")
        val customLayouts = stringPreferencesKey("custom_layouts")
    }

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default

    val settings: Flow<Settings> = store.data.map { p ->
        val d = Settings()
        Settings(
            themeMode = p.enum(Keys.themeMode, d.themeMode),
            dynamicColor = p[Keys.dynamicColor] ?: d.dynamicColor,
            tileTheme = p.enum(Keys.tileTheme, d.tileTheme),
            background = p.enum(Keys.background, d.background),
            showIndices = p[Keys.showIndices] ?: d.showIndices,
            highlightFree = p[Keys.highlightFree] ?: d.highlightFree,
            sound = p[Keys.sound] ?: d.sound,
            haptics = p[Keys.haptics] ?: d.haptics,
            showTimer = p[Keys.showTimer] ?: d.showTimer,
            smartHints = p[Keys.smartHints] ?: d.smartHints,
            autoShuffle = p[Keys.autoShuffle] ?: d.autoShuffle,
            showMatches = p[Keys.showMatches] ?: d.showMatches,
            simpleFaces = p[Keys.simpleFaces] ?: d.simpleFaces,
            dailyReminder = p[Keys.dailyReminder] ?: d.dailyReminder,
            reminderHour = p[Keys.reminderHour] ?: d.reminderHour,
            reminderMinute = p[Keys.reminderMinute] ?: d.reminderMinute,
        )
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        val s = transform(settings.first())
        store.edit { p ->
            p[Keys.themeMode] = s.themeMode.name
            p[Keys.dynamicColor] = s.dynamicColor
            p[Keys.tileTheme] = s.tileTheme.name
            p[Keys.background] = s.background.name
            p[Keys.showIndices] = s.showIndices
            p[Keys.highlightFree] = s.highlightFree
            p[Keys.sound] = s.sound
            p[Keys.haptics] = s.haptics
            p[Keys.showTimer] = s.showTimer
            p[Keys.smartHints] = s.smartHints
            p[Keys.autoShuffle] = s.autoShuffle
            p[Keys.showMatches] = s.showMatches
            p[Keys.simpleFaces] = s.simpleFaces
            p[Keys.dailyReminder] = s.dailyReminder
            p[Keys.reminderHour] = s.reminderHour
            p[Keys.reminderMinute] = s.reminderMinute
        }
    }

    val stats: Flow<Stats> = store.data.map { p ->
        p[Keys.stats]?.let { runCatching { json.decodeFromString<Stats>(it) }.getOrNull() } ?: Stats()
    }

    suspend fun updateStats(transform: (Stats) -> Stats): Stats {
        var result = Stats()
        store.edit { p ->
            val cur = p[Keys.stats]?.let { runCatching { json.decodeFromString<Stats>(it) }.getOrNull() } ?: Stats()
            result = transform(cur)
            p[Keys.stats] = json.encodeToString(Stats.serializer(), result)
        }
        return result
    }

    suspend fun resetStats() = store.edit { it.remove(Keys.stats) }

    val savedGame: Flow<SavedGame?> = store.data.map { p ->
        p[Keys.savedGame]?.let { runCatching { json.decodeFromString<SavedGame>(it) }.getOrNull() }
    }

    suspend fun saveGame(game: SavedGame?) = store.edit { p ->
        if (game == null) p.remove(Keys.savedGame)
        else p[Keys.savedGame] = json.encodeToString(SavedGame.serializer(), game)
    }

    val lastLayout: Flow<String?> = store.data.map { it[Keys.lastLayout] }
    suspend fun setLastLayout(id: String) = store.edit { it[Keys.lastLayout] = id }

    val seenTutorial: Flow<Boolean> = store.data.map { it[Keys.seenTutorial] ?: false }
    suspend fun setSeenTutorial() = store.edit { it[Keys.seenTutorial] = true }

    val customLayouts: Flow<List<CustomLayoutData>> = store.data.map { p ->
        p[Keys.customLayouts]?.let {
            runCatching { json.decodeFromString(ListSerializer(CustomLayoutData.serializer()), it) }.getOrNull()
        } ?: emptyList()
    }

    private suspend fun editCustom(transform: (List<CustomLayoutData>) -> List<CustomLayoutData>) {
        store.edit { p ->
            val cur = p[Keys.customLayouts]?.let {
                runCatching { json.decodeFromString(ListSerializer(CustomLayoutData.serializer()), it) }.getOrNull()
            } ?: emptyList()
            p[Keys.customLayouts] = json.encodeToString(ListSerializer(CustomLayoutData.serializer()), transform(cur))
        }
        syncCustomLayouts()
    }

    suspend fun saveCustomLayout(data: CustomLayoutData) = editCustom { list ->
        if (list.any { it.id == data.id }) list.map { if (it.id == data.id) data else it } else list + data
    }

    suspend fun deleteCustomLayout(id: String) = editCustom { list -> list.filterNot { it.id == id } }

    /** Pushes stored custom layouts into [LayoutRegistry]. */
    suspend fun syncCustomLayouts() {
        val layouts = customLayouts.first().mapNotNull { it.toLayout() }
        LayoutRegistry.setCustom(layouts)
    }
}

fun CustomLayoutData.toLayout(): Layout? {
    if (slots.size < 6 || slots.size % 3 != 0) return null
    val list = slots.chunked(3).map { Slot(it[0], it[1], it[2]) }
    return Layout(id, name, LayoutRegistry.estimateDifficulty(list), list)
}
