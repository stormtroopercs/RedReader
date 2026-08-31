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
package com.stormtroopercs.materialreader.common

import android.content.Context
import java.io.IOException

/**
 * Small shared helpers for reading files from the app's [Context.getAssets].
 *
 * Introduced when the legacy `HtmlViewActivity.showAsset` launcher was retired
 * (41st increment) — the Settings "License" row now reads `license.html`
 * itself and opens the Compose `HtmlView` route.
 */
object AssetHelper {
	/**
	 * Read an asset file as a UTF-8 string, or `null` if it cannot be opened.
	 */
	fun loadAssetAsString(context: Context, filename: String): String? = try {
		context.assets.open(filename).bufferedReader().use { it.readText() }
	} catch (e: IOException) {
		null
	}
}
