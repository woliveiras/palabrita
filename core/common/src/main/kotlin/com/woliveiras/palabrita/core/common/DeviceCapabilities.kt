package com.woliveiras.palabrita.core.common

import android.app.ActivityManager
import android.content.Context

object DeviceCapabilities {

  fun getDeviceTier(context: Context): DeviceTier {
    val totalRamMb = getTotalRamMb(context)
    return when {
      totalRamMb >= 8192 -> DeviceTier.HIGH
      totalRamMb >= 4096 -> DeviceTier.MEDIUM
      else -> DeviceTier.LOW
    }
  }

  fun getTotalRamMb(context: Context): Long {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)
    return memInfo.totalMem / (1024 * 1024)
  }

  fun getAvailableStorageMb(context: Context): Long {
    val stat = android.os.StatFs(context.filesDir.absolutePath)
    return stat.availableBytes / (1024 * 1024)
  }
}
