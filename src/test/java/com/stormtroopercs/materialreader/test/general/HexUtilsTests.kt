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

import org.junit.Assert
import org.junit.Test
import com.stormtroopercs.materialreader.common.HexUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale

class HexUtilsTests {

    @Test
    fun hexTestChars() {

        for (i in 0 until 16) {
            Assert.assertEquals(i, HexUtils.fromHex(String.format(Locale.US, "%X", i)[0]))
        }

        for (i in 0 until 16) {
            Assert.assertEquals(i, HexUtils.fromHex(String.format(Locale.US, "%x", i)[0]))
        }
    }

    @Test
    fun hexTest1() {

        val msg = "Hello World".toByteArray(StandardCharsets.UTF_8)

        val hexMsg = HexUtils.toHex(msg)

        Assert.assertEquals("48656C6C6F20576F726C64", hexMsg)

        Assert.assertArrayEquals(msg, HexUtils.fromHex(hexMsg))
        Assert.assertArrayEquals(msg, HexUtils.fromHex(hexMsg.lowercase()))
    }

    @Test
    fun hexTest2() {
        Assert.assertThrows(IOException::class.java) {
            HexUtils.fromHex("123")
        }
    }

    @Test
    fun hexTest3() {
        Assert.assertThrows(IOException::class.java) {
            HexUtils.fromHex("48656C6C6F20576F726CR4")
        }
    }
}
