package ai.amblify.app.work

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ai.amblify.app.data.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar

/**
 * WallpaperWorker runs periodically in the background via AndroidX WorkManager.
 * It selects the optimal ambient wallpaper based on the current time of day
 * (Dawn, Morning, Afternoon, Golden Hour, Night) or weather condition and applies
 * it using Android's native WallpaperManager.
 */
class WallpaperWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "AmblifyWallpaperWorker"
        const val WORK_NAME = "ai.amblify.ambient_wallpaper_sync"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting ambient wallpaper evaluation...")

            val repository = WallpaperRepository.getInstance(context)
            val settings = repository.getSettings()

            if (!settings.autoRotate) {
                Log.d(TAG, "Auto-rotate is disabled in settings. Skipping.")
                return@withContext Result.success()
            }

            // Determine appropriate ambient mood by time of day
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val targetTimeOfDay = when (currentHour) {
                in 5..7 -> "dawn"
                in 8..16 -> "morning"
                in 17..19 -> "golden_hour"
                in 20..22 -> "twilight"
                else -> "night"
            }

            Log.d(TAG, "Determined time of day mood: $targetTimeOfDay (hour: $currentHour)")

            // Fetch matched wallpaper from repository
            val wallpaper = repository.getWallpaperForMood(targetTimeOfDay)
                ?: repository.getRandomWallpaper()

            if (wallpaper != null) {
                Log.d(TAG, "Applying wallpaper: ${wallpaper.title} (${wallpaper.imageUrl})")
                val stream = URL(wallpaper.imageUrl).openStream()
                val bitmap = BitmapFactory.decodeStream(stream)

                if (bitmap != null) {
                    val wallpaperManager = WallpaperManager.getInstance(context)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val flags = when (settings.applyTarget) {
                            "home" -> WallpaperManager.FLAG_SYSTEM
                            "lock" -> WallpaperManager.FLAG_LOCK
                            else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                        }
                        wallpaperManager.setBitmap(bitmap, null, true, flags)
                    } else {
                        wallpaperManager.setBitmap(bitmap)
                    }

                    Log.i(TAG, "Successfully applied ambient wallpaper: ${wallpaper.title}")
                    return@withContext Result.success()
                } else {
                    Log.e(TAG, "Failed to decode bitmap from URL.")
                }
            }

            Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Error in WallpaperWorker execution", e)
            Result.retry()
        }
    }
}
