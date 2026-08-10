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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt Application class - marks the entry point for Hilt dependency injection.
 * This replaces the plain RedReader application class with Hilt integration.
 */
@HiltAndroidApp
class RedApplication : Application() {

    companion object {
        const val TAG = "RedApplication"

        /**
         * Get the RedApplication instance from any Context.
         */
        @JvmStatic
        fun getInstance(context: Context): RedApplication {
            return context.applicationContext as RedApplication
        }
    }
}
