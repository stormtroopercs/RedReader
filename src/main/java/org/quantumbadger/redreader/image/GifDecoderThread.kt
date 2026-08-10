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
package org.quantumbadger.redreader.image

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.ImageView
import jp.tomorrowkey.android.gifplayer.GifDecoder
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile
import kotlin.math.max

class GifDecoderThread(private val `is`: InputStream, private val listener: OnGifLoadedListener) :
    Thread("GIF playing thread") {
    @Volatile
    private var playing = true
    private var view: ImageView?=null

    fun setView(view: ImageView?) {
        this.view = view
    }

    interface OnGifLoadedListener {
        fun onGifLoaded()

        fun onOutOfMemory()

        fun onGifInvalid()
    }

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (playing && view != null) {
                view!!.setImageBitmap(msg.obj as Bitmap?)
            }
        }
    }

    fun stopPlaying() {
        playing = false
        interrupt()

        try {
            `is`.close()
        } catch (t: Throwable) {
            Log.e(TAG, "Exception while stopping", t)
        }
    }

    override fun run() {
        val loaded = AtomicBoolean(false)
        val failed = AtomicBoolean(false)

        val decoder = GifDecoder()

        object : Thread("GIF decoding thread") {
            override fun run() {
                try {
                    decoder.read(`is`)
                    loaded.set(true)
                } catch (t: Throwable) {
                    Log.i(TAG, "Got exception", t)
                    failed.set(true)
                }
            }
        }.start()

        try {
            if (!playing) {
                return
            }

            listener.onGifLoaded()

            var frame = 0

            while (playing) {
                while (decoder.frameCount <= frame + 1 && !loaded.get() && !failed.get()) {
                    try {
                        sleep(100)
                    } catch (e: InterruptedException) {
                        return
                    }
                }

                frame = frame % decoder.frameCount

                val img = decoder.getFrame(frame)

                val msg = Message.obtain()
                msg.obj = img
                handler.sendMessage(msg)

                try {
                    sleep(max(32, decoder.getDelay(frame)).toLong())
                } catch (e: InterruptedException) {
                    return
                }

                if (failed.get()) {
                    listener.onGifInvalid()
                    return
                }

                frame++
            }
        } catch (e: OutOfMemoryError) {
            listener.onOutOfMemory()
        } catch (t: Throwable) {
            listener.onGifInvalid()
        }
    }

    companion object {
        private const val TAG = "GifDecoderThread"
    }
}
