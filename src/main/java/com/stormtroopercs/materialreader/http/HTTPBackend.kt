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

package com.stormtroopercs.materialreader.http

import android.content.Context
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.common.Result
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.http.body.HTTPRequestBody
import com.stormtroopercs.materialreader.http.okhttp.OKHTTPBackend
import java.io.InputStream

abstract class HTTPBackend {
    data class RequestDetails(
        val url: UriString,
        val requestBody: HTTPRequestBody?
    )

    interface Request {
        fun executeInThisThread(listener: Listener)
        fun cancel()
        fun addHeader(name: String, value: String)
    }

    interface Listener {
        fun onError(
            failureType: RequestFailureType,
            exception: Throwable?,
            httpStatus: Int?,
            body: FailedRequestBody?
        )

        fun onSuccess(mimetype: String?, bodyBytes: Long?, body : InputStream)
    }

    abstract fun resolveRedirectUri(
        context: Context,
        url: UriString
    ): Result<UriString>

    abstract fun prepareRequest(context: Context, details: RequestDetails): Request

    abstract fun recreateHttpBackend()

    companion object {
        @JvmStatic
		val backend: HTTPBackend
            get() = OKHTTPBackend.getHttpBackend()
    }
}
