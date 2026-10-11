package com.gymlog.app.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Descarrega imagens com cache em memória e em disco (cacheDir/img). */
object ImageCache {
    private val mem = LruCache<String, ImageBitmap>(24)

    suspend fun load(ctx: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        mem.get(url)?.let { return@withContext it }
        val name = MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        val file = File(ctx.cacheDir, "img/$name")
        val bytes = if (file.exists()) file.readBytes() else runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000
            c.readTimeout = 20_000
            if (c.responseCode != 200) null else c.inputStream.use { it.readBytes() }
        }.getOrNull()?.also { file.parentFile?.mkdirs(); file.writeBytes(it) }
        bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }?.also { mem.put(url, it) }
    }
}

/**
 * Mostra as fotos de um exercício alternando entre elas (efeito tipo GIF).
 * Se não houver internet nem cache, não mostra nada.
 */
@Composable
fun AnimatedExerciseImages(urls: List<String>, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val frames by produceState<List<ImageBitmap>?>(null, urls) {
        value = urls.mapNotNull { ImageCache.load(ctx, it) }
    }
    var index by remember(urls) { mutableIntStateOf(0) }
    val list = frames
    LaunchedEffect(list) {
        if (list != null && list.size > 1) while (true) {
            delay(1100)
            index = (index + 1) % list.size
        }
    }
    when {
        list == null -> Box(
            modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)).background(Color.White),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(Modifier.size(28.dp)) }
        list.isEmpty() -> Unit
        else -> Box(
            modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(12.dp)).background(Color.White),
        ) {
            Image(
                bitmap = list[index % list.size], contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
