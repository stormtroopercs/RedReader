#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'
def apply(p, pairs):
    t = p.read_text(errors="replace")
    for old, new, exp in pairs:
        n = t.count(old)
        print(f"  [{'ok ' if n==exp else 'MISMATCH'} got={n} exp={exp}] {old[:54]!r}")
        if n: t = t.replace(old, new)
    p.write_text(t)

print("=== adapters/HeaderRecyclerAdapter.kt (base) ===")
apply(root/"adapters/HeaderRecyclerAdapter.kt", [
    ("abstract class HeaderRecyclerAdapter<VH : RecyclerView.ViewHolder?>\n    : RecyclerView.Adapter<VH?>()",
     "abstract class HeaderRecyclerAdapter<VH : RecyclerView.ViewHolder>\n    : RecyclerView.Adapter<VH>()", 1),
    ("protected abstract fun onCreateHeaderItemViewHolder(parent : ViewGroup): VH?",
     "protected abstract fun onCreateHeaderItemViewHolder(parent : ViewGroup): VH", 1),
    ("protected abstract fun onCreateContentItemViewHolder(parent : ViewGroup): VH?",
     "protected abstract fun onCreateContentItemViewHolder(parent : ViewGroup): VH", 1),
    ("protected abstract fun onBindHeaderItemViewHolder(holder: VH?, position: Int)",
     "protected abstract fun onBindHeaderItemViewHolder(holder: VH, position: Int)", 1),
    ("protected abstract fun onBindContentItemViewHolder(holder: VH?, position: Int)",
     "protected abstract fun onBindContentItemViewHolder(holder: VH, position: Int)", 1),
])
print("=== adapters/SessionListAdapter.kt ===")
apply(root/"adapters/SessionListAdapter.kt", [
    ("HeaderRecyclerAdapter<RecyclerView.ViewHolder?>()",
     "HeaderRecyclerAdapter<RecyclerView.ViewHolder>()", 1),
    ("        holder: RecyclerView.ViewHolder?,",
     "        holder: RecyclerView.ViewHolder,", 2),
])
print("=== adapters/AccountListAdapter.kt ===")
apply(root/"adapters/AccountListAdapter.kt", [
    ("HeaderRecyclerAdapter<RecyclerView.ViewHolder?>()",
     "HeaderRecyclerAdapter<RecyclerView.ViewHolder>()", 1),
    ("holder: RecyclerView.ViewHolder?,",
     "holder: RecyclerView.ViewHolder,", 2),
])
print("Done.")
