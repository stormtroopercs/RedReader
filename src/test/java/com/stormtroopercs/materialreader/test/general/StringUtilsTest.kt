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
 * along with MaterialReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.test.general

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.StringUtils

class StringUtilsTest {

    @Test
    fun testRemovePrefix() {

        assertEquals(Optional.empty<String>(), StringUtils.removePrefix("abc", "def"))
        assertEquals(Optional.of("def"), StringUtils.removePrefix("abcdef", "abc"))
        assertEquals(Optional.of("abcdef"), StringUtils.removePrefix("abcdef", ""))
        assertEquals(Optional.of(""), StringUtils.removePrefix("123", "123"))
    }

    @Test
    fun testAsciiUppercase() {

        assertEquals("ABC123", StringUtils.asciiUppercase("abc123"))
        assertEquals("åBC123", StringUtils.asciiUppercase("åbc123"))
        assertEquals("", StringUtils.asciiUppercase(""))
    }

    @Test
    fun testAsciiLowercase() {

        assertEquals("abc123", StringUtils.asciiLowercase("ABC123"))
        assertEquals("Åbc123", StringUtils.asciiLowercase("ÅBC123"))
        assertEquals("", StringUtils.asciiLowercase(""))
    }

    @Test
    fun testJoin() {

        assertEquals("helloworld", StringUtils.join(
            mutableListOf("hello", "world"),
            ""
        ))

        assertEquals("hello,world", StringUtils.join(
            mutableListOf("hello", "world"),
            ","
        ))

        assertEquals("hello,world, abc ", StringUtils.join(
            mutableListOf("hello", "world", " abc "),
            ","
        ))

        assertEquals("hello", StringUtils.join(
            mutableListOf("hello"),
            ","
        ))

        assertEquals("hello", StringUtils.join(
            mutableListOf("hello"),
            ""
        ))

        assertEquals("", StringUtils.join(
            mutableListOf(""),
            ""
        ))

        assertEquals("abcdefabcdef", StringUtils.join(
            mutableListOf("", "", ""),
            "abcdef"
        ))
    }

    @Test
    fun testIsEmpty() {

        assertTrue(StringUtils.isEmpty(null))
        assertTrue(StringUtils.isEmpty(""))
        assertFalse(StringUtils.isEmpty(" "))
        assertFalse(StringUtils.isEmpty("\t"))
        assertFalse(StringUtils.isEmpty("\n"))
        assertFalse(StringUtils.isEmpty("\r"))
        assertFalse(StringUtils.isEmpty("a"))
    }
}
