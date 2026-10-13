# Truyện+ minimalist reader

The application uses one neutral Apple Books-inspired design, with System, Light
and Dark appearance. Home, Library and Settings are the only shell destinations.
The round Search action opens Library, scrolls to its search field and requests
focus/keyboard. Reading progress remains inside Settings. Import, EPUB export,
book details, chapter navigation and floating listening retain their original
services and repositories.

The saved appearance key is retained for upgrades. Violet/Graphite map to Dark;
Pearl/Amber/Minimalist map to Light. New installations follow System. Legacy
palette implementations and their visible labels are removed. Reader-specific
paper colours and fonts remain independent when the reader does not follow the
app appearance.

## Floating navigation

`ReaderBottomNavigation.kt` uses a capsule with an animated selected tab and a
separate round Search control. Selection exposes tab semantics and all controls
retain at least 48 dp touch targets. Large fonts/small windows use labelled
accessibility icons. The dock's measured height supplies content bottom padding;
its width is bounded and centred on tablets.

`ReaderNavigationBackdrop.kt` records the content display list, excluding the
dock itself. Each capsule samples the matching source coordinates and blurs only
its own bounds plus a small edge margin. Icons and labels are drawn afterwards.
The blur effect is reused; there is no bitmap readback, screenshot polling,
network asset or new blur library. Android 12+ uses the effect unless Android
reports a low-RAM device; older/low-RAM devices use a translucent tinted surface.
The Android Compose GraphicsLayer API is documented at:
https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/layer/GraphicsLayer

## Verification

Desktop Compose tests cover shell scope, legacy appearance migration, minimum
navigation touch targets, tab selection, independent Search callback, and the
recent-book action. Render tests exercise light/dark screens and actual backdrop
sampling. Responsive tests include split windows, tablets, landscape and 200%
font scale. Android compilation is checked separately. These checks do not
measure frame rate or audio quality on a physical Samsung Tab A9.

These changes are included in version 2.0.36 (90).


## Floating controls and shelf (after 2.0.37)

The bottom navigation layout, tabs, Search action, animation and original blur
parameters are unchanged. Mini playback and chapter/audio controls now reuse the
bounded glass renderer. The reader records only its chapter layer, excluding the
header, footer, audio dock and appearance overlay, to avoid sampling feedback.
Top and bottom controls have side margins and no opaque full-width backing strip;
they follow the selected reading paper/text colors. Header and measured footer
insets keep content reachable, including large fonts and narrow windows.

Home, Settings and shelf cards use static translucent surfaces with a light sheen
and border. The appearance panel uses the same treatment within the existing
window; it does not introduce a modal window or full-screen blur. Unsupported and
low-RAM Android devices retain the inexpensive tinted fallback. There are no new
bitmap assets or dependencies. The decorative library/home/progress subtitles and
pre-download metadata explanation were removed.

These source changes require a newer APK. Device frame rate is not measured.
