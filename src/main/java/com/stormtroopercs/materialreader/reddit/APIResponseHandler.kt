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
package com.stormtroopercs.materialreader.reddit

import androidx.appcompat.app.AppCompatActivity
import com.stormtroopercs.materialreader.common.BugReporter.addGlobalError
import com.stormtroopercs.materialreader.common.BugReporter.handleGlobalError
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.reddit.things.RedditUser

abstract class APIResponseHandler private constructor(val context: AppCompatActivity) {
    enum class APIFailureType {
        INVALID_USER,
        BAD_CAPTCHA,
        NOTALLOWED,
        SUBREDDIT_REQUIRED,
        URL_REQUIRED,
        UNKNOWN,
        TOO_FAST,
        TOO_LONG,
        ALREADY_SUBMITTED,
        POST_FLAIR_REQUIRED
    }

    abstract fun onCallbackException(t : Throwable)

    abstract fun onFailure(error: RRError)

    fun notifyFailure(error: RRError) {
        try {
            onFailure(error)
        } catch (t1: Throwable) {
            try {
                onCallbackException(t1)
            } catch (t2: Throwable) {
                addGlobalError(RRError(null, null, true, t1))
                handleGlobalError(context, t2)
            }
        }
    }

    fun notifyFailure(
        type: APIFailureType,
        debuggingContext: String?,
        response: Optional<FailedRequestBody>
    ) {
        notifyFailure(
            General.getGeneralErrorForFailure(
                context,
                type,
                debuggingContext,
                response
            )
        )
    }

    abstract class SubmitResponseHandler protected constructor(context: AppCompatActivity) :
        APIResponseHandler(context) {
        abstract fun onSubmitErrors(errors: ArrayList<String?>)

        abstract fun onSuccess(
            redirectUrl: Optional<String>,
            thingId: Optional<String>
        )
    }

    abstract class ActionResponseHandler protected constructor(context: AppCompatActivity) :
        APIResponseHandler(context) {
        fun notifySuccess() {
            try {
                onSuccess()
            } catch (t1: Throwable) {
                try {
                    onCallbackException(t1)
                } catch (t2: Throwable) {
                    addGlobalError(RRError(null, null, true, t1))
                    handleGlobalError(context, t2)
                }
            }
        }

        protected abstract fun onSuccess()
    }

    abstract class ValueResponseHandler<E> protected constructor(context: AppCompatActivity) :
        APIResponseHandler(context) {
        fun notifySuccess(value: E) {
            try {
                onSuccess(value)
            } catch (t1: Throwable) {
                try {
                    onCallbackException(t1)
                } catch (t2: Throwable) {
                    addGlobalError(RRError(null, null, true, t1))
                    handleGlobalError(context, t2)
                }
            }
        }

        abstract fun onSuccess(value: E)
    }

    abstract class UserResponseHandler protected constructor(context: AppCompatActivity) :
        APIResponseHandler(context) {
        fun notifySuccess(result: RedditUser, timestamp: TimestampUTC) {
            try {
                onSuccess(result, timestamp)
            } catch (t1: Throwable) {
                try {
                    onCallbackException(t1)
                } catch (t2: Throwable) {
                    addGlobalError(RRError(null, null, true, t1))
                    handleGlobalError(context, t2)
                }
            }
        }

        fun notifyDownloadStarted() {
            try {
                onDownloadStarted()
            } catch (t1: Throwable) {
                try {
                    onCallbackException(t1)
                } catch (t2: Throwable) {
                    addGlobalError(RRError(null, null, true, t1))
                    handleGlobalError(context, t2)
                }
            }
        }

        protected abstract fun onDownloadStarted()

        protected abstract fun onSuccess(result : RedditUser, timestamp : TimestampUTC)
    }
}
