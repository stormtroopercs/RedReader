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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.receivers.announcements

import org.quantumbadger.redreader.common.HexUtils
import org.quantumbadger.redreader.receivers.announcements.SignatureHandler.SignatureInvalidException
import java.io.IOException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SignatureException

object SignedDataSerializer {
    private const val MARKER_START = "START"
    private const val MARKER_END = "END"

    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
    fun serialize(
        privateKey: PrivateKey,
        data: ByteArray
    ): String {
        return (MARKER_START
                + HexUtils.toHex(SignatureHandler.generateSignedPayload(privateKey, data))
                + MARKER_END)
    }

    @JvmStatic
    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        IOException::class,
        SignatureInvalidException::class
    )
    fun deserialize(
        publicKey: PublicKey,
        data: String
    ): ByteArray {
        val startMarkerIndex = data.indexOf(MARKER_START)
        val endMarkerIndex = data.indexOf(MARKER_END)

        if (startMarkerIndex == -1) {
            throw IOException("Start marker not found")
        }

        if (endMarkerIndex == -1) {
            throw IOException("End marker not found")
        }

        val start = startMarkerIndex + MARKER_START.length
        val length = endMarkerIndex - start

        if (length < 0) {
            throw IOException("Negative length")
        }

        val hexData = data.substring(start, endMarkerIndex)

        val signedPayload = HexUtils.fromHex(hexData)

        return SignatureHandler.readAndVerifySignedPayload(publicKey, signedPayload)
    }
}
