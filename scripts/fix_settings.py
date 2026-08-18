#!/usr/bin/env python3
"""Fix SettingsFragment.kt (45 errors) + RedReader.kt packageInfo visibility.
Reports found/expected counts per replacement so any mismatch is visible."""
from pathlib import Path

root = Path(__file__).resolve().parent.parent / 'src/main/java/org/quantumbadger/redreader'
sf = root / "settings/SettingsFragment.kt"
rr = root / "RedReader.kt"

def apply(path: Path, pairs):
    t = path.read_text(errors="replace")
    for old, new, exp in pairs:
        n = t.count(old)
        tag = "ok " if n == exp else "MISMATCH"
        print(f"  [{tag} got={n} exp={exp}] {old[:64]!r}")
        if n:
            t = t.replace(old, new)
    path.write_text(t)

print("=== RedReader.kt: packageInfo visibility ===")
apply(rr, [
    ("    private lateinit var packageInfo: AndroidCommon.PackageInfo",
     "    lateinit var packageInfo: AndroidCommon.PackageInfo", 1),
])

print("\n=== SettingsFragment.kt ===")
apply(sf, [
    # import DialogInterface
    ("import android.content.Context\nimport android.content.Intent",
     "import android.content.Context\nimport android.content.DialogInterface\nimport android.content.Intent", 1),
    # 3x context decls
    ("val context: Context?=getActivity()",
     "val context: Context = requireActivity()", 3),
    # findPreference non-null (single-line)
    ("val thumbnailNsfwPref: Preference = findPreference(getString(R.string.pref_appearance_thumbnails_nsfw_show_key))",
     "val thumbnailNsfwPref: Preference = findPreference<Preference>(getString(R.string.pref_appearance_thumbnails_nsfw_show_key))!!", 1),
    ("val thumbnailSpoilerPref: Preference = findPreference(getString(R.string.pref_appearance_thumbnails_spoiler_show_key))",
     "val thumbnailSpoilerPref: Preference = findPreference<Preference>(getString(R.string.pref_appearance_thumbnails_spoiler_show_key))!!", 1),
    ("val inlineImagesNsfwPref: Preference = findPreference(getString(R.string.pref_images_inline_image_previews_nsfw_key))",
     "val inlineImagesNsfwPref: Preference = findPreference<Preference>(getString(R.string.pref_images_inline_image_previews_nsfw_key))!!", 1),
    ("val inlineImagesSpoilerPref: Preference = findPreference(getString(R.string.pref_images_inline_image_previews_spoiler_key))",
     "val inlineImagesSpoilerPref: Preference = findPreference<Preference>(getString(R.string.pref_images_inline_image_previews_spoiler_key))!!", 1),
    # findPreference multi-line (shareAsPermalinkPref)
    ("val shareAsPermalinkPref: Preference = findPreference(\n                getString(R.string.pref_behaviour_share_permalink_key)\n            )",
     "val shareAsPermalinkPref: Preference = findPreference<Preference>(\n                getString(R.string.pref_behaviour_share_permalink_key)\n            )!!", 1),
    # findPreference direct call (updateStorageLocationText)
    ("findPreference(getString(R.string.pref_cache_location_key)).setSummary(",
     "findPreference<Preference>(getString(R.string.pref_cache_location_key))!!.setSummary(", 1),
    # currentStorage nullable
    ("val currentStorage: String=PrefsUtility.pref_cache_location(context)",
     "val currentStorage: String?=PrefsUtility.pref_cache_location(context)", 1),
    # checkPaths non-null File
    ("val checkPaths: MutableList<File?> = CacheManager.Companion.getCacheDirs(context)",
     "val checkPaths: MutableList<File> = CacheManager.Companion.getCacheDirs(context)", 1),
    # EnumMap non-null
    ("val cachesToClear = EnumMap<CacheType?, kotlin.Boolean?>(CacheType::class.java)",
     "val cachesToClear = EnumMap<CacheType, Boolean>(CacheType::class.java)", 1),
    # get() !! (pruneCache args)
    ("cachesToClear.get(CacheType.LISTINGS),", "cachesToClear.get(CacheType.LISTINGS)!!,", 1),
    ("cachesToClear.get(CacheType.THUMBNAILS),", "cachesToClear.get(CacheType.THUMBNAILS)!!,", 1),
    ("cachesToClear.get(CacheType.IMAGES)\n", "cachesToClear.get(CacheType.IMAGES)!!\n", 1),
    # requireNonNull -> !!
    ("if (Objects.requireNonNull<kotlin.Boolean?>(cachesToClear.get(CacheType.FLAGS))) {",
     "if (cachesToClear.get(CacheType.FLAGS)!!) {", 1),
    # BackupDestination / BackupSource (nested in PrefsBackup) + Uri/return nullability
    ("BackupDestination { contentResolver.openOutputStream(data.getData()) }",
     "PrefsBackup.BackupDestination { contentResolver.openOutputStream(data.data!!)!! }", 1),
    ("BackupSource { contentResolver.openInputStream(data.getData()) }",
     "PrefsBackup.BackupSource { contentResolver.openInputStream(data.data!!)!! }", 1),
    # dialog listeners: non-null dialog param
    ("DialogInterface.OnClickListener { dialog: DialogInterface?, i: Int ->\n                    dialog.dismiss()",
     "DialogInterface.OnClickListener { dialog: DialogInterface, i: Int ->\n                    dialog.dismiss()", 1),
    ("DialogInterface.OnClickListener { dialog: DialogInterface?, i: Int -> dialog.dismiss() }",
     "DialogInterface.OnClickListener { dialog: DialogInterface, i: Int -> dialog.dismiss() }", 1),
    ("DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->",
     "DialogInterface.OnClickListener { dialog: DialogInterface, id: Int ->", 1),
    # OnMultiChoiceClickListener (unresolved) -> DialogInterface.OnMultiChoiceClickListener
    ("OnMultiChoiceClickListener { dialog: DialogInterface?, which: Int, isChecked: kotlin.Boolean ->",
     "DialogInterface.OnMultiChoiceClickListener { dialog: DialogInterface, which: Int, isChecked: Boolean ->", 1),
])
print("\nDone.")
