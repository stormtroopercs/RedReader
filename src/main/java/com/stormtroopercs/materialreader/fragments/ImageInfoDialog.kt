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
package com.stormtroopercs.materialreader.fragments

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.os.BundleCompat
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.image.ImageInfo
import java.util.Objects

class ImageInfoDialog : PropertiesDialog() {
	override fun getTitle(context: Context): String = context.getString(string.props_image_title)

	override fun prepare(
		context: BaseActivity,
		items: LinearLayout,
	) {
		val info = Objects.requireNonNull<ImageInfo>(
			BundleCompat.getParcelable<ImageInfo?>(
				requireArguments(),
				"info",
				ImageInfo::class.java,
			),
		)

		var first = true

		if (info.title != null && !info.title.trim { it <= ' ' }.isEmpty()) {
			items.addView(
				propView(
					context,
					string.props_title,
					info.title.trim { it <= ' ' },
					first,
				),
			)
			first = false
		}

		if (info.caption != null && !info.caption.trim { it <= ' ' }.isEmpty()) {
			items.addView(
				propView(
					context,
					string.props_caption,
					info.caption.trim { it <= ' ' },
					first,
				),
			)
			first = false
		}

		items.addView(propView(context, string.props_url, info.original.url.value, first))

		if (info.original.size != null) {
			items.addView(
				propView(
					context,
					string.props_resolution,
					info.original.size.width.toString() + " x " + info.original.size.height,
					false,
				),
			)
		}
	}

	companion object {
		fun newInstance(info: ImageInfo?): ImageInfoDialog {
			val pp = ImageInfoDialog()

			val args = Bundle()
			args.putParcelable("info", info)
			pp.setArguments(args)

			return pp
		}
	}
}
