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
import org.junit.Test
import com.stormtroopercs.materialreader.common.StringUtils
import java.util.Locale

class GeneralTest {

    @Test
    fun testAsciiUppercase() {

        for (c in 0 until 128) {
            val ch = c.toChar()
            val str = "This is a test" + ch
            assertEquals(str.uppercase(Locale.ENGLISH), StringUtils.asciiUppercase(str))

            val str2 = "${ch}${ch}${ch}${ch}${ch}A${ch}A"
            assertEquals(str2.uppercase(Locale.ENGLISH), StringUtils.asciiUppercase(str2))
        }
    }
}
