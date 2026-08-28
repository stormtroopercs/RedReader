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
package com.stormtroopercs.materialreader.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.min

class GIFView(context: Context?, movie: Movie) : View(context) {
    private val mMovie: Movie
    private var movieStart: Long = 0

    private val paint = Paint()

    // Accept as byte[] rather than stream due to Android bug workaround
    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        mMovie = movie

        paint.setAntiAlias(true)
        paint.setFilterBitmap(true)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(Color.TRANSPARENT)
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()

        val scale = min(
            getWidth().toFloat() / mMovie.width(),
            getHeight().toFloat() / mMovie.height()
        )

        canvas.scale(scale, scale)
        canvas.translate(
            (getWidth().toFloat() / scale - mMovie.width().toFloat()) / 2f,
            (getHeight().toFloat() / scale - mMovie.height().toFloat()) / 2f
        )


        if (movieStart == 0L) {
            movieStart = now.toInt().toLong()
        }

        mMovie.setTime(((now - movieStart) % mMovie.duration()).toInt())
        mMovie.draw(canvas, 0f, 0f, paint)

        this.invalidate()
    }

    companion object {
        fun prepareMovie(
            data: ByteArray,
            offset: Int,
            length: Int
        ): Movie {
            val movie = Movie.decodeByteArray(data, offset, length)

            if (movie.duration() < 1) {
                throw RuntimeException("Invalid GIF")
            }

            return movie
        }
    }
}
