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
package com.stormtroopercs.materialreader.io

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.stormtroopercs.materialreader.common.TriggerableThread
import com.stormtroopercs.materialreader.reddit.prepared.RedditChangeDataManager
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class RedditChangeDataIO private constructor(private val mContext: Context) {
    private val mLock = Any()

    private val mIsInitialReadStarted = AtomicBoolean(false)
    private var mIsInitialReadComplete = false
    private var mUpdatePending = false

    private inner class WriteRunnable : Runnable {
        override fun run() {
            val startTime = System.currentTimeMillis()

            try {
                val dataFileTmpLocation: File = dataFileWriteTmpLocation

                Log.i(
                    TAG,
                    String.format(
                        Locale.US,
                        "Writing tmp data file at '%s'",
                        dataFileTmpLocation.absolutePath
                    )
                )

                val dos = ExtendedDataOutputStream(
                    BufferedOutputStream(
                        FileOutputStream(dataFileTmpLocation),
                        64 * 1024
                    )
                )

                dos.writeInt(DB_VERSION)

                RedditChangeDataManager.writeAllUsers(dos)

                dos.flush()
                dos.close()

                Log.i(TAG, "Write successful. Atomically replacing data file...")

                val dataFileLocation: File = this@RedditChangeDataIO.dataFileLocation

                if (!dataFileTmpLocation.renameTo(dataFileLocation)) {
                    Log.e(TAG, "Atomic replace failed!")
                    return
                }

                Log.i(TAG, "Write complete.")

                val bytes = dataFileLocation.length()
                val duration = System.currentTimeMillis() - startTime

                Log.i(
                    TAG,
                    String.format(
                        Locale.US,
                        "%d bytes written in %d ms",
                        bytes,
                        duration
                    )
                )
            } catch (e: IOException) {
                Log.e(TAG, "Write failed!", e)
            }
        }
    }

    private val mWriteThread = TriggerableThread(WriteRunnable(), 5000)

    private fun notifyUpdate() {
        synchronized(mLock) {
            if (mIsInitialReadComplete) {
                triggerUpdate()
            } else {
                mUpdatePending = true
            }
        }
    }

    private val dataFileLocation: File
        get() = File(mContext.getFilesDir(), DB_FILENAME)

    private val dataFileWriteTmpLocation: File
        get() = File(
            mContext.getFilesDir(),
            DB_WRITETMP_FILENAME
        )

    fun runInitialReadInThisThread() {
        if (mIsInitialReadStarted.getAndSet(true)) {
            throw RuntimeException("Attempted to run initial read twice!")
        }

        Log.i(TAG, "Running initial read...")

        try {
            val dataFileLocation = this.dataFileLocation

            Log.i(
                TAG,
                String.format(
                    Locale.US,
                    "Data file at '%s'",
                    dataFileLocation.absolutePath
                )
            )

            if (!dataFileLocation.exists()) {
                Log.i(TAG, "Data file does not exist. Aborting read.")
                return
            }

            val dis = ExtendedDataInputStream(
                BufferedInputStream(
                    FileInputStream(dataFileLocation),
                    64 * 1024
                )
            )

            try {
                val version = dis.readInt()

                if (DB_VERSION != version) {
                    Log.i(
                        TAG,
                        String.format(
                            Locale.US,
                            "Wanted version %d, got %d. Aborting read.",
                            DB_VERSION,
                            version
                        )
                    )
                    return
                }

                RedditChangeDataManager.readAllUsers(dis, mContext)

                Log.i(TAG, "Initial read successful.")
            } finally {
                try {
                    dis.close()
                } catch (e: IOException) {
                    Log.e(TAG, "IO error while trying to close input file", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Initial read failed", e)
        } finally {
            notifyInitialReadComplete()
        }
    }

    private fun notifyInitialReadComplete() {
        synchronized(mLock) {
            mIsInitialReadComplete = true
            if (mUpdatePending) {
                triggerUpdate()
                mUpdatePending = false
            }
        }
    }

    private fun triggerUpdate() {
        mWriteThread.trigger()
    }

    companion object {
        private const val TAG = "RedditChangeDataIO"

        private const val DB_VERSION = 1
        private const val DB_FILENAME = "rr_change_data.dat"
        private const val DB_WRITETMP_FILENAME = "rr_change_data_tmp.dat"

        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: RedditChangeDataIO? = null

        private var STATIC_UPDATE_PENDING = false

        fun getInstance(context: Context): RedditChangeDataIO {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = RedditChangeDataIO(context.getApplicationContext())

                    if (STATIC_UPDATE_PENDING) {
                        INSTANCE!!.notifyUpdate()
                    }
                }

                return INSTANCE!!
            }
        }

        fun notifyUpdateStatic() {
            synchronized(this) {
                if (INSTANCE != null) {
                    INSTANCE!!.notifyUpdate()
                } else {
                    STATIC_UPDATE_PENDING = true
                }
            }
        }
    }
}
