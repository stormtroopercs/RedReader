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
package com.stormtroopercs.materialreader.test.announcements

import org.junit.Assert
import org.junit.Test
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.time.TimeDuration
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.receivers.announcements.Announcement
import com.stormtroopercs.materialreader.receivers.announcements.Payload
import java.io.IOException

class AnnouncementTests {

    @Test
    @Throws(IOException::class)
    fun announcementTest() {

        val payload = Announcement.create(
            "test_id",
            "myTitle",
            "my message",
            UriString("https://my_url"),
            TimeDuration.ms(100000)
        ).toPayload().toBytes()

        val estUntil = TimestampUTC.now().add(TimeDuration.ms(100000))

        val reinflated = Announcement.fromPayload(Payload.fromBytes(payload))

        Assert.assertEquals("test_id", reinflated.id)
        Assert.assertEquals("myTitle", reinflated.title)
        Assert.assertEquals("my message", reinflated.message)
        Assert.assertEquals(UriString("https://my_url"), reinflated.url)

        Assert.assertFalse(estUntil.isLessThan(reinflated.showUntil))
        Assert.assertTrue(estUntil.subtract(TimeDuration.secs(1)).isLessThan(reinflated.showUntil))
        Assert.assertFalse(reinflated.isExpired)
    }

    @Test
    @Throws(IOException::class)
    fun announcementTestNullMessage() {

        val payload = Announcement.create(
            "test_id",
            "myTitle",
            null,
            UriString("https://my_url"),
            TimeDuration.ms(100000)
        ).toPayload().toBytes()

        val estUntil = TimestampUTC.now().add(TimeDuration.ms(100000))

        val reinflated = Announcement.fromPayload(Payload.fromBytes(payload))

        Assert.assertEquals("test_id", reinflated.id)
        Assert.assertEquals("myTitle", reinflated.title)
        Assert.assertNull(reinflated.message)
        Assert.assertEquals(UriString("https://my_url"), reinflated.url)

        Assert.assertFalse(estUntil.isLessThan(reinflated.showUntil))
        Assert.assertTrue(estUntil.subtract(TimeDuration.secs(1)).isLessThan(reinflated.showUntil))
        Assert.assertFalse(reinflated.isExpired)
    }
}
