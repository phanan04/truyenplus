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

A source records content only; floating controls stay outside it. Each surface
has an opaque safety base for first capture / window changes, a paper-aware tint,
restrained refraction and a shared performance-oriented rendering policy.
Circles/capsules use percentage rounding; panels use their actual corner radius.
Noise, chromatic fringing, interaction wobble and progressive full-page effects
are not added. Dense scrolling cards remain static to avoid shaders on every row.

The reader disables sampling during active scrolling. Android API 31 and low-RAM
configurations use the legible solid fallback; supported systems use Haze's own
capability-dependent Glass rendering. This is not a claim of physical A9 frame
rate or identical optical fidelity on every GPU.

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
ink positions, real-word marker fallback, shared Haze state, scroll fallback,
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
