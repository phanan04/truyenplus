# Reader focus and Haze 2 Glass

Approved reference: the light/amber and dark/lavender mockup with a static
paragraph wash, a sentence marker and a gutter. The implementation decorates the
existing text layout; it does not replace the reader, change fonts or split speech
for an animation.

## Reading focus

- A static two-stop gradient and a very low-opacity edge lift the active paragraph.
  No blur, animated shader, moving glow or asset is used for paragraph highlighting.
- The existing engine-supplied sentence range is drawn behind each affected text
  line with rounded ends. Real word timing remains optional; none is invented.
- A paragraph without a sentence range still receives its wash/gutter.
- Text positions, paragraph height, selection/seek gestures and reading progress
  remain unchanged. Crossfades animate draw properties, not font metrics.
- Surrounding speech paragraphs dim only slightly while playback is active and
  automatic following is engaged. Manual browsing or pause restores full contrast.
- The existing highlight switch controls the treatment; the existing word switch
  continues to require real engine word boundaries.

## Glass surfaces

The shared backdrop now uses `dev.chrisbanes.haze:haze` and `haze-glass` **2.0.1**.
Pinned upstream release: https://github.com/chrisbanes/haze/releases/tag/2.0.1.
It feeds the existing floating navigation, reader chrome, audio panels and shared
sheet/dialog helpers. Layout and business/navigation callbacks are retained.

A source records content only; floating controls stay outside it. An opaque
safety base covers first capture and window transitions without allowing sharp,
unrecorded controls to bleed through. Colors and optical tone explicitly follow
the app or independently selected reading paper, rather than the host system theme.

Small navigation/search capsules and the shell mini player use Glass, with a light
18–22% tint, the official size-aware Regular edge optics and no secondary refraction-detail layer,
content-normal lighting or chromatic dispersion. The previous inline `optics`
call silently inherited a 0.76 detail intensity; this extra retained layer was
unnecessary for text controls. Haze's fixed Balanced default is used. No animated
shader parameter is driven by playback or an idle timer.

Large sheets, dialogs and reader panels use the Haze 2 **Blur** material (12dp,
62% paper tint, no noise), without refraction. Dense scrolling cards remain static.
Shadows and borders are restrained; there is no second decorative rim over Glass.
Scrolling retains the same source and effect nodes instead of detaching them and
replacing the surface with a solid color on every drag.

Unsupported/low-RAM configurations retain a legible solid fallback. These choices
must be checked using native Android pixels and frame measurements; desktop
rendering and successful compilation alone do not establish Android visual quality
or physical A9 performance. The QA frame test compares both old and new materials
on the same moving source and surface bounds. Haze is an approximation and does
not promise native Apple Liquid Glass pixel parity.

## Required compatibility changes

Published Haze 2.0.1 depends on Kotlin 2.4.20 / Compose 1.12.1; the project catalogs
and Compose test artifacts are aligned accordingly. Compiler stability-file DSL
uses the plural API, and default strong-skipping/intrinsic-remember settings are
no longer set through removed options. Gradle source-directory/task registration
calls use their current equivalents.

The newer compiler exposed an impossible `Command` branch in Page-size estimation
and a legacy local `Result` wrapper incorrectly checked against `kotlin.Result`
from PluginThemeManager. The dead branch is removed and the theme error handler
unwraps the manager's actual result while retaining error recording/fallback.

## Verification

Regression tests check static paragraph tint without sentence timing, unchanged
ink positions, real-word marker fallback, shared Haze state, stable capture during scrolling,
source holes and theme readability. Neutral Glass shading is permitted, but
unrecorded green underlays may not leak through. Android tests separately exercise
native Glass capture/window alignment and actual reader/audio workflows.

Android compilation also exposed a WebView relative-URL check against CatalogLocal
instead of its contained source. It now reads `catalog.source` before checking
HttpSource, preserving the intended URL-resolution behavior.

A native book-opening regression exposed Lifecycle's restricted
ViewModelStore.put(String, ViewModel) ABI changing to Object keys in 2.11. Both
Android and common ViewModel dependencies are aligned to 2.11, and inline book
models are obtained through the public ViewModelProvider factory API. The
per-book owner, post-composition initialization and disposal/clear lifecycle are
retained. This was detected by the actual Android root-to-book flow, not inferred
from compilation alone.

Final verification on 2026-10-15: Android QA compilation and 362 desktop tests
passed. Android API 36 emulator passes covered all 11 reader/navigation scenarios
(including 200% font, phone/tablet, direct seek, scroll and live-session reentry),
two native Glass capture/window-alignment cases, three online audio cases and
three model/controller cases. Phone/tablet layout checks used the same logical
dimensions at reduced render resolution after a cold-boot System UI ANR; that
system-service startup failure preceded the app tests and was dismissed before
the passing runs. Model playback and latency trials used production audio
backends. Physical Tab A9 frame rate and acoustic quality remain unmeasured.


## October 16 approval prototype

The first static preview was rejected because it did not make the distinction from
ordinary blur visible. The revised compact material retains `GlassDefaults.optics`
(the upstream responsive diffusion and curved edge displacement), while explicitly
following the app/reader palette. Large text panels retain diffusion rather than
using decorative refraction. Publication is conditional on the Android performance
and functional checks recorded in `docs/research/GLASS_PERFORMANCE.md`.

`ReaderGlassComparisonTest` renders the pre-Haze manual blur and the proposed
material side by side on Android, over the same illustrative moving cover blocks.
The captures demonstrate material appearance; their export cadence is not an FPS
measurement. `ReaderGlassFrameTest` separately compares the v49 configuration and
the prototype with native frame/GPU metrics after warm-up, in both run orders.
Emulator results must not be presented as a Tab A9 or Redmi measurement.
