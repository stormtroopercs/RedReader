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

package com.stormtroopercs.materialreader.settings.types

import com.stormtroopercs.materialreader.settings.types.EnumSettingSerializer
import com.stormtroopercs.materialreader.settings.types.SerializableEnum

/**
 * The card style of the post **list** feed (FINAL-DESIGN Phase 4.1, DESIGN
 * §4.3): one of the reference app's card modes. `SLIDES` is the swipe-feed
 * variant (one full-bleed post per screen, its own screen) and is not a
 * `PostCard` rendering itself — the Change-View sheet lists it so the user
 * can jump between the card modes and the slides feed.
 */
enum class PostViewMode(
	override val stringValue: String,
) : SerializableEnum<PostViewMode> {
	CARDS("cards"),
	SMALLER("smaller"),
	COMPACT("compact"),
	SIMPLE("simple"),
	LIST("list"),
	THUMB_LEFT("thumb_left"),
	THUMB_RIGHT("thumb_right"),
	HORIZONTAL("horizontal"),
	SLIDES("slides"),
	;

	companion object {
		val settingSerializer = EnumSettingSerializer(PostViewMode.entries)
	}
}

/**
 * The action a horizontal swipe on a post card performs (FINAL-DESIGN Phase
 * 4.3, DESIGN §17.7). The card's first/second/third swipe slots are persisted
 * independently; `NONE` is a deliberately empty slot.
 */
enum class PostSwipeAction(
	override val stringValue: String,
) : SerializableEnum<PostSwipeAction> {
	NONE("none"),
	UPVOTE("upvote"),
	DOWNVOTE("downvote"),
	HIDE("hide"),
	SAVE("save"),
	COMMENTS("comments"),
	;

	companion object {
		val settingSerializer = EnumSettingSerializer(PostSwipeAction.entries)
	}
}
