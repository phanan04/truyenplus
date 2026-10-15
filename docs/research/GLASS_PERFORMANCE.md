# Glass performance verification — October 16, 2026

## Scope and changes

The reported v2.0.49 appearance and severe stalls prompted a material and runtime
review. Compact floating controls now use Haze 2.0.1's official Regular optics,
without its secondary refraction-detail layer. Large text panels use Haze Blur,
not stacked Glass. Scrolling retains capture/effect nodes. The navigation marker
reads animated state in placement instead of recomposing the tabs every frame.
Reading, synthesis, playback queues, chapter data and APIs are unchanged.

The published APK previously enabled `debuggable`. CI now disables it while
retaining the installed package and signing key. A release integrity test rejects
debuggable APKs. The inherited IReader update worker stays disabled when the
Truyện+ manifest updater is configured, including with the optimized runtime.

## Method

- Isolated x86_64 Android API 36 emulator, 1080×2400, density 480, font scale 1.
- Host GPU rendering; not a physical Samsung A9 or Redmi Note 11 measurement.
- QA build: `-PtruyenplusQa=true -PtruyenplusPerformance=true`; APK metadata
  confirmed `debuggable=false`, and ART speed compilation completed before measurement.
- Native `FrameMetrics` plus process CPU and PSS. No frame-rate claim is derived
  from the comparison GIF, screenshot cadence or desktop renderer.
- `ReaderNativeJourneyTest` uses UiAutomator input and the actual Choreographer,
  without a Compose test clock: four navigation/settings cycles, font controls,
  actual system TTS, listening controls and six scroll gestures. It also verifies
  that the inherited update worker was not scheduled.
- Compose instrumentation separately checks repeated interactions, playback
  advancement, idle work and functional regressions. Its injected gesture timing
  can inflate `UNKNOWN_DELAY`; native journey metrics are reported separately.

## Native journey results

| Phase | Frames | Median | p95 | Maximum | Frames >700 ms | PSS |
|---|---:|---:|---:|---:|---:|---:|
| Navigation/settings | 587 | 17.65 ms | 37.65 ms | 134.60 ms | 0 | 149 MiB |
| Reader/settings/scroll while speaking | 761 | 18.19 ms | 51.12 ms | 193.26 ms | 0 | 169 MiB |

The complete native journey passed. Speech advanced during scrolling and remained
active; pausing completed without a stuck loading state. This result rules out
reproducing the earlier multi-second freezes in this run; it does **not** establish
constant 60 fps or eliminate occasional slow frames. During the five seconds just
after pausing, 41 transition frames and 478 ms of process CPU were recorded; that
interval includes control/immersive transitions and is not a steady-state idle baseline.

The separate repeated-interaction checks also passed: reader p95 67.15 ms,
maximum 301.04 ms, no >700 ms frames, and all measured font/listening actions
completed within 367 ms. Shell p95 was 34.26 ms, maximum 101.28 ms, with no >700 ms
frames. Once shell transitions settled, a 3.001-second idle sample recorded zero
new frames and 25 ms of process CPU (about 185 MiB PSS). These are emulator samples,
not comparative physical-device benchmarks.

The complete host suite passed 366 tests with no failures, errors or skips;
the nine release-integrity Python tests passed. Native pixel tests cover backdrop
diffusion, capture holes, separate-window alignment and dark-paper tone. Functional
Android checks cover reader appearance, outside sheets in both themes, direct
paragraph seek, swipe-to-immerse, playback re-entry and root navigation/search.

Earlier debug-runtime cold trials had frames up to 1.9 seconds. Warm reruns alone
were therefore insufficient for acceptance. An exploratory method-sampling trace
identified substantial Compose measure/composition work; tracing overhead makes
its timings unsuitable as release performance numbers. Tests that initially failed
because they clicked while loading or targeted hidden immersive controls were
corrected without weakening the performance budgets.

## Reproduction and limits

Build the isolated app and Android tests with the flags above, then run
`ReaderNativeJourneyTest`, the two performance methods in `ReaderUiDeviceTest`,
the reader/navigation functional methods and `ReaderGlassDeviceTest`.
`ReaderUiFrameProbe` writes native measurements under the QA application's
external `files/qa-performance` directory. Never run the fixture tests against the
production package or a user's library.

Host rendering tests and CI integrity tests complement Android checks; they do
not certify GPU performance. Emulator compilation/warm-up and host load affect
results. Physical-device cold start, long listening sessions, battery/thermal
behavior and large real libraries still need device validation. API/service
latency for online TTS is outside this UI-only change.

Android's guidance on release-like performance measurement:
https://developer.android.com/develop/ui/compose/performance
