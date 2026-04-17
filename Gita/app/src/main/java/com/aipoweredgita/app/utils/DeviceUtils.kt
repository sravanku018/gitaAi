package com.aipoweredgita.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build

enum class DeviceConfigCategory {
    LOW, MEDIUM, HIGH
}

object DeviceUtils {

    fun getModelName(): String = "${Build.MANUFACTURER} ${Build.MODEL}"

    fun getAndroidVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun getTotalRAM(context: Context): Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    fun getFormattedRAM(context: Context): String {
        val totalBytes = getTotalRAM(context)
        val gb = totalBytes / (1024 * 1024 * 1024.0)
        return "%.1f GB".format(gb)
    }

    fun getCPUInfo(): String {
        return "${Build.SUPPORTED_ABIS.joinToString(", ")}"
    }

    fun getDeviceCategory(context: Context): DeviceConfigCategory {
        val ramGb = getTotalRAM(context) / (1024 * 1024 * 1024.0)
        return when {
            ramGb < 4.0 -> DeviceConfigCategory.LOW
            ramGb < 8.0 -> DeviceConfigCategory.MEDIUM
            else -> DeviceConfigCategory.HIGH
        }
    }
}
