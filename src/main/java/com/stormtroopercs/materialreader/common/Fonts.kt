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

import android.content.res.AssetManager
import android.graphics.Typeface
import android.util.Log
import com.stormtroopercs.materialreader.common.General.startNewThread
import java.util.concurrent.atomic.AtomicReference

object Fonts {
    private const val TAG = "Fonts"

    private val sVeraMono = AtomicReference<Typeface?>()
    private val sRobotoLight = AtomicReference<Typeface?>()

    fun onAppCreate(assetManager: AssetManager) {
        startNewThread("FontCreate", Runnable {
            try {
                sVeraMono.set(Typeface.createFromAsset(assetManager, "fonts/VeraMono.ttf"))
                sRobotoLight.set(Typeface.createFromAsset(assetManager, "fonts/Roboto-Light.ttf"))

                Log.i(TAG, "Fonts created")
            } catch (e: Exception) {
                Log.e(TAG, "Got exception while creating fonts", e)
            }
        })
    }

    val veraMonoOrAlternative: Typeface
        get() {
            val result = sVeraMono.get()

            if (result == null) {
                return Typeface.MONOSPACE
            } else {
                return result
            }
        }

    val robotoLightOrAlternative: Typeface
        get() {
            val result = sRobotoLight.get()

            if (result == null) {
                return Typeface.DEFAULT
            } else {
                return result
            }
        }
}
