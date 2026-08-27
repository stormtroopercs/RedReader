package org.quantumbadger.redreader.compose.net

import androidx.compose.ui.graphics.ImageBitmap
import org.quantumbadger.redreader.common.LruCache

/**
 * In-memory cache of decoded [ImageBitmap]s, keyed by URI + scale, so that a
 * Compose row recycled by a `LazyColumn` (or a screen re-entered after a
 * back-navigation) does not re-run the decode + scale step on every
 * recomposition.
 *
 * The on-DISK bytes are already cached by [CacheManager]; this caches only the
 * CPU- and memory-expensive *decoded* result.
 *
 * Cost model: an entry's cost is its pixel memory (`w * h * 4` bytes, ARGB_8888)
 * and the whole cache is bounded by [MAX_BYTES], so memory usage is
 * self-limiting regardless of image sizes.
 */
object ImageBitmapCache {

	/** Budget: 24 MiB of decoded pixels. */
	private const val MAX_BYTES = 24L * 1024 * 1024

	private val cache = LruCache<String, ImageBitmap>(maxSize = MAX_BYTES) { bitmap ->
		bitmap.width.toLong() * bitmap.height * 4L
	}

	fun key(uri: String, scaleToMaxAxis: Int): String = "$uri#scale$scaleToMaxAxis"

	fun get(uri: String, scaleToMaxAxis: Int): ImageBitmap? =
		cache.get(key(uri, scaleToMaxAxis))

	fun put(uri: String, scaleToMaxAxis: Int, bitmap: ImageBitmap) =
		cache.put(key(uri, scaleToMaxAxis), bitmap)

	fun size(): Int = cache.size()

	fun clear() = cache.clear()
}
