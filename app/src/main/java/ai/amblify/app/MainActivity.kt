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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import ai.amblify.app.data.WallpaperItem
import ai.amblify.app.data.WallpaperRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : ComponentActivity() {

    private lateinit var repository: WallpaperRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = WallpaperRepository.getInstance(this)

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
                        onApplyWallpaper = { item -> applyWallpaper(item) }
                    )
                }
            }
        }
    }

    private fun applyWallpaper(item: WallpaperItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val stream = URL(item.imageUrl).openStream()
                val bitmap = BitmapFactory.decodeStream(stream)
                val manager = WallpaperManager.getInstance(this@MainActivity)
                manager.setBitmap(bitmap)

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
                        "Error setting wallpaper: ${e.message}",
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
    onApplyWallpaper: (WallpaperItem) -> Unit
) {
    val wallpapers = remember { repository.getCuratedWallpapers() }
    var selectedWallpaper by remember { mutableStateOf(wallpapers.firstOrNull()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Amblify AI Wallpapers") },
                actions = {
                    IconButton(onClick = { /* Open Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Preview Card
            selectedWallpaper?.let { current ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = current.imageUrl,
                            contentDescription = current.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(current.title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text(current.description, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                        }
                    }
                }
            }

            // Wallpapers Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(wallpapers) { item ->
                    Card(
                        modifier = Modifier
                            .size(100.dp, 160.dp)
                            .clickable { selectedWallpaper = item },
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

            // Apply Button
            selectedWallpaper?.let { current ->
                Button(
                    onClick = { onApplyWallpaper(current) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Wallpaper, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Dynamic Wallpaper Now")
                }
            }
        }
    }
}
