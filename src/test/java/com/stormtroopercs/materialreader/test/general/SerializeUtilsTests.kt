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
import com.stormtroopercs.materialreader.common.SerializeUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class SerializeUtilsTests {

    private class DataHandler {

        private val mOutput = ByteArrayOutputStream()

        fun getOutput(): DataOutputStream {
            return DataOutputStream(mOutput)
        }

        fun getInput(): DataInputStream {
            return DataInputStream(ByteArrayInputStream(mOutput.toByteArray()))
        }
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testNull() {

        val dataHandler = DataHandler()

        SerializeUtils.serialize(dataHandler.getOutput(), null)

        Assert.assertNull(SerializeUtils.deserialize(dataHandler.getInput()))
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testByte() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 123.toByte())

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Byte)
        Assert.assertEquals(123.toByte(), result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testChar() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 123.toChar())

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Char)
        Assert.assertEquals(123.toChar(), result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testShort() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 123.toShort())

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Short)
        Assert.assertEquals(123.toShort(), result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testInt() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 123)

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Int)
        Assert.assertEquals(123, result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testLong() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 123L)

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Long)
        Assert.assertEquals(123L, result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testFloat() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 0.25f)

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Float)
        Assert.assertEquals(0.25f, result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testDouble() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), 0.25)

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Double)
        Assert.assertEquals(0.25, result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testTrue() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), true)

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Boolean)
        Assert.assertEquals(true, result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testFalse() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), false)

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is Boolean)
        Assert.assertEquals(false, result)
    }

    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testString() {

        val dataHandler = DataHandler()
        SerializeUtils.serialize(dataHandler.getOutput(), "Hello world")

        val result = SerializeUtils.deserialize(dataHandler.getInput())

        Assert.assertTrue(result is String)
        Assert.assertEquals("Hello world", result)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testSet() {

        val dataHandler = DataHandler()

        val input: MutableSet<Any?> = HashSet()
        input.add(12345)
        input.add("String value")
        input.add(null)

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableSet<Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
        Assert.assertEquals(input.size, result.size)

        Assert.assertTrue(result.contains(12345))
        Assert.assertTrue(result.contains("String value"))
        Assert.assertTrue(result.contains(null))
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testEmptySet() {

        val dataHandler = DataHandler()

        val input: MutableSet<Any?> = HashSet()

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableSet<Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
        Assert.assertEquals(input.size, result.size)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testList() {

        val dataHandler = DataHandler()

        val input: MutableList<Any?> = ArrayList()
        input.add(12345)
        input.add("String value")
        input.add(null)

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableList<Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
        Assert.assertEquals(input.size, result.size)

        Assert.assertEquals(12345, result[0])
        Assert.assertEquals("String value", result[1])
        Assert.assertNull(result[2])
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testEmptyList() {

        val dataHandler = DataHandler()

        val input: MutableList<Any?> = ArrayList()

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableList<Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
        Assert.assertEquals(input.size, result.size)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testMap() {

        val dataHandler = DataHandler()

        val input: MutableMap<Any?, Any?> = HashMap()
        input["first"] = 12345
        input["second"] = "String value"
        input[543] = null
        input[98.toByte()] = 0.25

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableMap<Any?, Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
        Assert.assertEquals(input.size, result.size)

        Assert.assertEquals(12345, result["first"])
        Assert.assertEquals("String value", result["second"])
        Assert.assertNull(result[543])
        Assert.assertEquals(0.25, result[98.toByte()])
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testEmptyMap() {

        val dataHandler = DataHandler()

        val input: MutableMap<Any?, Any?> = HashMap()

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableMap<Any?, Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
        Assert.assertEquals(input.size, result.size)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    @Throws(SerializeUtils.UnhandledTypeException::class, IOException::class)
    fun testMapContainingList() {

        val dataHandler = DataHandler()

        val input: MutableMap<Any?, Any?> = HashMap()
        input["first"] = listOf(0.25)

        SerializeUtils.serialize(dataHandler.getOutput(), input)

        val result =
            SerializeUtils.deserialize(dataHandler.getInput()) as MutableMap<Any?, Any?>

        Assert.assertNotNull(result)
        Assert.assertEquals(input, result)
    }
}
