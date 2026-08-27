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
package org.quantumbadger.redreader.reddit

import org.quantumbadger.redreader.common.StringUtils

enum class PostCommentSort(
    val key: String
) {
    BEST("confidence"),
    HOT("hot"),
    NEW("new"),
    OLD("old"),
    TOP("top"),
    CONTROVERSIAL("controversial"),
    QA("qa");

    companion object {
        fun lookup(name: String): PostCommentSort? {
            var name = name
            name = StringUtils.asciiUppercase(name)

            if (name == "CONFIDENCE") {
                return PostCommentSort.BEST // oh, reddit...
            }

            try {
                return valueOf(name)
            } catch (e: IllegalArgumentException) {
                return null
            }
        }
    }
}
