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
package org.quantumbadger.redreader.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.General.dpToPixels
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object ChangelogManager {
    fun generateViews(
        context: AppCompatActivity,
        items: LinearLayout,
        showAll: Boolean
    ) {
        val attr = RRThemeAttributes(context)

        val outerPaddingPx = dpToPixels(context, 12f)
        items.setPadding(outerPaddingPx, 0, outerPaddingPx, outerPaddingPx)

        val filename: String

        if (context.getPackageName().contains("alpha")) {
            filename = "changelog-alpha.txt"
        } else {
            filename = "changelog.txt"
        }

        try {
            BufferedReader(
                InputStreamReader(context.getAssets().open(filename)),
                128 * 1024
            ).use { br ->
                var curVersionName: String?=null
                var itemsToShow = 10

                var line: String?
                while ((br.readLine().also { line = it }) != null) {
                    if (line!!.isEmpty()) {
                        curVersionName = null

                        if (!showAll) {
                            itemsToShow--
                            if (itemsToShow <= 0) {
                                break
                            }
                        }
                    } else if (curVersionName == null) {
                        val lineSplit: Array<String?> =                             line.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        curVersionName = lineSplit[1]

                        val header = LayoutInflater.from(context)
                            .inflate(
                                R.layout.list_sectionheader,
                                items,
                                false
                            ) as TextView
                        header.setText(curVersionName)
                        header.setTextColor(attr.colorAccent)

                        //From https://stackoverflow.com/a/54082384
                        ViewCompat.setAccessibilityDelegate(
                            header,
                            object : AccessibilityDelegateCompat() {
                                override fun onInitializeAccessibilityNodeInfo(
                                    host: View,
                                    info: AccessibilityNodeInfoCompat
                                ) {
                                    super.onInitializeAccessibilityNodeInfo(host, info)
                                    info.setHeading(true)
                                }
                            })

                        items.addView(header)
                    } else {
                        val bulletItem = LinearLayout(context)
                        val paddingPx = dpToPixels(context, 6f)
                        bulletItem.setPadding(paddingPx, paddingPx, paddingPx, 0)
                        bulletItem.setFocusable(true)

                        val bullet = TextView(context)
                        bullet.setText("•  ")
                        bullet.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
                        bulletItem.addView(bullet)

                        val text = TextView(context)
                        text.setText(line)
                        bulletItem.addView(text)

                        items.addView(bulletItem)
                    }
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }
}
