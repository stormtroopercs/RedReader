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

import android.content.Context
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategy
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds
import com.stormtroopercs.materialreader.common.BugReporter.handleGlobalError
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.Constants.Reddit
import com.stormtroopercs.materialreader.common.Consumer
import com.stormtroopercs.materialreader.common.FunctionOneArgNoReturn
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.General.nullAlternative
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.UriString.Companion.from
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimeDuration.Companion.hours
import com.stormtroopercs.materialreader.common.time.TimeDuration.Companion.minutes
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.http.PostField
import com.stormtroopercs.materialreader.http.body.HTTPRequestBody.PostFields
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.jsonwrap.JsonString
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.APIFailureType
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.ActionResponseHandler
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.SubmitResponseHandler
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.UserResponseHandler
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.ValueResponseHandler
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.RedditThing
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId
import java.io.IOException
import java.util.LinkedList
import java.util.Objects
import java.util.UUID
import kotlin.Exception
import kotlin.Int
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throwable

object RedditAPI {
	const val ACTION_UPVOTE: Int = 0
	const val ACTION_UNVOTE: Int = 1
	const val ACTION_DOWNVOTE: Int = 2
	const val ACTION_SAVE: Int = 3
	const val ACTION_HIDE: Int = 4
	const val ACTION_UNSAVE: Int = 5
	const val ACTION_UNHIDE: Int = 6
	const val ACTION_DELETE: Int = 8

	const val SUBSCRIPTION_ACTION_SUBSCRIBE: Int = 0
	const val SUBSCRIPTION_ACTION_UNSUBSCRIBE: Int = 1

	fun flairSelectorForNewLink(
		context: Context,
		cm: CacheManager,
		user: RedditAccount,
		subreddit: SubredditCanonicalId,
		responseHandler: FlairSelectorResponseHandler,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("is_newlink", "true"))

		val apiUrl = Reddit.getUri(subreddit.toString() + "/api/flairselector")

