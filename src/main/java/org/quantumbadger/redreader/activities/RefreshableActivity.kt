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
package org.quantumbadger.redreader.activities

import android.content.res.Configuration
import android.os.Bundle
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.General.recreateActivityNoAnimation
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.SharedPrefsWrapper
import java.util.EnumSet

abstract class RefreshableActivity : ViewsBaseActivity() {
    private var paused = false
    private val refreshOnResume: EnumSet<RefreshableFragment?> =
        EnumSet.noneOf<RefreshableFragment?>(
            RefreshableFragment::class.java
        )

    enum class RefreshableFragment {
        MAIN, MAIN_RELAYOUT, POSTS, COMMENTS, RESTART, ALL
    }

    override fun onPause() {
        super.onPause()
        paused = true
    }

    override fun onSharedPreferenceChangedInner(
        prefs: SharedPrefsWrapper?,
        key: String
    ) {
        if (PrefsUtility.isRestartRequired(this, key)) {
            requestRefresh(RefreshableFragment.RESTART, false)
            return
        }

        if (this is MainActivity && PrefsUtility.isReLayoutRequired(this, key)) {
            requestRefresh(RefreshableFragment.MAIN_RELAYOUT, false)
            return
        }

        if (PrefsUtility.isRefreshRequired(this, key)) {
            requestRefresh(RefreshableFragment.ALL, false)
            return
        }

        if (this is MainActivity) {
            if (key == getString(string.pref_pinned_subreddits_key) ||
                key == getString(string.pref_blocked_subreddits_key)
            ) {
                requestRefresh(RefreshableFragment.MAIN, false)
            }
        }
    }

    protected override fun onResume() {
        super.onResume()

        paused = false

        if (!refreshOnResume.isEmpty()) {
            for (f in refreshOnResume) {
                doRefreshNow(f, false)
            }

            refreshOnResume.clear()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        invalidateOptionsMenu()
        super.onConfigurationChanged(newConfig)
    }

    protected fun doRefreshNow(which: RefreshableFragment?, force: Boolean) {
        if (which == RefreshableFragment.RESTART) {
            recreateActivityNoAnimation(this)
        } else {
            doRefresh(which, force, null)
        }
    }

    protected abstract fun doRefresh(
        which: RefreshableFragment?,
        force: Boolean,
        savedInstanceState: Bundle?
    )

    fun requestRefresh(
        which: RefreshableFragment?,
        force: Boolean
    ) {
        runOnUiThread(Runnable {
            if (!paused) {
                doRefreshNow(which, force)
            } else {
                // TODO this doesn't remember "force" //  (but it doesn't really matter...)
                refreshOnResume.add(which)
            }
        }
        )
    }
}
