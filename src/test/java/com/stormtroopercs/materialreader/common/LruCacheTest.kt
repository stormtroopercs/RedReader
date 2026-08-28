package com.stormtroopercs.materialreader.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LruCacheTest {

    @Test
    fun `get misses on empty cache`() {
        val cache = LruCache<String, String>(maxSize = 3)
        assertNull(cache.get("a"))
    }

    @Test
    fun `put then get returns the value`() {
        val cache = LruCache<String, Int>(maxSize = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        assertEquals(1, cache.get("a"))
        assertEquals(2, cache.get("b"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `least recently used entry is evicted first`() {
        val cache = LruCache<String, Int>(maxSize = 2)
        cache.put("a", 1)
        cache.put("b", 2)
        cache.get("a") // 'a' is now the most recently used
        cache.put("c", 3) // must evict 'b'

        assertEquals(1, cache.get("a"))
        assertNull(cache.get("b"))
        assertEquals(3, cache.get("c"))
        assertEquals(2, cache.size())
    }

    @Test
    fun `sizeOf weights eviction by cost`() {
        val cache = LruCache<String, Int>(maxSize = 10) { value: Int -> value.toLong() }
        cache.put("a", 6)
        cache.put("b", 5) // cost 11 > 10, so 'a' (LRU) is evicted
        assertNull(cache.get("a"))
        assertEquals(5, cache.get("b"))
    }

    @Test
    fun `item larger than the whole budget is not cached`() {
        val cache = LruCache<String, Int>(maxSize = 4) { value: Int -> value.toLong() }
        cache.put("a", 5)
        assertNull(cache.get("a"))
        assertEquals(0, cache.size())
    }
}
