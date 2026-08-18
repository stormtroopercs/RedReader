#!/usr/bin/env python3
"""De-null GroupedRecyclerViewAdapter.Item (+ storage) and subclass type-args.
Root cause: converter over-nulled the Item<VH> bound/params and made nested
members private (Java same-file private access doesn't exist in Kotlin)."""
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'

def apply(p: Path, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        n = t.count(old)
        tag = "ok " if n == exp else "MISMATCH"
        print(f"  [{tag} got={n} exp={exp}] {old[:58]!r}")
        if n:
            t = t.replace(old, new)
    p.write_text(t)

print("=== adapters/GroupedRecyclerViewAdapter.kt ===")
apply(root/"adapters/GroupedRecyclerViewAdapter.kt", [
    (": RecyclerView.Adapter<RecyclerView.ViewHolder?>()", ": RecyclerView.Adapter<RecyclerView.ViewHolder>()", 1),
    ("abstract class Item<VH : RecyclerView.ViewHolder?> {", "abstract class Item<VH : RecyclerView.ViewHolder> {", 1),
    ("        private val mUniqueId: Long = ITEM_UNIQUE_ID_GENERATOR.incrementAndGet()",
     "        internal val mUniqueId: Long = ITEM_UNIQUE_ID_GENERATOR.incrementAndGet()", 1),
    ("        private var mCurrentlyHidden = false", "        internal var mCurrentlyHidden = false", 1),
    ("        abstract val viewType: Class<*>?", "        abstract val viewType: Class<*>", 1),
    ("        abstract fun onCreateViewHolder(viewGroup : ViewGroup): VH?", "        abstract fun onCreateViewHolder(viewGroup : ViewGroup): VH", 1),
    ("        abstract fun onBindViewHolder(viewHolder: VH?)", "        abstract fun onBindViewHolder(viewHolder: VH)", 1),
    ("        private fun onBindViewHolderInner(\n            viewHolder: RecyclerView.ViewHolder?\n        ) {\n            onBindViewHolder(viewHolder as VH?)\n        }",
     "        internal fun onBindViewHolderInner(\n            viewHolder: RecyclerView.ViewHolder\n        ) {\n            onBindViewHolder(viewHolder as VH)\n        }", 1),
    ("    private val mItems: Array<ArrayList<Item<*>?>>", "    private val mItems: Array<ArrayList<Item<*>>>", 1),
    ("    private val mItemViewTypeMap = HashMap<Class<*>?, Int?>()", "    private val mItemViewTypeMap = HashMap<Class<*>, Int>()", 1),
    ("    private val mViewTypeItemMap = HashMap<Int?, Item<*>?>()", "    private val mViewTypeItemMap = HashMap<Int, Item<*>>()", 1),
    ("        mItems = arrayOfNulls<ArrayList<*>>(groups) as Array<ArrayList<Item<*>?>>\n\n        for (i in 0..<groups) {\n            mItems[i] = ArrayList<Item<*>?>()\n        }\n",
     "        mItems = Array(groups) { ArrayList<Item<*>>() }\n", 1),
    ("    private fun getItemPositionInternal(groupId: Int, item: Item<*>?): Int",
     "    private fun getItemPositionInternal(groupId: Int, item: Item<*>): Int", 1),
    ("    fun notifyItemChanged(groupId: Int, item: Item<*>?) {", "    fun notifyItemChanged(groupId: Int, item: Item<*>) {", 1),
])

# subclass type-args
subs = {
    "adapters/GroupedRecyclerViewItemView.kt": [("GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder?>", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>", 1)],
    "adapters/GroupedRecyclerViewItemFrameLayout.kt": [("GroupedRecyclerViewAdapter.Item<Any?>()", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>()", 1)],
    "adapters/GroupedRecyclerViewItemRRError.kt": [("GroupedRecyclerViewAdapter.Item<ErrorHolder?>()", "GroupedRecyclerViewAdapter.Item<ErrorHolder>()", 1)],
    "views/list/GroupedRecyclerViewItemListItemView.kt": [("GroupedRecyclerViewAdapter.Item<Any?>()", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>()", 1)],
    "views/list/GroupedRecyclerViewItemListSectionHeaderView.kt": [("GroupedRecyclerViewAdapter.Item<Any?>()", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>()", 1)],
    "activities/InboxListingActivity.kt": [("GroupedRecyclerViewAdapter.Item<Any?>()", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>()", 1)],
    "reddit/RedditCommentListItem.kt": [("GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder?>", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>", 1)],
    "reddit/RedditPostListItem.kt": [("GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder?>", "GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>", 1)],
}
for rel, pairs in subs.items():
    if not pairs:
        continue
    print(f"=== {rel} ===")
    apply(root/rel, pairs)
print("Done.")
