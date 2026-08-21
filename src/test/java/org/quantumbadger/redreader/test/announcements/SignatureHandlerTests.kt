/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.test.announcements

import org.junit.Assert
import org.junit.Test
import org.quantumbadger.redreader.receivers.announcements.SignatureHandler
import java.nio.charset.StandardCharsets
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec

class SignatureHandlerTests {

    companion object {

        @JvmStatic
        @Throws(NoSuchAlgorithmException::class, InvalidAlgorithmParameterException::class)
        fun generateKeyPair(): KeyPair {

            val keyGen = KeyPairGenerator.getInstance("EC")

            keyGen.initialize(ECGenParameterSpec("secp256r1"), SecureRandom())

            return keyGen.generateKeyPair()
        }
    }

    @Test
    @Throws(Exception::class)
    fun signTest1() {

        val keyPair = generateKeyPair()

        val msg = "Hello World".toByteArray(StandardCharsets.UTF_8)

        val payload = SignatureHandler.generateSignedPayload(keyPair.private, msg)

        Assert.assertArrayEquals(
            msg,
            SignatureHandler.readAndVerifySignedPayload(keyPair.public, payload)
        )
    }

    @Test
    @Throws(Exception::class)
    fun signTest2() {

        val keyPair = generateKeyPair()

        val msg = "Hello World".toByteArray(StandardCharsets.UTF_8)

        val payload = SignatureHandler.generateSignedPayload(keyPair.private, msg)

        // The message starts at payload[4]
        Assert.assertArrayEquals(msg, payload.copyOfRange(4, 4 + msg.size))

        // Corrupt the message
        payload[6] = (payload[6] + 1).toByte()

        Assert.assertThrows(
            SignatureHandler.SignatureInvalidException::class.java,
            { SignatureHandler.readAndVerifySignedPayload(keyPair.public, payload) }
        )
    }

    @Test
    @Throws(Exception::class)
    fun signTest3() {

        val keyPair = generateKeyPair()
        val keyPair2 = generateKeyPair()

        val msg = "Hello World".toByteArray(StandardCharsets.UTF_8)

        val payload = SignatureHandler.generateSignedPayload(keyPair.private, msg)

        Assert.assertThrows(
            SignatureHandler.SignatureInvalidException::class.java,
            { SignatureHandler.readAndVerifySignedPayload(keyPair2.public, payload) }
        )
    }

    @Test
    @Throws(Exception::class)
    fun keyTest() {

        val keyPair = generateKeyPair()

        val pubKeyStr = SignatureHandler.keyToString(keyPair.public)
        val privKeyStr = SignatureHandler.keyToString(keyPair.private)

        val msg = "Testing 123".toByteArray(StandardCharsets.UTF_8)

        Assert.assertArrayEquals(msg, SignatureHandler.readAndVerifySignedPayload(
            SignatureHandler.stringToPublicKey(pubKeyStr),
            SignatureHandler.generateSignedPayload(keyPair.private, msg)
        ))

        Assert.assertArrayEquals(msg, SignatureHandler.readAndVerifySignedPayload(
            keyPair.public,
            SignatureHandler.generateSignedPayload(
                SignatureHandler.stringToPrivateKey(privKeyStr),
                msg
            )
        ))
    }
}
