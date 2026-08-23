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
package org.quantumbadger.redreader

import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost

/**
 * Implemented by the host Activity that displays a post, so that tapping a post
 * (or its "comments" control) routes to the matching in-app listing. Relocated
 * to a top-level interface when the legacy listing stack was retired (it was
 * previously nested inside `views.RedditPostView`). The live `ImageViewActivity`
 * and `WebViewFragment` implement it; `RedditPostActions` casts the host Activity
 * to it to dispatch post-selected / post-comments-selected.
 */
interface PostSelectionListener {
    fun onPostSelected(post: RedditPreparedPost)

    fun onPostCommentsSelected(post: RedditPreparedPost)
}
