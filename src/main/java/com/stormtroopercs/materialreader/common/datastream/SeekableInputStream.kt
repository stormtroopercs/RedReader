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
package com.stormtroopercs.materialreader.common.datastream

import java.io.IOException
import java.io.InputStream

abstract class SeekableInputStream : InputStream() {
	private var mMark = 0

	abstract val position: Long

	@Throws(IOException::class)
	abstract fun seek(position: Long)

	override fun mark(readlimit: Int) {
		mMark = this.position.toInt()
	}

	@Throws(IOException::class)
	override fun reset() {
		seek(mMark.toLong())
	}

	override fun markSupported(): Boolean = true

	@Throws(IOException::class)
	abstract fun readRemainingAsBytes(
		callback: ByteArrayCallback,
	)
}
