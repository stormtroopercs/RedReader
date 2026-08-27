package org.quantumbadger.redreader.common

/**
 * Minimal LRU cache: newest insertions/re-lookups are evicted last.
 *
 * Sized by count, not bytes. Intended for decoded [android.graphics.ImageBitmap]s:
 * [sizeOf] may report an item's approximate memory footprint (pixel bytes) so the
 * cache self-limits against its [maxSize] byte budget.
 */
class LruCache<K, V>(
    /**
     * Maximum cost the cache may hold; when adding an item pushes the total
     * cost above this, least-recently-used entries are evicted until it fits.
     */
    val maxSize: Long,
    private val sizeOf: (V) -> Long = { 1L },
) {
    private val linked = LinkedHashMap<K, V>(32, 0.75f, true)
    private var size = 0L

    private val lock = Any()

    fun size(): Int = synchronized(lock) { linked.size }

    fun get(key: K): V? = synchronized(lock) { linked[key] }

    fun put(key: K, value: V) {
        synchronized(lock) {
            val itemCost = sizeOf(value)
            if (itemCost >= maxSize) return // a single item too large for the whole budget

            val previous = linked.put(key, value)
            size += itemCost - (if (previous != null) sizeOf(previous) else 0L)

            while (size > maxSize) {
                // accessOrder=true: the first key is the least recently used
                val eldestKey = linked.keys.iterator().next()
                val evicted = linked.remove(eldestKey)!!
                size -= sizeOf(evicted)
            }
        }
    }
}