		cm.makeRequest(
			createPostRequest(
				apiUrl,
				user,
				postFields,
				context,
				object : CacheRequestJSONParser.Listener {
					override fun onJsonParsed(
						result: JsonValue,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
					) {
						if (result.asObject() != null &&
							result.asObject()!!.isEmpty
						) {
							responseHandler.onSuccess(mutableListOf<RedditFlairChoice>())
							return
						}

						if (result.asString() != null &&
							Objects.requireNonNull<String?>(result.asString()) == "{}"
						) {
							responseHandler.onSuccess(mutableListOf<RedditFlairChoice>())
							return
						}

						val array = result.getArrayAtPath("choices")

						if (array.isEmpty) {
							val failureType = findFailureType(result)

							responseHandler.onFailure(
								getGeneralErrorForFailure(
									context,
									nullAlternative<APIFailureType?>(
										failureType,
										APIFailureType.UNKNOWN,
									),
									"flairselector",
									Optional.Companion.of<FailedRequestBody>(
										FailedRequestBody(
											result,
										),
									),
								),
							)

							return
						}

						val choices: Optional<MutableList<RedditFlairChoice>> = RedditFlairChoice.Companion.fromJsonList(array.get())

						if (choices.isEmpty) {
							responseHandler.onFailure(
								getGeneralErrorForFailure(
									context,
									RequestFailureType.PARSE,
									RuntimeException(),
									null,
									apiUrl,
									Optional.Companion.of<FailedRequestBody>(
										FailedRequestBody(
											result,
										),
									),
								),
							)
							return
						}

						responseHandler.onSuccess(choices.get())
					}

					override fun onFailure(error: RRError) {
						if (error.httpStatus != null && error.httpStatus == 404) {
							responseHandler.onSubredditDoesNotExist()
						} else if (error.httpStatus != null && error.httpStatus == 403) {
							responseHandler.onSubredditPermissionDenied()
						} else {
							responseHandler.onFailure(error)
						}
					}
				},
			),
		)
	}

	fun submit(
		cm: CacheManager,
		responseHandler: SubmitResponseHandler,
		user: RedditAccount,
		isSelfPost: Boolean,
		subreddit: String,
		title: String,
		body: String,
		sendRepliesToInbox: Boolean,
		markAsNsfw: Boolean,
		markAsSpoiler: Boolean,
		flairId: String?,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("api_type", "json"))
		postFields.add(PostField("kind", if (isSelfPost) "self" else "link"))
		postFields.add(
			PostField(
				"sendreplies",
				if (sendRepliesToInbox) "true" else "false",
			),
		)
		postFields.add(PostField("nsfw", if (markAsNsfw) "true" else "false"))
		postFields.add(PostField("spoiler", if (markAsSpoiler) "true" else "false"))
		postFields.add(PostField("sr", subreddit))
		postFields.add(PostField("title", title))

		if (flairId != null) {
			postFields.add(PostField("flair_id", flairId))
		}

		if (isSelfPost) {
			postFields.add(PostField("text", body))
		} else {
			postFields.add(PostField("url", body))
		}

		cm.makeRequest(
			createPostRequest(
				Reddit.getUri("/api/submit"),
				user,
				postFields,
				context,
				SubmitJSONListener(responseHandler),
			),
		)
	}

	fun compose(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		recipient: String,
		subject: String,
		body: String,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("api_type", "json"))
		postFields.add(PostField("subject", subject))
		postFields.add(PostField("to", recipient))
		postFields.add(PostField("text", body))

		cm.makeRequest(
			createPostRequest(
				Reddit.getUri("/api/compose"),
				user,
				postFields,
				context,
				GenericResponseHandler(responseHandler),
			),
		)
	}

	fun comment(
		cm: CacheManager,
		responseHandler: SubmitResponseHandler,
		inboxResponseHandler: ActionResponseHandler,
		user: RedditAccount,
		parentIdAndType: RedditIdAndType,
		markdown: String,
		sendRepliesToInbox: Boolean,
		context: AppCompatActivity,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("api_type", "json"))
		postFields.add(PostField("thing_id", parentIdAndType.value))
		postFields.add(PostField("text", markdown))

		cm.makeRequest(
			createPostRequest(
				Reddit.getUri("/api/comment"),
				user,
				postFields,
				context,
				SubmitJSONListener(object : SubmitResponseHandler(context) {
					override fun onSubmitErrors(errors: ArrayList<String?>) {
						responseHandler.onSubmitErrors(errors)
					}

					override fun onSuccess(
						redirectUrl: Optional<String>,
						thingId: Optional<String>,
					) {
						if (!sendRepliesToInbox) {
							thingId.ifPresent(
								Consumer { commentFullname: String? ->
									RedditAPI.sendReplies(
										cm,
										inboxResponseHandler,
										user,
										commentFullname!!,
										false,
										context,
									)
								},
							)
						}

						responseHandler.onSuccess(redirectUrl, thingId)
					}

					override fun onCallbackException(t: Throwable) {
						responseHandler.onCallbackException(t)
					}

					override fun onFailure(error: RRError) {
						responseHandler.notifyFailure(error)
					}
				}),
			),
		)
	}

	fun markAllAsRead(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()

		cm.makeRequest(
			createPostRequestUnprocessedResponse(
				Reddit.getUri("/api/read_all_messages"),
				user,
				postFields,
				context,
				object : CacheRequestCallbacks {
					override fun onFailure(error: RRError) {
						responseHandler.notifyFailure(error)
					}

					override fun onDataStreamComplete(
						stream: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?,
					) {
						responseHandler.notifySuccess()
					}
				},
			),
		)
	}

	/**
	 * ViewModel-friendly variant of [markAllAsRead]: fires the same
	 * `/api/read_all_messages` POST but reports success/failure through plain
	 * callbacks instead of an [ActionResponseHandler], so it can be invoked
	 * from a `@HiltViewModel` that holds no `AppCompatActivity` reference.
	 */
	fun markAllAsRead(
		cm: CacheManager,
		user: RedditAccount,
		context: Context,
		onSuccess: () -> Unit = {},
		onFailure: (RRError) -> Unit = {},
	) {
		val postFields = LinkedList<PostField>()

		cm.makeRequest(
			createPostRequestUnprocessedResponse(
				Reddit.getUri("/api/read_all_messages"),
				user,
				postFields,
				context,
				object : CacheRequestCallbacks {
					override fun onFailure(error: RRError) {
						onFailure(error)
					}

					override fun onDataStreamComplete(
						stream: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?,
					) {
						onSuccess()
					}
				},
			),
		)
	}

	/**
	 * ViewModel-friendly variant of the per-message mark-as-read: fires the
	 * same `/api/read_message` POST (thing id as `id`) with plain
	 * success/failure callbacks so it can be invoked from a `@HiltViewModel`.
	 */
	fun markRead(
		cm: CacheManager,
		user: RedditAccount,
		context: Context,
		id: String,
		onSuccess: () -> Unit = {},
		onFailure: (RRError) -> Unit = {},
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("id", id))

		cm.makeRequest(
			createPostRequestUnprocessedResponse(
				Reddit.getUri("/api/read_message"),
				user,
				postFields,
				context,
				object : CacheRequestCallbacks {
					override fun onFailure(error: RRError) {
						onFailure(error)
					}

					override fun onDataStreamComplete(
						stream: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?,
					) {
						onSuccess()
					}
				},
			),
		)
	}

	fun editComment(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		commentIdAndType: RedditIdAndType,
		markdown: String,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("thing_id", commentIdAndType.value))
		postFields.add(PostField("text", markdown))

		cm.makeRequest(
			createPostRequest(
				Reddit.getUri("/api/editusertext"),
				user,
				postFields,
				context,
				GenericResponseHandler(responseHandler),
			),
		)
	}

	fun action(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		idAndType: RedditIdAndType,
		@RedditAction action: Int,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("id", idAndType.value))

		val url = prepareActionUri(action, postFields)

		cm.makeRequest(
			createPostRequest(
				url,
				user,
				postFields,
				context,
				GenericResponseHandler(responseHandler),
			),
		)
	}

	private fun prepareActionUri(
		@RedditAction action: Int,
		postFields: LinkedList<PostField>,
	): UriString {
		when (action) {
			ACTION_DOWNVOTE -> {
				postFields.add(PostField("dir", "-1"))
				return Reddit.getUri(Reddit.PATH_VOTE)
			}

			ACTION_UNVOTE -> {
				postFields.add(PostField("dir", "0"))
				return Reddit.getUri(Reddit.PATH_VOTE)
			}

			ACTION_UPVOTE -> {
				postFields.add(PostField("dir", "1"))
				return Reddit.getUri(Reddit.PATH_VOTE)
			}

			ACTION_SAVE -> return Reddit.getUri(Reddit.PATH_SAVE)
			ACTION_HIDE -> return Reddit.getUri(Reddit.PATH_HIDE)
			ACTION_UNSAVE -> return Reddit.getUri(Reddit.PATH_UNSAVE)
			ACTION_UNHIDE -> return Reddit.getUri(Reddit.PATH_UNHIDE)
			ACTION_DELETE -> return Reddit.getUri(Reddit.PATH_DELETE)

			else -> throw RuntimeException("Unknown post/comment action")
		}
	}

	/**
	 * Reports a post or comment. The reason fields should be constructed using
	 * ReportReason.toPostFields().
	 */
	fun report(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		idAndType: RedditIdAndType,
		subredditName: String?,
		reasonFields: List<PostField>,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("api_type", "json"))
		postFields.add(PostField("thing_id", idAndType.value))

		if (subredditName != null) {
			postFields.add(PostField("sr_name", subredditName))
		}

		postFields.addAll(reasonFields)

		cm.makeRequest(
			createPostRequest(
				Reddit.getUri(Reddit.PATH_REPORT),
				user,
				postFields,
				context,
				GenericResponseHandler(responseHandler),
			),
		)
	}

	fun subscriptionAction(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		subredditId: SubredditCanonicalId,
		@RedditSubredditAction action: Int,
		context: Context,
	) {
		RedditSubredditManager.Companion.getInstance(context, user).getSubreddit(
			subredditId,
			TimestampBound.Companion.ANY,
			object : RequestResponseHandler<RedditSubreddit, RRError> {
				override fun onRequestFailed(failureReason: RRError) {
					responseHandler.notifyFailure(failureReason)
				}

				override fun onRequestSuccess(
					subreddit: RedditSubreddit,
					timeCached: TimestampUTC?,
				) {
					val postFields = LinkedList<PostField>()

					postFields.add(PostField("sr", subreddit.name))

					val url = subscriptionPrepareActionUri(action, postFields)

					cm.makeRequest(
						createPostRequest(
							url,
							user,
							postFields,
							context,
							GenericResponseHandler(responseHandler),
						),
					)
				}
			},
			null,
		)
	}

	private fun subscriptionPrepareActionUri(
		@RedditSubredditAction action: Int,
		postFields: LinkedList<PostField>,
	): UriString {
		when (action) {
			SUBSCRIPTION_ACTION_SUBSCRIBE -> {
				postFields.add(PostField("action", "sub"))
				return Reddit.getUri(Reddit.PATH_SUBSCRIBE)
			}

			SUBSCRIPTION_ACTION_UNSUBSCRIBE -> {
				postFields.add(PostField("action", "unsub"))
				return Reddit.getUri(Reddit.PATH_SUBSCRIBE)
			}

			else -> throw RuntimeException("Unknown subreddit action")
		}
	}

	fun getUser(
		cm: CacheManager,
		usernameToGet: String?,
		responseHandler: UserResponseHandler,
		user: RedditAccount,
		downloadStrategy: DownloadStrategy,
		context: Context,
	) {
		val uri = Reddit.getUri("/user/" + usernameToGet + "/about.json")

		cm.makeRequest(
			createGetRequest(
				uri,
				user,
				Priority(Constants.Priority.API_USER_ABOUT),
				Constants.FileType.USER_ABOUT,
				downloadStrategy,
				context,
				object : CacheRequestJSONParser.Listener {
					override fun onJsonParsed(
						result: JsonValue,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
					) {
						try {
							val userThing = result.asObject<RedditThing>(RedditThing::class.java)
							val userResult = userThing!!.asUser()
							responseHandler.notifySuccess(userResult, timestamp)
						} catch (t: Throwable) {
							// TODO look for error
							responseHandler.notifyFailure(
								getGeneralErrorForFailure(
									context,
									RequestFailureType.PARSE,
									t,
									null,
									uri,
									Optional.Companion.of<FailedRequestBody>(
										FailedRequestBody(
											result,
										),
									),
								),
							)
						}
					}

					override fun onFailure(error: RRError) {
						responseHandler.notifyFailure(error)
					}
				},
			),
		)
	}

	fun unblockUser(
		cm: CacheManager,
		usernameToUnblock: String,
		currentUserFullname: String,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("name", usernameToUnblock))
		postFields.add(PostField("container", currentUserFullname))
		postFields.add(PostField("type", "enemy"))

		cm.makeRequest(
			createPostRequest(
				Reddit.getUri("/api/unfriend"),
				user,
				postFields,
				context,
				GenericResponseHandler(responseHandler),
			),
		)
	}

	fun blockUser(
		cm: CacheManager,
		usernameToBlock: String,
		responseHandler: BlockUserResponseHandler,
		user: RedditAccount,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("name", usernameToBlock))
		postFields.add(PostField("api_type", "json"))

		cm.makeRequest(
			createPostRequestUnprocessedResponse(
				Reddit.getUri("/api/block_user"),
				user,
				postFields,
				context,
				object : CacheRequestCallbacks {
					override fun onFailure(error: RRError) {
						// we upgraded the OAuth scope to include account,
						// so check for missing permission
						if (error.httpStatus != null && error.httpStatus == 403) {
							responseHandler.onBlockUserPermissionDenied()
						} else {
							responseHandler.onFailure(error)
						}
					}

					override fun onDataStreamComplete(
						stream: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?,
					) {
						responseHandler.onSuccess()
					}
				},
			),
		)
	}

	fun sendReplies(
		cm: CacheManager,
		responseHandler: ActionResponseHandler,
		user: RedditAccount,
		fullname: String,
		state: Boolean,
		context: Context,
	) {
		val postFields = LinkedList<PostField>()
		postFields.add(PostField("id", fullname))
		postFields.add(PostField("state", state.toString()))
		cm.makeRequest(
			createPostRequest(
				Reddit.getUri("/api/sendreplies"),
				user,
				postFields,
				context,
				GenericResponseHandler(responseHandler),
			),
		)
	}

	fun popularSubreddits(
		cm: CacheManager,
		user: RedditAccount,
		context: Context,
		handler: ValueResponseHandler<SubredditListResponse>,
		after: Optional<String>,
	) {
		val maxCacheAgeMs = hours(1)

		val builder = Reddit.getUriBuilder(
			Reddit.PATH_SUBREDDITS_POPULAR,
		)

		builder.appendQueryParameter("limit", "100")

		after.apply(
			FunctionOneArgNoReturn { value: String? ->
				builder.appendQueryParameter(
					"after",
					value,
				)
			},
		)

		val uri = from(builder.build())

		requestSubredditList(
			cm,
			uri,
			user,
			context,
			handler,
			DownloadStrategyIfTimestampOutsideBounds(
				TimestampBound.Companion.notOlderThan(maxCacheAgeMs),
			),
		)
	}

	fun searchSubreddits(
		cm: CacheManager,
		user: RedditAccount,
		queryString: String,
		context: Context,
		handler: ValueResponseHandler<SubredditListResponse>,
		after: Optional<String>,
	) {
		val maxCacheAgeMs = minutes(1)

		val builder = Reddit.getUriBuilder(
			"/subreddits/search.json",
		)

		builder.appendQueryParameter("q", queryString)
		builder.appendQueryParameter("limit", "100")

		if (PrefsUtility.pref_behaviour_nsfw()) {
			builder.appendQueryParameter("include_over_18", "on")
		}

		after.apply(
			FunctionOneArgNoReturn { value: String? ->
				builder.appendQueryParameter(
					"after",
					value,
				)
			},
		)

		val uri = from(builder.build())

		requestSubredditList(
			cm,
			uri,
			user,
			context,
			handler,
			DownloadStrategyIfTimestampOutsideBounds(
				TimestampBound.Companion.notOlderThan(maxCacheAgeMs),
			),
		)
	}

	fun subscribedSubreddits(
		cm: CacheManager,
		user: RedditAccount,
		context: AppCompatActivity,
		handler: ValueResponseHandler<ArrayList<RedditSubreddit?>?>,
	) {
		subscribedSubredditsInternal(
			cm,
			user,
			context,
			handler,
			Optional.Companion.empty<String>(),
			ArrayList<RedditSubreddit?>(128),
		)
	}

	private fun subscribedSubredditsInternal(
		cm: CacheManager,
		user: RedditAccount,
		context: AppCompatActivity,
		handler: ValueResponseHandler<ArrayList<RedditSubreddit?>?>,
		after: Optional<String>,
		results: ArrayList<RedditSubreddit?>,
	) {
		val builder = Reddit.getUriBuilder(
			Reddit.PATH_SUBREDDITS_MINE_SUBSCRIBER,
		)

		after.apply(
			FunctionOneArgNoReturn { value: String? ->
				builder.appendQueryParameter(
					"after",
					value,
				)
			},
		)

		val uri = from(builder.build())

		requestSubredditList(
			cm,
			uri,
			user,
			context,
			object : ValueResponseHandler<SubredditListResponse>(context) {
				override fun onSuccess(value: SubredditListResponse) {
					results.addAll(value.subreddits)

					if (value.after.isEmpty) {
						handler.onSuccess(results)
					} else {
						subscribedSubredditsInternal(
							cm,
							user,
							context,
							handler,
							value.after,
							results,
						)
					}
				}

				override fun onCallbackException(t: Throwable) {
					handler.onCallbackException(t)
				}

				override fun onFailure(error: RRError) {
					handler.onFailure(error)
				}
			},
			DownloadStrategyAlways.Companion.INSTANCE,
		)
	}

	fun requestSubredditList(
		cm: CacheManager,
		uri: UriString,
		user: RedditAccount,
		context: Context,
		handler: ValueResponseHandler<SubredditListResponse>,
		downloadStrategy: DownloadStrategy,
	) {
		cm.makeRequest(
			createGetRequest(
				uri,
				user,
				Priority(Constants.Priority.API_SUBREDDIT_LIST),
				Constants.FileType.SUBREDDIT_LIST,
				downloadStrategy,
				context,
				object : CacheRequestJSONParser.Listener {
					override fun onJsonParsed(
						result: JsonValue,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
					) {
						try {
							val subreddits = result.getArrayAtPath("data", "children")

							val after = result.getStringAtPath("data", "after")

							if (subreddits.isEmpty) {
								throw IOException("Subreddit data not found")
							}

							val output = ArrayList<RedditSubreddit?>()

							for (value in subreddits.get()) {
								val redditThing = value.asObject<RedditThing>(RedditThing::class.java)
								val subreddit = redditThing!!.asSubreddit()
								output.add(subreddit)
							}

							handler.notifySuccess(
								SubredditListResponse(output, after),
							)
						} catch (e: Exception) {
							onFailure(
								getGeneralErrorForFailure(
									context,
									RequestFailureType.PARSE,
									e,
									null,
									uri,
									Optional.Companion.of<FailedRequestBody>(
										FailedRequestBody(
											result,
										),
									),
								),
							)
						}
					}

					override fun onFailure(error: RRError) {
						handler.notifyFailure(error)
					}
				},
			),
		)
	}

	private fun findFailureType(response: JsonValue?): APIFailureType? {
		// TODO handle 403 forbidden

		if (response == null) {
			return null
		}

		var unknownError = false

		if (response.asObject() != null) {
			for (v in response.asObject()!!) {
				if ("success" == v.key &&
					false == v.value.asBoolean()
				) {
					unknownError = true
				}

				val failureType = findFailureType(v.value)

				if (failureType == APIFailureType.UNKNOWN) {
					unknownError = true
				} else if (failureType != null) {
					return failureType
				}
			}

			val errors = response.getArrayAtPath("json", "errors")

			if (errors.isPresent && errors.get().size() > 0) {
				unknownError = true
			}
		} else if (response.asArray() != null) {
			for (v in response.asArray()!!) {
				val failureType = findFailureType(v)

				if (failureType == APIFailureType.UNKNOWN) {
					unknownError = true
				} else if (failureType != null) {
					return failureType
				}
			}
		} else if (response is JsonString) {
			val responseAsString = response.asString()

			if (Reddit.isApiErrorUser(responseAsString)) {
				return APIFailureType.INVALID_USER
			}

			if (Reddit.isApiErrorCaptcha(responseAsString)) {
				return APIFailureType.BAD_CAPTCHA
			}

			if (Reddit.isApiErrorNotAllowed(responseAsString)) {
				return APIFailureType.NOTALLOWED
			}

			if (Reddit.isApiErrorSubredditRequired(responseAsString)) {
				return APIFailureType.SUBREDDIT_REQUIRED
			}

			if (Reddit.isApiErrorURLRequired(responseAsString)) {
				return APIFailureType.URL_REQUIRED
			}

			if (Reddit.isApiTooFast(responseAsString)) {
				return APIFailureType.TOO_FAST
			}

			if (Reddit.isApiTooLong(responseAsString)) {
				return APIFailureType.TOO_LONG
			}

			if (Reddit.isApiAlreadySubmitted(responseAsString)) {
				return APIFailureType.ALREADY_SUBMITTED
			}

			if (Reddit.isPostFlairRequired(responseAsString)) {
				return APIFailureType.POST_FLAIR_REQUIRED
			}

			if (Reddit.isApiError(responseAsString)) {
				unknownError = true
			}
		}

		return if (unknownError) APIFailureType.UNKNOWN else null
	}

	private fun createPostRequest(
		url: UriString,
		user: RedditAccount,
		postFields: MutableList<PostField>,
		context: Context,
		handler: CacheRequestJSONParser.Listener,
	): CacheRequest = createPostRequestUnprocessedResponse(
		url,
		user,
		postFields,
		context,
		CacheRequestJSONParser(context, handler),
	)

	private fun createPostRequestUnprocessedResponse(
		url: UriString,
		user: RedditAccount,
		postFields: MutableList<PostField>,
		context: Context,
		callbacks: CacheRequestCallbacks,
	): CacheRequest = CacheRequest(
		url,
		user,
		null,
		Priority(Constants.Priority.API_ACTION),
		DownloadStrategyAlways.Companion.INSTANCE,
		Constants.FileType.NOCACHE,
		DownloadQueueType.REDDIT_API,
		PostFields(postFields),
		context,
		callbacks,
	)

	private fun createGetRequest(
		url: UriString,
		user: RedditAccount,
		priority: Priority,
		fileType: Int,
		downloadStrategy: DownloadStrategy,
		context: Context,
		handler: CacheRequestJSONParser.Listener,
	): CacheRequest = CacheRequest(
		url,
		user,
		null,
		priority,
		downloadStrategy,
		fileType,
		DownloadQueueType.REDDIT_API,
		null,
		context,
		CacheRequestJSONParser(context, handler),
	)

	@IntDef(ACTION_UPVOTE, ACTION_UNVOTE, ACTION_DOWNVOTE, ACTION_SAVE, ACTION_HIDE, ACTION_UNSAVE, ACTION_UNHIDE, ACTION_DELETE)
	@Retention(AnnotationRetention.SOURCE)
	annotation class RedditAction

	@IntDef(SUBSCRIPTION_ACTION_SUBSCRIBE, SUBSCRIPTION_ACTION_UNSUBSCRIBE)
	@Retention(
		AnnotationRetention.SOURCE,
	)
	annotation class RedditSubredditAction

	private class GenericResponseHandler(private val mHandler: ActionResponseHandler) : CacheRequestJSONParser.Listener {
		override fun onJsonParsed(
			result: JsonValue,
			timestamp: TimestampUTC,
			session: UUID,
			fromCache: kotlin.Boolean,
		) {
			try {
				val failureType = findFailureType(result)

				if (failureType != null) {
					mHandler.notifyFailure(
						failureType,
						"GenericResponseHandler",
						Optional.Companion.of<FailedRequestBody>(FailedRequestBody(result)),
					)
				} else {
					mHandler.notifySuccess()
				}
			} catch (e: Exception) {
				handleGlobalError(
					mHandler.context,
					RRError(
						null,
						null,
						true,
						e,
						null,
						null,
						result.toString(),
					),
				)
			}
		}

		override fun onFailure(error: RRError) {
			mHandler.notifyFailure(error)
		}
	}

	interface BlockUserResponseHandler {
		fun onSuccess()
		fun onBlockUserPermissionDenied()
		fun onFailure(error: RRError)
	}

	interface FlairSelectorResponseHandler {
		fun onSuccess(choices: MutableCollection<RedditFlairChoice>)

		fun onSubredditDoesNotExist()

		fun onSubredditPermissionDenied()

		fun onFailure(error: RRError)
	}

	private class SubmitJSONListener(private val mResponseHandler: SubmitResponseHandler) : CacheRequestJSONParser.Listener {
		override fun onJsonParsed(
			result: JsonValue,
			timestamp: TimestampUTC,
			session: UUID,
			fromCache: kotlin.Boolean,
		) {
			try {
				val errorsJson = result.getArrayAtPath("json", "errors")

				if (errorsJson.isPresent) {
					val errors = ArrayList<String?>()

					for (errorValue in errorsJson.get()) {
						val error = errorValue.asArray()

						if (error != null && error.getString(1) != null) {
							errors.add(error.getString(1))
						}
					}

					if (!errors.isEmpty()) {
						mResponseHandler.onSubmitErrors(errors)
						return
					}
				}

				val failureType = findFailureType(result)

				if (failureType != null) {
					mResponseHandler.notifyFailure(
						failureType,
						null,
						Optional.Companion.of<FailedRequestBody>(FailedRequestBody(result)),
					)
				} else {
					mResponseHandler.onSuccess(
						result.getStringAtPath("json", "data", "things", 0, "data", "permalink")
							.orElse(result.getStringAtPath("json", "data", "url")),
						result.getStringAtPath("json", "data", "things", 0, "data", "name"),
					)
				}
			} catch (e: Exception) {
				handleGlobalError(
					mResponseHandler.context,
					RRError(
						null,
						null,
						true,
						e,
						null,
						null,
						result.toString(),
					),
				)
			}
		}

		override fun onFailure(error: RRError) {
			mResponseHandler.notifyFailure(error)
		}
	}

	class SubredditListResponse(
		val subreddits: ArrayList<RedditSubreddit?>,
		val after: Optional<String>,
	)
}
