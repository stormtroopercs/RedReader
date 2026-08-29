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

/**
 * How the app's primary destinations are surfaced: a bottom navigation bar,
 * a left navigation drawer, or both. `BOTH` adds a drawer on top of the
 * bottom bar. (The reference's onboarding step 5 + settings.)
 */
enum class NavigationType(
	override val stringValue: String,
) : SerializableEnum<NavigationType> {
	BOTTOM("bottom"),
	DRAWER("drawer"),
	BOTH("both"),
	;

	companion object {
		val settingSerializer = EnumSettingSerializer(NavigationType.entries)
	}
}
