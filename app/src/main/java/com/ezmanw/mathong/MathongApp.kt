package com.ezmanw.mathong

import android.app.Application
import com.ezmanw.mathong.audio.SoundManager
import com.ezmanw.mathong.data.Repository

class MathongApp : Application() {
    val repository by lazy { Repository(this) }
    val sounds by lazy { SoundManager(this) }
}
