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
package com.stormtroopercs.materialreader.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.General.dpToPixels
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.General

class LinkDetailsView @SuppressLint("ClickableViewAccessibility") constructor(
    context: Context,
    title: String,
    subtitle: String?
) : FrameLayout(context) {
    init {
        setClickable(true)

        val layout = LinearLayout(context)
        layout.setOrientation(LinearLayout.HORIZONTAL)
        addView(layout)
        val marginPx = dpToPixels(context, 10f)

        layout.setGravity(Gravity.CENTER_VERTICAL)

        val appearance = context.obtainStyledAttributes(intArrayOf(R.attr.rrIconLink))
        val icon = ImageView(context)
        icon.setImageDrawable(appearance.getDrawable(0))
        appearance.recycle()
        layout.addView(icon)
        (icon.getLayoutParams() as LinearLayout.LayoutParams).setMargins(
            marginPx,
            marginPx,
            marginPx,
            marginPx
        )

        val textLayout = LinearLayout(context)
        textLayout.setOrientation(LinearLayout.VERTICAL)
        layout.addView(textLayout)
        (textLayout.getLayoutParams() as LinearLayout.LayoutParams).setMargins(
            0,
            marginPx,
            marginPx,
            marginPx
        )

        val linkFontScale = PrefsUtility.appearance_fontscale_linkbuttons()

        run {
            val titleView = TextView(context)
            titleView.setText(title)
            titleView.setTextSize(15f * linkFontScale)
            textLayout.addView(titleView)
        }

        if (subtitle != null && title != subtitle) {
            val subtitleView = TextView(context)
            subtitleView.setText(subtitle)
            subtitleView.setTextSize(11f * linkFontScale)
            textLayout.addView(subtitleView)
        }

        val borderPx = dpToPixels(context, 2f).toFloat()

        val borderShape = RectShape()
        val border = ShapeDrawable(borderShape)
        border.getPaint().setColor(Color.argb(128, 128, 128, 128))
        border.getPaint().setStrokeWidth(borderPx)
        border.getPaint().setStyle(Paint.Style.STROKE)

        setBackground(border)

        setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            when (event!!.getActionMasked()) {
                MotionEvent.ACTION_DOWN -> {
                    layout.setBackgroundColor(Color.argb(50, 128, 128, 128))
                    invalidate()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    layout.setBackgroundColor(Color.TRANSPARENT)
                    invalidate()
                }
            }
            false
        })
    }
}
