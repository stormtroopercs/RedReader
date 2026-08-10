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
import androidx.annotation.StringRes
import org.quantumbadger.redreader.R.string
import java.util.Locale

object ScreenreaderPronunciation {
    private val LANGUAGE_CODE_EN: String = Locale("en").getLanguage()

    fun getPronunciation(
        context: Context,
        text: String
    ): String {
        val textLowercase = text.lowercase()

        when (textLowercase) {
            "i.redd.it" -> return context.getString(
                string.accessibility_subtitle_domain_i_redd_it
            )

            "v.redd.it" -> return context.getString(
                string.accessibility_subtitle_domain_v_redd_it
            )

            "imgur.com", "i.imgur.com" -> return "imager dot com"

            "gfycat.com" -> return "giffy cat dot com"
        }

        return pronounceSubreddit(textLowercase)
    }

    fun getAccessibilityString(
        context: Context,
        @StringRes res: Int
    ): String {
        // Only override for English for now

        if (Locale.getDefault().getLanguage() != LANGUAGE_CODE_EN) {
            return context.getString(res)
        }

        // Replace "read" with the English homophone "red" to work around bad speech synth handling
        if (res == string.accessibility_post_already_read_withperiod) {
            return "Red."
        } else {
            return context.getString(res)
        }
    }

    private fun pronounceSubreddit(nameLowercase: String): String {
        if (nameLowercase.startsWith("/r/") || nameLowercase.startsWith("/u/")) {
            return (nameLowercase.get(1)
                .toString() + " slash "
                    + pronounceSubredditStripped(nameLowercase.substring(3)))
        } else if (nameLowercase.startsWith("r/") || nameLowercase.startsWith("u/")) {
            return (nameLowercase.get(0)
                .toString() + " slash "
                    + pronounceSubredditStripped(nameLowercase.substring(2)))
        } else {
            return pronounceSubredditStripped(nameLowercase)
        }
    }

    @Suppress("SpellCheckingInspection")
    private fun pronounceSubredditStripped(nameLowercase: String): String {
        when (nameLowercase) {
            "iama" -> return "i am a"

            "askreddit" -> return "ask reddit"

            "redreader" -> return "red reader"

            "quantumbadger" -> return "quantum badger"

            "automoderator" -> return "auto moderator"

            "whatcouldgowrong" -> return "what could go wrong"

            "mildlyinteresting" -> return "mildly interesting"

            "lifeprotips" -> return "life pro tips"

            "listentothis" -> return "listen to this"

            "nosleep" -> return "no sleep"

            "nottheonion" -> return "not the onion"

            "personalfinance" -> return "personal finance"

            "tifu" -> return "t i f u"

            "todayilearned" -> return "today i learned"

            "twoxchromosomes" -> return "two x chromosomes"

            "writingprompts" -> return "writing prompts"

            "dataisbeautiful" -> return "data is beautiful"

            "explainlikeimfive" -> return "explain like I'm five"
        }

        return nameLowercase
    }
}
