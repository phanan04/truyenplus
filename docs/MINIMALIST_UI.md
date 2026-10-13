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
