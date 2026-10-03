package ai.amblify.app.data

import android.content.Context
import android.content.SharedPreferences

data class WallpaperItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val timeOfDay: String,
    val dominantColor: String,
    val isOled: Boolean
)

data class AmblifySettings(
    val autoRotate: Boolean = true,
    val intervalMinutes: Long = 60,
    val syncWithTimeOfDay: Boolean = true,
    val applyTarget: String = "both",
    val requiresWifi: Boolean = true
)

class WallpaperRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("amblify_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var instance: WallpaperRepository? = null

        fun getInstance(context: Context): WallpaperRepository =
            instance ?: synchronized(this) {
                instance ?: WallpaperRepository(context.applicationContext).also { instance = it }
            }
    }

    fun getSettings(): AmblifySettings {
        return AmblifySettings(
            autoRotate = prefs.getBoolean("auto_rotate", true),
            intervalMinutes = prefs.getLong("interval_minutes", 60),
            syncWithTimeOfDay = prefs.getBoolean("sync_time", true),
            applyTarget = prefs.getString("apply_target", "both") ?: "both",
            requiresWifi = prefs.getBoolean("requires_wifi", true)
        )
    }

    fun saveSettings(settings: AmblifySettings) {
        prefs.edit()
            .putBoolean("auto_rotate", settings.autoRotate)
            .putLong("interval_minutes", settings.intervalMinutes)
            .putBoolean("sync_time", settings.syncWithTimeOfDay)
            .putString("apply_target", settings.applyTarget)
            .putBoolean("requires_wifi", settings.requiresWifi)
            .apply()
    }

    fun getCuratedWallpapers(): List<WallpaperItem> {
        return listOf(
            WallpaperItem(
                id = "cyber-01",
                title = "Neo-Tokyo Neon Rain",
                description = "Reflective cybernetic city streets during night rain",
                imageUrl = "https://images.unsplash.com/photo-1519501025264-65ba15a82390",
                timeOfDay = "night",
                dominantColor = "#4338ca",
                isOled = false
            ),
            WallpaperItem(
                id = "aurora-02",
                title = "Glacial Aurora Borealis",
                description = "Emerald cosmic ribbons dancing over alpine lake",
                imageUrl = "https://images.unsplash.com/photo-1531366936337-7c912a4589a7",
                timeOfDay = "night",
                dominantColor = "#065f46",
                isOled = false
            ),
            WallpaperItem(
                id = "dune-03",
                title = "Amber Dune Horizon",
                description = "Warm minimalist desert ridges at golden hour",
                imageUrl = "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9",
                timeOfDay = "golden_hour",
                dominantColor = "#b45309",
                isOled = false
            )
        )
    }

    fun getWallpaperForMood(mood: String): WallpaperItem? {
        return getCuratedWallpapers().firstOrNull { it.timeOfDay == mood }
    }

    fun getRandomWallpaper(): WallpaperItem? {
        return getCuratedWallpapers().randomOrNull()
    }
}
