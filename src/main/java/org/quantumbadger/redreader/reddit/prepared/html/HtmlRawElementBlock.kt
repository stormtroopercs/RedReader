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
package org.quantumbadger.redreader.reddit.prepared.html

import android.text.SpannableStringBuilder
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.reddit.prepared.bodytext.BlockType
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElementTextSpanned
import java.util.Arrays

class HtmlRawElementBlock : HtmlRawElement {
    private val mBlockType: BlockType
    private val mChildren: ArrayList<HtmlRawElement>

    constructor(
        blockType: BlockType,
        children: ArrayList<HtmlRawElement>
    ) {
        mBlockType = blockType
        mChildren = children
    }

    constructor(
        blockType: BlockType,
        vararg children: HtmlRawElement?
    ) {
        mBlockType = blockType
        mChildren = ArrayList<HtmlRawElement>(children.size)
        mChildren.addAll(Arrays.asList<HtmlRawElement?>(*children))
    }

    override fun getPlainText(stringBuilder: StringBuilder) {
        for (element in mChildren) {
            element.getPlainText(stringBuilder)
        }
    }

    fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity
    ): HtmlRawElementBlock {
        val reduced = ArrayList<HtmlRawElement>()
        val linkButtons = ArrayList<LinkButtonDetails>()

        for (child in mChildren) {
            child.reduce(activeAttributes, activity, reduced, linkButtons)
        }

        for (details in linkButtons) {
            reduced.add(HtmlRawElementLinkButton(details))
        }

        return HtmlRawElementBlock(mBlockType, reduced)
    }

    override fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement?>,
        linkButtons: ArrayList<LinkButtonDetails?>
    ) {
        destination.add(reduce(activeAttributes, activity))
    }

    override fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement?>
    ) {
        var stringWrittenTo = false

        var ssb = SpannableStringBuilder()

        var bodyElementTextSpanned =
            BodyElementTextSpanned(mBlockType, ssb)

        for (child in mChildren) {
            if (child is HtmlRawElementStyledText) {
                child.writeTo(ssb)
                stringWrittenTo = true
            } else if (child is HtmlRawElementImg) {
                child.writeTo(
                    ssb,
                    activity,
                    bodyElementTextSpanned
                )
                stringWrittenTo = true
            } else {
                if (stringWrittenTo) {
                    destination.add(bodyElementTextSpanned)

                    ssb = SpannableStringBuilder()
                    bodyElementTextSpanned = BodyElementTextSpanned(mBlockType, ssb)

                    stringWrittenTo = false
                }
                child.generate(activity, destination)
            }
        }

        // If the last child in the array is a HtmlRawElementStyledText
        // or HtmlRawElementImg object, it won't be added to the destination array in the loop
        // Need this logic to make sure that it's added
        if (stringWrittenTo) {
            destination.add(bodyElementTextSpanned)
        }
    }
}
