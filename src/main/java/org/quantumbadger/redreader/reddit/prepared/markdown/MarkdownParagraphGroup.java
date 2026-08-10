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
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.reddit.prepared.markdown

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.BufferType
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.Fonts
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser.MarkdownParagraphType
import org.quantumbadger.redreader.views.LinkDetailsView
import org.quantumbadger.redreader.views.LinkifiedTextView
import kotlin.math.min

class MarkdownParagraphGroup(private val paragraphs: Array<MarkdownParagraph>) {
    fun buildView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): ViewGroup {
        val dpScale = activity.getResources().getDisplayMetrics().density

        val paragraphSpacing = (dpScale * 6).toInt()
        val codeLineSpacing = (dpScale * 3).toInt()
        val quoteBarWidth = (dpScale * 3).toInt()
        val maxQuoteLevel = 5

        val layout = LinearLayout(activity)
        layout.setOrientation(LinearLayout.VERTICAL)

        for (paragraph in paragraphs) {
            val tv: TextView = LinkifiedTextView(activity)
            tv.setText(paragraph.spanned, BufferType.SPANNABLE)

            if (textColor != null) {
                tv.setTextColor(textColor)
            }
            if (textSize != null) {
                tv.setTextSize(textSize)
            }

            when (paragraph.type) {
                MarkdownParagraphType.BULLET -> {
                    val bulletItem = LinearLayout(activity)
                    val paddingPx = dpToPixels(activity, 6f)
                    bulletItem.setPadding(paddingPx, paddingPx, paddingPx, 0)

                    val bullet = TextView(activity)
                    bullet.setText("•   ")
                    if (textSize != null) {
                        bullet.setTextSize(textSize)
                    }

                    bulletItem.addView(bullet)
                    bulletItem.addView(tv)

                    layout.addView(bulletItem)

                    (bulletItem.getLayoutParams() as MarginLayoutParams).leftMargin
                    = (dpScale * (if (paragraph.level == 0) 12 else 24)).toInt()
                }

                MarkdownParagraphType.NUMBERED -> {
                    val numberedItem = LinearLayout(activity)
                    val paddingPx = dpToPixels(activity, 6f)
                    numberedItem.setPadding(paddingPx, paddingPx, paddingPx, 0)

                    val number = TextView(activity)
                    number.setText(paragraph.number.toString() + ".   ")
                    if (textSize != null) {
                        number.setTextSize(textSize)
                    }

                    numberedItem.addView(number)
                    numberedItem.addView(tv)

                    layout.addView(numberedItem)

                    (numberedItem.getLayoutParams() as MarginLayoutParams).leftMargin
                    = (dpScale * (if (paragraph.level == 0) 12 else 24)).toInt()
                }

                MarkdownParagraphType.CODE -> {
                    tv.setTypeface(Fonts.getVeraMonoOrAlternative())
                    tv.setText(
                        paragraph.raw.arr,
                        paragraph.raw.start,
                        paragraph.raw.length
                    )
                    layout.addView(tv)

                    if (paragraph.parent != null) {
                        (tv.getLayoutParams() as MarginLayoutParams).topMargin
                        = if (paragraph.parent.type
                            == MarkdownParagraphType.CODE
                        )
                            codeLineSpacing
                        else
                            paragraphSpacing
                    }

                    (tv.getLayoutParams() as MarginLayoutParams).leftMargin = (dpScale * 6).toInt()
                }

                MarkdownParagraphType.HEADER -> {
                    val underlinedText =
                        SpannableString(paragraph.spanned)
                    underlinedText.setSpan(
                        UnderlineSpan(),
                        0,
                        underlinedText.length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    tv.setText(underlinedText)
                    layout.addView(tv)
                    if (paragraph.parent != null) {
                        (tv.getLayoutParams() as MarginLayoutParams).topMargin =
                            paragraphSpacing
                    }
                }

                MarkdownParagraphType.HLINE -> {
                    val hLine = View(activity)
                    layout.addView(hLine)
                    val hLineParams =
                        hLine.getLayoutParams() as MarginLayoutParams
                    hLineParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    hLineParams.height = dpScale.toInt()
                    hLineParams.setMargins(
                        (dpScale * 15).toInt(),
                        paragraphSpacing,
                        (dpScale * 15).toInt(),
                        0
                    )
                    hLine.setLayoutParams(hLineParams)
                    hLine.setBackgroundColor(Color.rgb(128, 128, 128))
                }

                MarkdownParagraphType.QUOTE -> {
                    val quoteLayout = LinearLayout(activity)

                    var lvl = 0
                    while (lvl < min(maxQuoteLevel, paragraph.level)
                    ) {
                        val quoteIndent = View(activity)
                        quoteLayout.addView(quoteIndent)
                        quoteIndent.setBackgroundColor(Color.rgb(128, 128, 128))
                        quoteIndent.getLayoutParams().width = quoteBarWidth
                        quoteIndent.getLayoutParams().height =
                            ViewGroup.LayoutParams.MATCH_PARENT
                        (quoteIndent.getLayoutParams() as MarginLayoutParams).rightMargin
                        = quoteBarWidth
                        quoteIndent.setLayoutParams(quoteIndent.getLayoutParams())
                        lvl++
                    }

                    quoteLayout.addView(tv)
                    layout.addView(quoteLayout)

                    if (paragraph.parent != null) {
                        if (paragraph.parent.type
                            == MarkdownParagraphType.QUOTE
                        ) {
                            (tv.getLayoutParams() as MarginLayoutParams).topMargin
                            =
                            paragraphSpacing
                        } else {
                            (quoteLayout.getLayoutParams() as MarginLayoutParams).topMargin
                            =
                            paragraphSpacing
                        }
                    }
                }

                MarkdownParagraphType.TEXT -> {
                    layout.addView(tv)
                    if (paragraph.parent != null) {
                        (tv.getLayoutParams() as MarginLayoutParams).topMargin =
                            paragraphSpacing
                    }
                }

                MarkdownParagraphType.EMPTY -> throw RuntimeException(
                    "Internal error: empty paragraph when building view"
                )
            }

            if (showLinkButtons) {
                for (link in paragraph.links) {
                    val ldv =
                        LinkDetailsView(activity, link.title, link.subtitle)
                    layout.addView(ldv)

                    val linkMarginPx = Math.round(dpScale * 8)
                    (ldv.getLayoutParams() as LinearLayout.LayoutParams).setMargins(
                        0,
                        linkMarginPx,
                        0,
                        linkMarginPx
                    )

                    setLayoutMatchWidthWrapHeight(ldv)

                    ldv.setOnClickListener(View.OnClickListener { v: View? ->
                        link.onClicked(
                            activity
                        )
                    })

                    ldv.setOnLongClickListener(OnLongClickListener { v: View? ->
                        link.onLongClicked(activity)
                        true
                    })
                }
            }
        }

        return layout
    }
}
