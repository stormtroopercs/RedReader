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
package org.quantumbadger.redreader.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.activities.ViewsBaseActivity
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.General

abstract class RRFragment protected constructor(
    @get:JvmName("getActivityProp")
    protected val activity: AppCompatActivity,
    savedInstanceState: Bundle?
) {
    /**
     * Legacy accessor for converted call sites that still use
     * the Java-style `getActivity()` call form.
     */
    protected fun getActivity(): AppCompatActivity {
        return activity
    }

    protected val context: Context
        get() = this.activity

    protected fun getString(resource: Int): String {
        return activity.getApplicationContext().getString(resource)
    }

    protected fun startActivity(intent: Intent?) {
        activity.startActivity(intent)
    }

    protected fun startActivityForResult(
        intent: Intent,
        requestCode: Int
    ) {
        activity.startActivityForResult(intent, requestCode)
    }

    open fun onCreateOptionsMenu(menu : Menu) {
    }

    open fun onOptionsItemSelected(item : MenuItem): Boolean {
        return false
    }

    abstract val listingView: View

    open val overlayView: View?
        get() =// Null by default
            null

    fun createCombinedListingAndOverlayView(): View {
        val outer = FrameLayout(this.activity)

        run {
            val view = this.listingView
            outer.addView(view)
            setLayoutMatchParent(view)
        }

        run {
            val overlayView = this.overlayView
            if (overlayView != null) {
                outer.addView(overlayView)
                setLayoutMatchParent(overlayView)
            }
        }

        return outer
    }

    fun setBaseActivityContent(baseActivity: ViewsBaseActivity) {
        run {
            val view = this.listingView
            baseActivity.setBaseActivityListing(view)
            setLayoutMatchParent(view)
        }

        run {
            val overlayView = this.overlayView
            if (overlayView != null) {
                baseActivity.setBaseActivityOverlay(overlayView)
                setLayoutMatchParent(overlayView)
            }
        }
    }

    abstract fun onSaveInstanceState(): Bundle?
}