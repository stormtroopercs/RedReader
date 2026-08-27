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
package org.quantumbadger.redreader.common

enum class NeverAlwaysOrWifiOnly {
    NEVER {
        override fun isEnabled(wifiActive: Boolean): Boolean {
            return false
        }
    },
    WIFIONLY {
        override fun isEnabled(wifiActive: Boolean): Boolean {
            return wifiActive
        }
    },
    ALWAYS {
        override fun isEnabled(wifiActive: Boolean): Boolean {
            return true
        }
    };

    abstract fun isEnabled(wifiActive: Boolean): Boolean
}
