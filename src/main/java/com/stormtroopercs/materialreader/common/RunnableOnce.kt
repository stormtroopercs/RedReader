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

import java.util.concurrent.atomic.AtomicBoolean

class RunnableOnce(private val mRunnable: Runnable) : Runnable {
    private val mAlreadyRun = AtomicBoolean(false)

    override fun run() {
        if (!mAlreadyRun.getAndSet(true)) {
            mRunnable.run()
        }
    }

    companion object {
        val DO_NOTHING: RunnableOnce = RunnableOnce(Runnable {})
    }
}
