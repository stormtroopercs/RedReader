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
package org.quantumbadger.redreader.fragments.postsubmit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Consumer
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.streams.Stream
import org.quantumbadger.redreader.reddit.RedditSubredditHistory
import org.quantumbadger.redreader.reddit.api.RedditPostActions.ActionDescriptionPair.Companion.from
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.viewholders.VH1Text
import java.util.Objects

class PostSubmitSubredditSelectionFragment : Fragment() {
    class Args(val subreddit: SubredditCanonicalId?) {
        fun toBundle(): Bundle {
            val result = Bundle(1)
            if (subreddit != null) {
                result.putParcelable(KEY_SUBREDDIT, subreddit)
            }
            return result
        }

        companion object {
            private const val KEY_SUBREDDIT = "subreddit"

            fun fromBundle(bundle: Bundle): Args {
                return Args(
                    BundleCompat.getParcelable<SubredditCanonicalId?>(
                        bundle,
                        KEY_SUBREDDIT,
                        SubredditCanonicalId::class.java
                    )
                )
            }
        }
    }

    interface Listener {
        fun onSubredditSelected(
            username: String,
            subreddit: SubredditCanonicalId
        )

        fun onNotLoggedIn()
    }

    private class AutocompleteEntry(val listId: Long, val nameWithoutPrefix: String)

    private inner class AutocompleteAdapter(context: Context?) : RecyclerView.Adapter<VH1Text?>() {
        private val mAllSuggestions: ArrayList<AutocompleteEntry?> = ArrayList<AutocompleteEntry?>()

        private val mCurrentSuggestions: ArrayList<AutocompleteEntry?> =
            ArrayList<AutocompleteEntry?>()

        init {
            setHasStableIds(true)

            val allSuggestions = RedditSubredditHistory.getSubredditsSorted(
                RedditAccountManager.Companion.getInstance(context)
                    .getDefaultAccount()
            )

            for (i in allSuggestions.indices) {
                mAllSuggestions.add(
                    AutocompleteEntry(
                        i.toLong(),
                        allSuggestions.get(i)!!.getDisplayNameLowercase()
                    )
                )
            }

            mCurrentSuggestions.addAll(mAllSuggestions)
        }

        @SuppressLint("NotifyDataSetChanged")
        fun updateSuggestions() {
            mCurrentSuggestions.clear()

            val currentText = StringUtils.asciiLowercase(
                mSubredditBox!!.getText().toString().trim { it <= ' ' })

            val searchString: String

            try {
                searchString = RedditSubreddit.Companion.stripRPrefix(currentText)
            } catch (e: InvalidSubredditNameException) {
                mCurrentSuggestions.addAll(mAllSuggestions)
                notifyDataSetChanged()
                return
            }

            val possibleSuggestions
                    : ArrayList<AutocompleteEntry?> = ArrayList<AutocompleteEntry?>(mAllSuggestions)

            run {
                val it: MutableIterator<AutocompleteEntry> = possibleSuggestions.iterator()
                while (it.hasNext()) {
                    val entry = it.next()

                    if (entry.nameWithoutPrefix.startsWith(searchString)) {
                        mCurrentSuggestions.add(entry)
                        it.remove()
                    }
                }
            }

            run {
                val it: MutableIterator<AutocompleteEntry> = possibleSuggestions.iterator()
                while (it.hasNext()) {
                    val entry = it.next()

                    if (entry.nameWithoutPrefix.contains(searchString)) {
                        mCurrentSuggestions.add(entry)
                        it.remove()
                    }
                }
            }

            mCurrentSuggestions.addAll(possibleSuggestions)

            notifyDataSetChanged()
            scrollToTop()
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): VH1Text {
            val view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_item_1_text, viewGroup, false)

            val result = VH1Text(view)

            view.setOnClickListener(View.OnClickListener { v: View? ->
                mSubredditBox!!.setText(
                    result.text.getText()
                )
            })

            return result
        }

