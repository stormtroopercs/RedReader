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
package com.stormtroopercs.materialreader.reddit.url

import com.stormtroopercs.materialreader.reddit.PostSort
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.url.RedditURLParser.RedditURL

abstract class PostListingURL : RedditURL() {
    abstract fun after(after : RedditIdAndType): PostListingURL

    abstract fun limit(limit: Int?): PostListingURL

    open val order: PostSort?
        get() = null
}
