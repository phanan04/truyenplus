# Reader/listener review — 2026-10-15

The review follows tasks a reader performs, not just isolated screens. Device
results and latency measurements belong in the verification section when run.

| Journey/problem | Correction |
|---|---|
| Scrolling chapter text remains sharp behind the chapter identity | Give the identity its own paper-aware frosted capsule, matching close/Aa |
| A listening tap opens an extra menu instead of seeking | Direct mapped-paragraph seek through the existing IReader controller; first immersive tap works too |
| Selection gestures intercept listening taps | Prioritize playback taps while listening; preserve normal-reading selection |
| Swiping leaves floating controls over the story | Manual scroll hides chrome/dock; tapping restores them without stopping TTS; smart follow does not hide them |
| Scroll inspection loses the listening position | Suspend follow until explicit return or paragraph selection; keep a small return action clear of the dock |
| Every paragraph briefly reports preparation | Delay visual preparation feedback by 350ms; retain immediate pause and show genuine longer waits |
| Highlight is a weak gray rectangle | Warm sentence-only line highlight, small gutter marker and gentle fade; word tint only with real timing |
| Choosing a network voice looks like downloading an offline model | Separate labeled CapCut online card in the existing manager, with preview/use and no model download |
| Voice preview and story speak over each other | Existing preview lifecycle pauses narration, cancels previous auditions and restores the listening state |
| Network failure may advance repeatedly over unread paragraphs | Fatal online errors must stop at the current paragraph and allow retry; transient native synthesis behavior remains unchanged |
| Scrolled chapter glyphs overlap Android's clock/gesture area | Protect only system insets with the reader's paper color and keep content padding when chrome hides |
| Navigation/search hugs the bottom safe-area edge | Share a 12dp bottom clearance; measured dock height updates content clearance automatically |
| Returning through the mini player opens reading while narration continues | Serialize/decode listening mode and paragraph in the actual navigation route; resume actions use the live controller chapter/position |
| Mini player briefly shows resume while buffering | Keep pause visible through both PLAYING and LOADING, matching notification and reader controls |

Test matrix: phone, 200% system font, tablet; independent paper/application light
and dark colors; loading, errors, preview cancellation, direct seek, compact and
expanded player, chapter navigation, manual scroll/return, background playback and
timer. No new top-level navigation or business-data migration is required.

Performance notes: the app currently prepares one upcoming sentence in memory.
This bounds memory and cancellation work. It cannot guarantee continuous audio
when generation is consistently slower than playback, especially at 2x. Offline
inference depends on CPU/model/thermal conditions and competing tasks; online
TTS additionally depends on network/server response. A short LOADING transition
is not by itself evidence of an audible stall. Compare first-audio time and real
Completed→Started gaps independently from loading-label time. Emulator results
must not be presented as measurements on a Samsung A9.

## Verification

- All 350 presentation host tests passed, zero failures/errors/skips, after the
  final navigation compatibility correction (`reader46-update-compatibility-build.log`).
  The earlier system-inset build passed 345 and the narrower reader/product suite
  passed 234 cases as well.
- Warm highlight screenshots inspected in light and dark paper. Timing remains
  engine-driven; no fabricated word timestamps or word-by-word animation.
- Six native audio cases passed: all three CapCut voices through MediaPlayer,
  terminal timeout handling, official Piper/Nano models, actual controller
  continuity at 1x/2x and background chapter/timer lifecycle
  (`reader46-real-audio.log`). See the separate latency report for sample limits.
- Six native reader journeys passed on the initial build: real finger seek,
  manual scroll with audio still playing, independent paper/application themes,
  200% font, CapCut preview/use/highlight and tablet scroll
  (`reader46-user-journeys.log`).
- Reviewing those screenshots uncovered system-bar bleed. A new whole-device
  pixel assertion reproduced 1009 dark chapter pixels in the white status area
  before the inset-only correction (`reader46-system-bars-red.log`).

The final ten native UI journeys all passed after the inset fix, including root
navigation, repeated font-panel changes, live voice selection, first-touch seek,
scroll, independent paper themes, 200% font, CapCut and tablet. The pixel check
distinguishes black chapter glyphs from the floating player's light gray shadow;
the playback check allows a seek's temporary LOADING state and still waits for
actual PLAYING. Evidence: `reader46-final-native-*.log`. Six real-audio cases
passed separately.

The bottom-clearance/reentry follow-up passed real phone and tablet journeys,
including measured 12dp spacing, opening the active mini player and Continue,
retaining the live chapter/paragraph, then pausing outside and reopening without
restarting audio. The complete root navigation/search/library journey passed
again (`reader46-reentry-native.log`). The original reader destination identity
is retained for previously saved navigation stacks; the listening route carries
required mode/paragraph arguments separately. After that compatibility change,
the complete phone reentry case passed again (`reader46-update-final-native.log`).

Network recovery now distinguishes a terminal online failure from a transient
native synthesis problem. A failed/expired CapCut request stops at the same
paragraph, shows a retry/change-voice action and clears the error on retry.
Request timeouts are reported as errors; deliberate pause/seek cancellations stay
silent. The offline AudioTrack startup timeout also reports an event instead of
silently abandoning its coroutine and leaving the controller loading forever.

A further voice-switch regression was reproduced: speed/pitch labels stayed at the
chosen values while the replacement engine silently returned to 1x. Engine
initialization now reapplies the current controls. The regression test failed at
expected1.5/actual1.0 before this correction; other continuous-playback and network
recovery tests passed in that run.
