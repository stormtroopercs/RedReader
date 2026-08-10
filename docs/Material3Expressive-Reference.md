# Material 3 Expressive — Android Views (MDC-Android) Reference

Captured: 2026-08-08
Sources:
- https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md
- https://github.com/material-components/material-components-android/blob/master/docs/components/CommonButton.md
- https://github.com/material-components/material-components-android/blob/master/docs/components/List.md
- https://github.com/material-components/material-components-android/blob/master/docs/components/TopAppBar.md

---

## 1. Status: Maintenance Mode

- Announced at Google I/O 2026.
- MDC-Android (Views) is in **maintenance mode** — no more feature releases.
- Material 3 Expressive is the **last major Views update**.
- Future Material updates will be **Compose-only**.
- All Views projects should begin or continue migrating to Jetpack Compose.

Key links:
- Android Compose-first blog: https://android-developers.googleblog.com/2026/05/android-ui-development-is-compose-first.html
- Material Android is Compose-first: https://m3.material.io/blog/material-is-compose-first
- Compose M3 getting started: https://developer.android.com/develop/ui/compose/designsystems/material3

---

## 2. Requirements

### Dependency
- `implementation 'com.google.android.material:material:1.14.0'` (or later) for Material3Expressive.
- `1.5.0+` required for base Material3 themes.

### Build Tooling (for 1.13.0+)
- Gradle: 8.9+
- AGP: 8.7.3+
- Android Studio: Ladybug (2024.2.1)+
- compileSdk: 35+
- minSdk: 21+ (but MDC 1.14.0 effectively requires minSdk 23+ for Material3Expressive)

### Non-Transitive R Classes
Starting with 1.13.0-alpha12:
- R classes are no longer transitive.
- Must fully qualify resources, e.g.:
  - AppCompat: `androidx.appcompat.R.attr.colorPrimary`
  - Material: `com.google.android.material.R.attr.colorOnPrimary`
- Opt out with `android.nonTransitiveRClass=false` in `gradle.properties`.

---

## 3. Material3Expressive Themes

Require MDC-Android 1.14.0-alpha01+.

These are drop-in replacements for Material3 themes and enable `Widget.Material3Expressive.*` component styles.

| Material3Expressive Theme                              | Material3 Equivalent              |
|--------------------------------------------------------|-----------------------------------|
| Theme.Material3Expressive.Light                        | Theme.Material3.Light             |
| Theme.Material3Expressive.Light.NoActionBar            | Theme.Material3.Light.NoActionBar |
| Theme.Material3Expressive.Dark                         | Theme.Material3.Dark              |
| Theme.Material3Expressive.Dark.NoActionBar             | Theme.Material3.Dark.NoActionBar  |
| Theme.Material3Expressive.DayNight                     | Theme.Material3.DayNight          |
| Theme.Material3Expressive.DayNight.NoActionBar         | Theme.Material3.DayNight.NoActionBar |
| Theme.Material3Expressive.DynamicColors.Light          | Theme.Material3.DynamicColors.Light |
| Theme.Material3Expressive.DynamicColors.Light.NoActionBar | Theme.Material3.DynamicColors.Light.NoActionBar |
| Theme.Material3Expressive.DynamicColors.Dark           | Theme.Material3.DynamicColors.Dark |
| Theme.Material3Expressive.DynamicColors.Dark.NoActionBar | Theme.Material3.DynamicColors.Dark.NoActionBar |
| Theme.Material3Expressive.DynamicColors.DayNight       | Theme.Material3.DynamicColors.DayNight |
| Theme.Material3Expressive.DynamicColors.DayNight.NoActionBar | Theme.Material3.DynamicColors.DayNight.NoActionBar |

Usage:
```xml
<style name="Theme.MyApp" parent="Theme.Material3Expressive.DayNight.NoActionBar">
    <!-- your custom attributes -->
</style>
```

---

## 4. Material3Expressive Components

### 4.1 Buttons

Expressive updates:
- Five sizes: extra small, small (default), medium, large, extra large
- Toggle (checkable) behavior
- Two shapes: round and square; shape morphs when pressed/selected
- New small padding widths: 16dp (recommended), 24dp (deprecated)

Five styles (by emphasis):
1. Elevated button
2. Filled button
3. Filled tonal button
4. Outlined button
5. Text button

Enabling checkable:
```xml
<Button
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Button"
    android:checkable="true"/>
```

Expressive styles:
- Filled with icon: `@style/Widget.Material3Expressive.Button.Icon`
- Use theme attributes: `?attr/materialButtonStyle`, `?attr/materialButtonTonalStyle`, `?attr/materialButtonOutlinedStyle`, `?attr/materialButtonElevatedStyle`

### 4.2 Lists

New expressive list variants:
- Standard
- Segmented

