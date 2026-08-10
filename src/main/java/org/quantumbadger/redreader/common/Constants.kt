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

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import org.quantumbadger.redreader.RedReader
import org.quantumbadger.redreader.common.collections.CollectionStream
import org.quantumbadger.redreader.common.collections.MapStreamRethrowExceptions
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId

object Constants {
    fun version(context: Context): String? {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e) // Internal error
        }
    }

    fun ua(context: Context): String {
        val canonicalName = RedReader::class.java.getCanonicalName()
        return canonicalName.substring(0, canonicalName.lastIndexOf('.')) + "/" + version(
            context
        )
    }

    const val OA_CS: String = "client_secret"
    const val OA_CI: String = "client_id"

    object Mime {
        fun isImage(mimetype: String): Boolean {
            return StringUtils.asciiLowercase(mimetype).startsWith("image/")
                    && !isImageGif(mimetype)
        }

        fun isImageGif(mimetype: String): Boolean {
            return mimetype.equals("image/gif", ignoreCase = true)
        }

        fun isVideo(mimetype: String): Boolean {
            return mimetype.startsWith("video/")
        }

        fun isOctetStream(mimetype: String): Boolean {
            return mimetype == "application/octet-stream"
        }
    }

    object Reddit {
        val DEFAULT_SUBREDDITS: ArrayList<SubredditCanonicalId?>

        val BOT_USERNAMES_LOWERCASE: HashSet<String?>

        init {
            val defaultSubredditStrings = arrayOf<String?>(
                "/r/Art",
                "/r/AskReddit",
                "/r/askscience",
                "/r/aww",
                "/r/books",
                "/r/creepy",
                "/r/dataisbeautiful",
                "/r/DIY",
                "/r/Documentaries",
                "/r/EarthPorn",
                "/r/explainlikeimfive",
                "/r/Fitness",
                "/r/food",
                "/r/funny",
                "/r/Futurology",
                "/r/gadgets",
                "/r/gaming",
                "/r/GetMotivated",
                "/r/gifs",
                "/r/history",
                "/r/IAmA",
                "/r/InternetIsBeautiful",
                "/r/Jokes",
                "/r/LifeProTips",
                "/r/listentothis",
                "/r/mildlyinteresting",
                "/r/movies",
                "/r/Music",
                "/r/news",
                "/r/nosleep",
                "/r/nottheonion",
                "/r/oldschoolcool",
                "/r/personalfinance",
                "/r/philosophy",
                "/r/photoshopbattles",
                "/r/pics",
                "/r/reddit",
                "/r/science",
                "/r/Showerthoughts",
                "/r/space",
                "/r/sports",
                "/r/television",
                "/r/tifu",
                "/r/todayilearned",
                "/r/TwoXChromosomes",
                "/r/UpliftingNews",
                "/r/videos",
                "/r/worldnews",
                "/r/writingprompts"
            )

            DEFAULT_SUBREDDITS = CollectionStream<String?>(*defaultSubredditStrings)
                .mapRethrowExceptions<SubredditCanonicalId?>(MapStreamRethrowExceptions.Operator { name: SubredditCanonicalId? ->
                    SubredditCanonicalId(
                        name
                    )
                })
                .collect<ArrayList<SubredditCanonicalId?>>(
                    ArrayList<SubredditCanonicalId?>(
                        defaultSubredditStrings.size
                    )
                )

            BOT_USERNAMES_LOWERCASE = HashSet<String?>()
            BOT_USERNAMES_LOWERCASE.add("automoderator")
            BOT_USERNAMES_LOWERCASE.add("qualityvote")
            BOT_USERNAMES_LOWERCASE.add("visualmod")
            BOT_USERNAMES_LOWERCASE.add("a-mirror-bot")
            BOT_USERNAMES_LOWERCASE.add("unexbot")
            BOT_USERNAMES_LOWERCASE.add("rfauxmoi")
            BOT_USERNAMES_LOWERCASE.add("ukbot-nicolabot")
            BOT_USERNAMES_LOWERCASE.add("qualityvote2")
            BOT_USERNAMES_LOWERCASE.add("trendingtattler")
            BOT_USERNAMES_LOWERCASE.add("cannabun")
            BOT_USERNAMES_LOWERCASE.add("pcmrbot")
            BOT_USERNAMES_LOWERCASE.add("spotlight-app")
            BOT_USERNAMES_LOWERCASE.add("flairassistant")
            BOT_USERNAMES_LOWERCASE.add("sponge-tron")
        }

        const val scheme: String = "https"
        const val domain: String = "oauth.reddit.com"
        const val humanReadableDomain: String = "reddit.com"
        const val PATH_VOTE: String = "/api/vote"
        const val PATH_SAVE: String = "/api/save"
        const val PATH_HIDE: String = "/api/hide"
        const val PATH_UNSAVE: String = "/api/unsave"
        const val PATH_UNHIDE: String = "/api/unhide"
        const val PATH_REPORT: String = "/api/report"
        const val PATH_DELETE: String = "/api/del"
        const val PATH_SUBSCRIBE: String = "/api/subscribe"
        const val PATH_SUBREDDITS_MINE_SUBSCRIBER: String="/subreddits/mine/subscriber.json?limit=100"
        const val PATH_SUBREDDITS_MINE_MODERATOR: String="/subreddits/mine/moderator.json?limit=100"
        const val PATH_SUBREDDITS_POPULAR: String = "/subreddits/popular.json"
        const val PATH_MULTIREDDITS_MINE: String = "/api/multi/mine.json"
        const val PATH_COMMENTS: String = "/comments/"
        const val PATH_ME: String = "/api/v1/me"

        fun getUriBuilder(path: String?): Uri.Builder? {
            return Uri.parse(getUri(path).toString()).buildUpon()
        }

        fun getUri(path: String?): UriString {
            return UriString(scheme + "://" + domain + path)
        }

        fun getNonAPIUri(path: String?): UriString {
            return UriString(scheme + "://reddit.com" + path)
        }

        fun isApiErrorUser(str: String?): Boolean {
            return ".error.USER_REQUIRED" == str || "please login to do that" == str
        }

        fun isApiErrorCaptcha(str: String?): Boolean {
            return ".error.BAD_CAPTCHA.field-captcha" == str
                    || "care to try these again?" == str
        }

        fun isApiErrorNotAllowed(str: String?): Boolean {
            return ".error.SUBREDDIT_NOTALLOWED.field-sr" == str
                    || "you aren't allowed to post there." == str
        }

        fun isApiErrorSubredditRequired(str: String?): Boolean {
            return ".error.SUBREDDIT_REQUIRED.field-sr" == str
                    || "you must specify a subreddit" == str
        }

        fun isApiErrorURLRequired(str: String?): Boolean {
            return ".error.NO_URL.field-url" == str
                    || "a url is required" == str
        }

        fun isApiTooFast(str: String?): Boolean {
            return ".error.RATELIMIT.field-ratelimit" == str
                    || (str != null && str.contains("you are doing that too much"))
        }

        fun isApiTooLong(str: String?): Boolean {
            return "TOO_LONG" == str
                    || (str != null && str.contains("this is too long"))
        }

        fun isApiAlreadySubmitted(str: String?): Boolean {
            return ".error.ALREADY_SUB.field-url" == str
                    || (str != null
                    && str.contains("that link has already been submitted"))
        }

        fun isPostFlairRequired(str: String?): Boolean {
            return ".error.SUBMIT_VALIDATION_FLAIR_REQUIRED.field-flair" == str
                    || (str != null
                    && str.contains("Your post must contain post flair."))
        }

        fun isApiError(str: String?): Boolean {
            return str != null && str.startsWith(".error.")
        }
    }

    object Priority {
        val CAPTCHA: Int = -600
        val API_ACTION: Int = -500
        val API_MULTIREDDIT_LIST: Int = -200
        val API_SUBREDDIT_LIST: Int = -100
        val API_SUBREDDIT_SEARCH: Int = -500
        val API_SUBREDDIT_INVIDIVUAL: Int = -250
        val API_POST_LIST: Int = -200
        val API_COMMENT_LIST: Int = -300
        const val THUMBNAIL: Int = 100
        const val INLINE_IMAGE_PREVIEW: Int = 100
        const val IMAGE_PRECACHE: Int = 500
        const val COMMENT_PRECACHE: Int = 500
        val IMAGE_VIEW: Int = -400
        val API_USER_ABOUT: Int = -500
        val API_INBOX_LIST: Int = -500
        const val DEV_ANNOUNCEMENTS: Int = 600
    }

    object FileType {
        val NOCACHE: Int = -1
        const val SUBREDDIT_LIST: Int = 100
        const val SUBREDDIT_ABOUT: Int = 101
        const val MULTIREDDIT_LIST: Int = 102
        const val POST_LIST: Int = 110
        const val COMMENT_LIST: Int = 120
        const val USER_ABOUT: Int = 130
        const val INBOX_LIST: Int = 140
        const val THUMBNAIL: Int = 200
        const val IMAGE: Int = 201
        const val CAPTCHA: Int = 202
        const val INLINE_IMAGE_PREVIEW: Int = 203
        const val IMAGE_INFO: Int = 300
    }
}
