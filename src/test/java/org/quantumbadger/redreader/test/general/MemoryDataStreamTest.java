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

package org.quantumbadger.redreader.test.general;

import org.junit.Assert;
import org.junit.Test;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.datastream.MemoryDataStream;

import java.io.IOException;

public class MemoryDataStreamTest {

	private void assertBytesStartsWith(final byte[] actual, final char... expected) {

		for(int i = 0; i < expected.length; i++) {
			Assert.assertEquals(expected[i], actual[i]);
		}
	}

	@Test
	public void test() throws IOException {

		final MemoryDataStream stream = new MemoryDataStream();

		Assert.assertEquals(0, stream.size());

		stream.writeBytes(new byte[] {'H', 'i'}, 0, 2);

		final byte[] buf = new byte[10];
		Assert.assertEquals(
				2,
				stream.blockingRead(0, buf, 0, buf.length));

		Assert.assertEquals('H', buf[0]);
		Assert.assertEquals('i', buf[1]);

		assertBytesStartsWith(buf, 'H', 'i');

		stream.writeBytes(new byte[] {'A'}, 0, 1);
		stream.writeBytes(new byte[] {'Z'}, 0, 1);

		Assert.assertEquals(
				4,
				stream.blockingRead(0, buf, 0, buf.length));

		assertBytesStartsWith(buf, 'H', 'i', 'A', 'Z');

		Assert.assertEquals(
				3,
				stream.blockingRead(1, buf, 0, buf.length));

		assertBytesStartsWith(buf, 'i', 'A', 'Z');

		stream.setComplete();

		Assert.assertEquals(
				"HiAZ",
				General.readWholeStreamAsUTF8(stream.getInputStream()));
	}
}