New views/interfaces:
- `ListItemLayout`: container that applies position states (first/middle/last/single)
- `ListItemCardView`: recommended child of ListItemLayout; updates shape/corners based on state
- `ListItemViewHolder`: helper for RecyclerView; call `bind()` to auto-update appearance
- `SwipeableListItem` interface: for swipe-to-reveal (default impl: ListItemCardView)
- `RevealableListItem` interface: for swipe-to-reveal (default impl: ListItemRevealLayout)
- `ListItemRevealLayout`: reveals actions on swipe; supports `app:primaryActionSwipeMode`

Basic ViewHolder example:
```xml
<com.google.android.material.listitem.ListItemLayout
  xmlns:android="http://schemas.android.com/apk/res/android"
  android:layout_width="match_parent"
  android:layout_height="wrap_content">
  <com.google.android.material.listitem.ListItemCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:checkable="true">
    <!-- content -->
  </com.google.android.material.listitem.ListItemCardView>
</com.google.android.material.listitem.ListItemLayout>
```

RecyclerView adapter:
```kotlin
class ListsAdapter(private val items: List<Data>) :
    RecyclerView.Adapter<ListsAdapter.ListItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ListItemViewHolder {
        return ListItemViewHolder.create(parent, R.layout.list_item_viewholder)
    }

    override fun onBindViewHolder(viewHolder: ListItemViewHolder, position: Int) {
        viewHolder.bind(items[position])
        // set content...
    }
}
```

Swipe-to-reveal example:
```xml
<com.google.android.material.listitem.ListItemLayout
  xmlns:android="http://schemas.android.com/apk/res/android"
  xmlns:app="http://schemas.android.com/apk/res-auto"
  android:layout_width="match_parent"
  android:layout_height="wrap_content">

  <!-- SwipeableListItem -->
  <com.google.android.material.listitem.ListItemCardView
    android:id="@+id/card_view"
    style="?attr/listItemCardViewSegmentedStyle"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    <!-- content -->
  </com.google.android.material.listitem.ListItemCardView>

  <!-- RevealableListItem -->
  <com.google.android.material.listitem.ListItemRevealLayout
    android:layout_width="wrap_content"
    android:layout_height="match_parent"
    app:primaryActionSwipeMode="indirect">
    <!-- reveal actions -->
  </com.google.android.material.listitem.ListItemRevealLayout>
</com.google.android.material.listitem.ListItemLayout>
```

List item accessibility styles:
- `Widget.Material3.Checkbox.ListItem`
- `Widget.Material3.RadioButton.ListItem`
- `Widget.Material3.Switch.ListItem`

### 4.3 App Bars (Top App Bars)

Renamed from "top app bar" to "app bar" in design language.

Four variants:
1. Search app bar (new)
2. Small app bar (updated)
3. Medium flexible app bar (new)
4. Large flexible app bar (new)

Deprecated:
- Medium app bar → use medium flexible
- Large app bar → use large flexible

Search app bar:
- Supports icons inside and outside the search bar
- Centered text
- Opens search view component when selected

Small app bar updates:
- Subtitle support
- Center-aligned text option
- More flexible elements for imagery and filled buttons

Medium/Large flexible app bar updates:
- Reduced overall height
- Larger title text
- Subtitle
- Left- and center-aligned text options
- Text wrapping
- More flexible elements for imagery and filled buttons

### 4.4 Other Expressive Updates (mentioned)

Material 3 Expressive brings expressive styles for 11 existing components including:
- Buttons
- Icon buttons
- FABs
- Top app bars
- Navigation bar/rail
- Lists
- Search
- Progress indicators
- Sliders
- Emphasized typescale

---

## 5. Migration Checklist (Views → Material3Expressive)

1. Update dependency:
   - `google-material = "1.14.0"` in `libs.versions.toml`

2. Bump minSdk:
   - `minSdkVersion` 23+ (required by MDC 1.14.0)

3. Update theme parents:
   - Replace `Theme.Material3.*` with `Theme.Material3Expressive.*`

4. Selective adoption of Expressive component styles:
   - Swap `Widget.Material3.*` → `Widget.Material3Expressive.*` in layouts/styles as desired

5. Long-term: plan Compose migration
   - MDC-Android is in maintenance mode
   - Material3Expressive is the last Views update

---

## 6. Useful Links

- MDC-Android getting started: https://github.com/material-components/material-components-android/blob/master/docs/getting-started.md
- Buttons doc: https://github.com/material-components/material-components-android/blob/master/docs/components/CommonButton.md
- Lists doc: https://github.com/material-components/material-components-android/blob/master/docs/components/List.md
- App Bars doc: https://github.com/material-components/material-components-android/blob/master/docs/components/TopAppBar.md
- Material.io M3: https://m3.material.io/
- Compose M3 guide: https://developer.android.com/develop/ui/compose/designsystems/material3
- Migrate Views → Compose: https://developer.android.com/develop/ui/compose/migrate/migrate-xml-views-to-jetpack-compose
