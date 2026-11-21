package com.example

import android.app.Application
import com.example.data.notifications.NotificationHelper
import com.example.workers.RadarScheduler

class LunoIntelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        RadarScheduler.scheduleAllLayers(this)
    }
}
