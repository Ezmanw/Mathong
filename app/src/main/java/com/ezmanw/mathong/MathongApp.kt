package com.ezmanw.mathong

import android.app.Application
import com.ezmanw.mathong.audio.SoundManager
import com.ezmanw.mathong.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MathongApp : Application() {
    val repository by lazy { Repository(this) }
    val sounds by lazy { SoundManager(this) }
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { repository.syncCustomLayouts() }
    }
}
