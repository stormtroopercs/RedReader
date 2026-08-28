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
import com.stormtroopercs.materialreader.common.FileUtils
import com.stormtroopercs.materialreader.common.Optional

class FileUtilsTest {

    @Test
    fun testGetExtensionFromPath() {

        assertEquals(
            Optional.of("jpg"),
            FileUtils.getExtensionFromPath("image.jpg")
        )

        assertEquals(
            Optional.of("mp4"),
            FileUtils.getExtensionFromPath("path/segments/image.other.mp4")
        )

        assertEquals(
            Optional.of("jpg"),
            FileUtils.getExtensionFromPath("other.image.jpg")
        )

        assertEquals(
            Optional.empty<String>(),
            FileUtils.getExtensionFromPath("image")
        )

        assertEquals(
            Optional.empty<String>(),
            FileUtils.getExtensionFromPath("path/segments.test/image")
        )

        assertEquals(
            Optional.of("bmp"),
            FileUtils.getExtensionFromPath("path/segments.test/image.bmp")
        )

        assertEquals(
            Optional.empty<String>(),
            FileUtils.getExtensionFromPath("path/segments.test/image.")
        )

        assertEquals(
            Optional.empty<String>(),
            FileUtils.getExtensionFromPath("path/segments.test/.image")
        )

        assertEquals(
            Optional.empty<String>(),
            FileUtils.getExtensionFromPath("path/segments.test/.image.")
        )

        assertEquals(
            Optional.of("jpg"),
            FileUtils.getExtensionFromPath("path/segments.test/.image.jpg")
        )
    }
}
