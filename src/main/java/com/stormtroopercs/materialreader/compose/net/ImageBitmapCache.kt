/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.stormtroopercs.materialreader.compose.net

import androidx.compose.ui.graphics.ImageBitmap
import com.stormtroopercs.materialreader.common.LruCache

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
