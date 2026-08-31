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

import com.stormtroopercs.materialreader.common.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionalTest {

	@Test
	fun testOptional() {
		assertEquals(Optional.empty<Any>(), Optional.empty<Any>())
		assertNotEquals(Optional.empty<Int>(), Optional.of(123))

		assertEquals(Optional.of(123), Optional.of(123))
		assertNotEquals(Optional.of(123), Optional.of(456))

		assertNotEquals(Optional.of<Any>(Object()), Optional.of<Any>(Object()))

		assertFalse(Optional.empty<Any>().isPresent)
		assertTrue(Optional.of(123).isPresent)

		assertEquals("Hello", Optional.of("Hello").get())
		assertThrows(Optional.OptionalHasNoValueException::class.java) {
			Optional.empty<Any>().get()
		}

		assertEquals("Hello", Optional.of("Hello").orElse("Alternative"))
		assertEquals("Alternative", Optional.empty<String>().orElse("Alternative"))

		assertEquals(Optional.empty<Any?>(), Optional.ofNullable<Any?>(null))
		assertEquals(Optional.of("Test"), Optional.ofNullable("Test"))

		assertThrows(RuntimeException::class.java) {
			Optional.empty<Any>().orThrow { RuntimeException() }
		}

		assertEquals("Test", Optional.of("Test").orThrow { RuntimeException() })
	}
}
