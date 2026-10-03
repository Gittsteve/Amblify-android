package ai.amblify.app

import android.app.WallpaperManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import ai.amblify.app.data.AmblifySettings
import ai.amblify.app.data.WallpaperItem
import ai.amblify.app.data.WallpaperRepository
import ai.amblify.app.work.WallpaperWorker
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var repository: WallpaperRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = WallpaperRepository.getInstance(this)
        scheduleFromSettings(repository.getSettings())

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF818CF8),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AmblifyMainScreen(
                        repository = repository,
                        onApplyWallpaper = ::applyWallpaper,
                        onSettingsChanged = ::scheduleFromSettings
                    )
                }
            }
        }
    }

    private fun scheduleFromSettings(settings: AmblifySettings) {
        val workManager = WorkManager.getInstance(this)

        if (!settings.autoRotate) {
            workManager.cancelUniqueWork(WallpaperWorker.WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (settings.requiresWifi)
                    NetworkType.UNMETERED
                else
                    NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(
            settings.intervalMinutes.coerceAtLeast(15),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WallpaperWorker.TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WallpaperWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun applyWallpaper(item: WallpaperItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                URL(item.imageUrl).openStream().use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                        ?: throw IllegalStateException("Image could not be decoded")

                    WallpaperManager
                        .getInstance(this@MainActivity)
                        .setBitmap(bitmap)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Applied: ${item.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "Couldn't set wallpaper: ${e.message ?: "unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmblifyMainScreen(
    repository: WallpaperRepository,
    onApplyWallpaper: (WallpaperItem) -> Unit,
    onSettingsChanged: (AmblifySettings) -> Unit
) {
    val wallpapers = remember {
        repository.getCuratedWallpapers()
    }

    var selectedWallpaper by remember {
        mutableStateOf(wallpapers.firstOrNull())
    }

    var settings by remember {
        mutableStateOf(repository.getSettings())
    }

    var showSettings by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Amblify")
                        Text(
                            "Ambient wallpapers, automatically.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettings = true }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Ambient Mode",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            if (settings.autoRotate)
                                "Automatically changing wallpapers"
                            else
                                "Manual mode",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Switch(
                        checked = settings.autoRotate,
                        onCheckedChange = {
                            settings = settings.copy(
                                autoRotate = it
                            )

                            repository.saveSettings(settings)
                            onSettingsChanged(settings)
                        }
                    )
                }
            }

            selectedWallpaper?.let { current ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        AsyncImage(
                            model = current.imageUrl,
                            contentDescription = current.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(
                                    Color.Black.copy(alpha = 0.68f)
                                )
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {

                            Text(
                                current.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                current.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            Text(
                "Explore wallpapers",
                style = MaterialTheme.typography.titleMedium
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(wallpapers) { item ->

                    Card(
                        modifier = Modifier
                            .size(100.dp, 150.dp)
                            .clickable {
                                selectedWallpaper = item
                            },
                        shape = RoundedCornerShape(16.dp)
                    ) {

                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            selectedWallpaper?.let { current ->

                Button(
                    onClick = {
                        onApplyWallpaper(current)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Icon(
                        Icons.Default.Wallpaper,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Text("Set Wallpaper")
                }
            }
        }
    }

    if (showSettings) {

        SettingsDialog(
            settings = settings,
            onDismiss = {
                showSettings = false
            },
            onSave = { newSettings ->

                settings = newSettings

                repository.saveSettings(newSettings)
                onSettingsChanged(newSettings)

                showSettings = false
            }
        )
    }
}

@Composable
private fun SettingsDialog(
    settings: AmblifySettings,
    onDismiss: () -> Unit,
    onSave: (AmblifySettings) -> Unit
) {

    var interval by remember {
        mutableStateOf(
            settings.intervalMinutes.toString()
        )
    }

    var wifiOnly by remember {
        mutableStateOf(settings.requiresWifi)
    }

    var syncTime by remember {
        mutableStateOf(settings.syncWithTimeOfDay)
    }

    var target by remember {
        mutableStateOf(settings.applyTarget)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Amblify Settings")
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                OutlinedTextField(
                    value = interval,
                    onValueChange = {
                        if (it.all(Char::isDigit)) {
                            interval = it
                        }
                    },
                    label = {
                        Text("Interval in minutes")
                    },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text("Wi-Fi only")

                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = {
                            wifiOnly = it
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text("Match time of day")

                    Switch(
                        checked = syncTime,
                        onCheckedChange = {
                            syncTime = it
                        }
                    )
                }

                Text("Apply wallpaper to")

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    FilterChip(
                        selected = target == "both",
                        onClick = {
                            target = "both"
                        },
                        label = {
                            Text("Both")
                        }
                    )

                    FilterChip(
                        selected = target == "home",
                        onClick = {
                            target = "home"
                        },
                        label = {
                            Text("Home")
                        }
                    )

                    FilterChip(
                        selected = target == "lock",
                        onClick = {
                            target = "lock"
                        },
                        label = {
                            Text("Lock")
                        }
                    )
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = {

                    val minutes =
                        (interval.toLongOrNull() ?: 60L)
                            .coerceAtLeast(15)

                    onSave(
                        settings.copy(
                            intervalMinutes = minutes,
                            requiresWifi = wifiOnly,
                            syncWithTimeOfDay = syncTime,
                            applyTarget = target
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
