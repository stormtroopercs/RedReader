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

class GenerationalCache<In : HasUniqueId?, Out>(private val mCreator: FunctionOneArgWithReturn<In?, Out?>) {
    private var mPreviousGen = HashMap<String?, Out?>()
    private var mThisGen = HashMap<String?, Out?>()


    fun get(`in`: In): Out {
        val uniqueId = `in`!!.uniqueId

        var result = mThisGen.get(uniqueId)

        if (result != null) {
            return result
        }

        result = mPreviousGen.get(uniqueId)

        if (result == null) {
            result = mCreator.apply(`in`)
            mThisGen.put(uniqueId, result)
        }

        mThisGen.put(uniqueId, result)
        return result
    }

    fun nextGeneration() {
        mPreviousGen = mThisGen
        mThisGen = HashMap<String?, Out?>()
    }
}
