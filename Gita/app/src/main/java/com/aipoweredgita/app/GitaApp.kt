package com.aipoweredgita.app

import android.app.Application
import android.util.Log
import com.aipoweredgita.app.utils.DeviceTierDetector
import com.aipoweredgita.app.utils.DeviceProfile

class GitaApp : Application() {

    lateinit var deviceProfile: DeviceProfile
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val tier = DeviceTierDetector.detect(this)
        deviceProfile = DeviceProfile.from(tier)

        Log.d("GitaApp", "Device tier=$tier profile=$deviceProfile")
    }

    companion object {
        lateinit var instance: GitaApp
            private set
    }
}
