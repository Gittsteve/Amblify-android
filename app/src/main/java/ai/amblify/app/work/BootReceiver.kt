package ai.amblify.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ai.amblify.app.data.WallpaperRepository
import java.util.concurrent.TimeUnit

/**
 * BootReceiver triggers on ACTION_BOOT_COMPLETED to re-register the periodic
 * WorkManager ambient wallpaper sync after a device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AmblifyBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            Log.d(TAG, "Received ${intent.action}. Re-enqueuing WallpaperWorker...")

            val repository = WallpaperRepository.getInstance(context)
            val settings = repository.getSettings()

            if (settings.autoRotate) {
                scheduleAmbientWorker(context, settings.intervalMinutes, settings.requiresWifi)
            }
        }
    }

    private fun scheduleAmbientWorker(context: Context, intervalMinutes: Long, requiresWifi: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requiresWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
            intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WallpaperWorker.TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WallpaperWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )

        Log.i(TAG, "Successfully scheduled WallpaperWorker for every $intervalMinutes minutes.")
    }
}