        override fun onBindViewHolder(
            viewHolder: VH1Text,
            i: Int
        ) {
            viewHolder.text.setText(mCurrentSuggestions.get(i)!!.nameWithoutPrefix)
        }

        override fun getItemCount(): Int {
            return mCurrentSuggestions.size
        }

        override fun getItemId(position: Int): Long {
            return mCurrentSuggestions.get(position)!!.listId
        }
    }

    private var mUsernameSpinner: MaterialAutoCompleteTextView? = null
    private var mSubredditBox: TextInputEditText? = null

    private var mAutocompleteSuggestions: RecyclerView? = null
    private var mAutocompleteSuggestionsLayout: RecyclerView.LayoutManager? = null

    override fun onResume() {
        super.onResume()

        val activity = getActivity()

        if (activity != null) {
            activity.setTitle(string.subreddit_selector_title)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val args: Args = Args.Companion.fromBundle(requireArguments())

        val context = Objects.requireNonNull<ViewGroup?>(container).getContext()

        val root = inflater.inflate(R.layout.subreddit_selection, container, false)

        mUsernameSpinner =
            root.findViewById<MaterialAutoCompleteTextView>(R.id.subreddit_selection_account)
        mSubredditBox = root.findViewById<TextInputEditText>(R.id.subreddit_selection_textbox)

        mAutocompleteSuggestions =
            root.findViewById<RecyclerView>(R.id.subreddit_selection_autocomplete)
        mAutocompleteSuggestionsLayout
        = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        mAutocompleteSuggestions!!.setLayoutManager(mAutocompleteSuggestionsLayout)

        val adapter = AutocompleteAdapter(context)

        mAutocompleteSuggestions!!.setAdapter(adapter)

        AndroidCommon.onTextChanged(mSubredditBox!!, Runnable { adapter.updateSuggestions() })
        AndroidCommon.onTextChanged(mUsernameSpinner!!, Runnable { adapter.updateSuggestions() })

        val accountManager: RedditAccountManager =
            RedditAccountManager.Companion.getInstance(context)

        val usernames = ArrayList<String?>()

        Stream.Companion.from<RedditAccount?>(accountManager.getAccounts())
            .filter(RedditAccount::isNotAnonymous)
            .forEach(Consumer { account: RedditAccount? -> usernames.add(account!!.username) })

        if (usernames.isEmpty()) {
            val activity = getActivity()

            if (activity != null) {
                (activity as Listener).onNotLoggedIn()
            }

            return null
        }

        AndroidCommon.setAutoCompleteTextViewItemsNoFilter(mUsernameSpinner!!, usernames)

        mUsernameSpinner!!.setText(accountManager.getDefaultAccount().username)

        run {
            val continueButton = root.findViewById<Button>(R.id.subreddit_selection_button_continue)
            continueButton.setOnClickListener(View.OnClickListener { v: View? ->
                val activity = getActivity()
                if (activity == null) {
                    return@setOnClickListener
                }

                val subreddit: SubredditCanonicalId


                try {
                    subreddit = SubredditCanonicalId(mSubredditBox!!.getText().toString())
                } catch (e: InvalidSubredditNameException) {
                    val applicationContext = activity.getApplicationContext()

                    showResultDialog(
                        activity as AppCompatActivity, RRError(
                            applicationContext.getString(string.invalid_subreddit_name),
                            applicationContext.getString(string.invalid_subreddit_name_message),
                            false,
                            e
                        )
                    )

                    return@setOnClickListener
                }
                (activity as Listener).onSubredditSelected(
                    mUsernameSpinner!!.getText().toString(),
                    subreddit
                )
            })
        }

        if (args.subreddit != null) {
            mSubredditBox!!.setText(args.subreddit.getDisplayNameLowercase())
            adapter.updateSuggestions()
        }

        return root
    }

    private fun scrollToTop() {
        mAutocompleteSuggestionsLayout!!.smoothScrollToPosition(mAutocompleteSuggestions, null, 0)
    }
}
