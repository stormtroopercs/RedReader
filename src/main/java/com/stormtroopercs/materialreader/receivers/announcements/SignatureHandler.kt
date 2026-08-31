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
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.receivers.announcements

import com.stormtroopercs.materialreader.common.HexUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.security.InvalidKeyException
import java.security.Key
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object SignatureHandler {
	private const val ALG = "EC"
	private const val SIGNATURE_ALG = "SHA256withECDSA"

	@JvmStatic
	fun keyToString(key: Key): String = HexUtils.toHex(key.getEncoded())

	@JvmStatic
	@Throws(NoSuchAlgorithmException::class, IOException::class, InvalidKeySpecException::class)
	fun stringToPrivateKey(input: String): PrivateKey = KeyFactory.getInstance(ALG)
		.generatePrivate(PKCS8EncodedKeySpec(HexUtils.fromHex(input)))

	@JvmStatic
	@Throws(NoSuchAlgorithmException::class, IOException::class, InvalidKeySpecException::class)
	fun stringToPublicKey(input: String): PublicKey = KeyFactory.getInstance(ALG)
		.generatePublic(X509EncodedKeySpec(HexUtils.fromHex(input)))

	@Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
	private fun sign(privateKey: PrivateKey, message: ByteArray): ByteArray {
		val signer = Signature.getInstance(SIGNATURE_ALG)

		signer.initSign(privateKey)
		signer.update(message)

		return signer.sign()
	}

	@Throws(
		NoSuchAlgorithmException::class,
		InvalidKeyException::class,
		SignatureException::class,
		SignatureInvalidException::class,
	)
	private fun verify(
		publicKey: PublicKey,
		message: ByteArray,
		signature: ByteArray,
	) {
		val signer = Signature.getInstance(SIGNATURE_ALG)

		signer.initVerify(publicKey)
		signer.update(message)

		if (!signer.verify(signature)) {
			throw SignatureInvalidException()
		}
	}

	@JvmStatic
	@Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, SignatureException::class)
	fun generateSignedPayload(
		privateKey: PrivateKey,
		message: ByteArray,
	): ByteArray {
		val signature = sign(privateKey, message)

		val result = ByteArrayOutputStream()

		val dos = DataOutputStream(result)

		try {
			dos.writeInt(message.size)
			dos.write(message)

			dos.writeInt(signature.size)
			dos.write(signature)

			dos.flush()
			dos.close()
		} catch (e: IOException) {
			throw RuntimeException(e)
		}

		return result.toByteArray()
	}

	@JvmStatic
	@Throws(
		NoSuchAlgorithmException::class,
		InvalidKeyException::class,
		SignatureException::class,
		IOException::class,
		SignatureInvalidException::class,
	)
	fun readAndVerifySignedPayload(
		publicKey: PublicKey,
		payload: ByteArray,
	): ByteArray {
		DataInputStream(ByteArrayInputStream(payload)).use { payloadStream ->
			val msgLength = payloadStream.readInt()
			val msg = ByteArray(msgLength)
			payloadStream.readFully(msg)

			val sigLength = payloadStream.readInt()

			val sig = ByteArray(sigLength)
			payloadStream.readFully(sig)

			// (any trailing bytes in payloadStream are safely ignored)
			verify(publicKey, msg, sig)
			return msg
		}
	}

	class SignatureInvalidException : Exception()
}
