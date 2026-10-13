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

## Reader interaction fixes (after 2.0.37)

Reader control insets now apply even when immersive mode is enabled. Tap-to-hide
still removes the header/footer in reading mode; active listening retains its
measured bottom inset. A down-arrow minimizes the listening dock without stopping
TTS. The small dock retains expand, play/pause, settings and close controls.

A shared `ReaderPanelState` owns Appearance/Listening settings. Opening one closes
the other. While Appearance is open, listening uses the small dock and the
appearance frame reserves its space. Both settings use fixed-height pages rather
than vertical scrolling. Appearance groups text, layout, colors and auto-scroll;
listening groups voice/pitch, music, reading and timer controls. Line spacing and
margins expose levels 1–10. The quick font picker uses the system Default, Serif
and Sans serif families, retaining the existing font preference/rendering path.

Scroll positions are observed via snapshotFlow outside composition. Tracking no
longer copies the complete chapter state for each paragraph. Progress stays in
memory immediately and database writes debounce for 500ms; leaving a chapter
flushes a captured position so a reset cannot overwrite the old chapter's value.
During a fling/scroll, chapter display-list recording and control blur use the
cheap tinted path. They resume when scrolling settles. This removes specific
sources of extra work; it is not a physical-device FPS measurement.

These changes remain source-only until a new APK is built.


## Edge TTS removal (after 2.0.37)

The Edge engine, streaming client/protocol, Android audio output, DI registration,
voice picker, strings and dedicated client WebSocket dependency have been removed.
The listening voice button opens the existing system voice selector directly.
A one-way preference migration removes saved Edge keys; users who selected Edge
return to Native, while other engine selections remain unchanged. No library,
reading progress or music file is removed. This source change needs a newer APK.


## Listening controller redesign (after 2.0.37)

The expanded floating glass dock shows the selected system voice, current chapter,
passage index/count, previous/next chapter labels, play/pause, settings, minimize,
and speed choices in 0.25x increments. Play shows a preparing indicator while TTS
loads. The 72dp compact dock keeps expand, play/pause, settings and stop reachable.
The chapter remains the reading surface; listening does not open an artwork page.

Settings have a distinct close button and four fixed pages: voice/pitch/media
volume, local background music, reading behavior, and sleep timer. Opening settings
uses the compact dock; font and listening settings remain mutually exclusive.
Short landscape settings use two columns to keep controls inside the available
height. Playback buttons use neutral contrasting colors for light/dark paper.
Existing TTS, chapter navigation, music and timer callbacks are reused.

### Reading UX follow-ups

Based on source and rendered layouts, the highest-value remaining improvements are:

- Suspend automatic TTS following after manual scrolling, with an explicit action
  to return to the spoken passage. Current following resumes on later passages.
- Make paragraph selection while listening clearly distinct from tap-to-hide
  reading controls, to avoid accidental jumps.
- Add word highlighting only when the native engine supplies timing ranges;
  retain passage highlighting otherwise. No word timing feature is implemented
  by this controller redesign.

These are follow-up recommendations, not shipped changes. Physical Tab A9 audio
quality and frame rate still require device verification.
