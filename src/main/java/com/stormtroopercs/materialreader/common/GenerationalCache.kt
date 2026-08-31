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

class GenerationalCache<In : HasUniqueId, Out>(private val mCreator: FunctionOneArgWithReturn<In, Out>) {
	private var mPreviousGen = HashMap<String, Out>()
	private var mThisGen = HashMap<String, Out>()

	fun get(`in`: In): Out {
		val uniqueId = `in`.uniqueId

		val current: Out? = mThisGen.get(uniqueId)

		if (current != null) {
			return current
		}

		val previous: Out? = mPreviousGen.get(uniqueId)

		if (previous != null) {
			mThisGen.put(uniqueId, previous)
			return previous
		}

		val created = mCreator.apply(`in`)

		mThisGen.put(uniqueId, created)
		return created
	}

	fun nextGeneration() {
		mPreviousGen = mThisGen
		mThisGen = HashMap<String, Out>()
	}
}
