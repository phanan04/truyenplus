# Full-system Glass and CapCut recovery

## Scope and compatibility

The setting **Cài đặt → Glass toàn hệ thống** (`truyenplus_full_glass`) defaults to false. Existing floating controls retain their current Compact Glass/Frosted treatment with the switch off. Enabling it changes panel material to Haze 2 Glass Regular and opts additional cards, menus and utility-screen surfaces into the same material. Turning it off restores each component's previous color and elevation; the controller and navigation remain mounted.

The preference provider lives above navigation, popup windows and the independent reading-paper theme. Reader surfaces take their tint and foreground from the selected paper; app surfaces use app appearance. Android requires API 32 or later for this capture path. The existing low-RAM exclusion remains in standard mode; full-system mode is an explicit opt-in on supported low-RAM devices. This is not a guarantee of smooth Glass on every device.

## Audited surface mapping

| Surface | Treatment when enabled |
| --- | --- |
| Bottom navigation, separate search button, global mini player | Existing shared Compact Glass |
| Reading header/footer, return-to-audio control, compact/expanded player | Existing shared Compact Glass |
| Text/listening/voice options, account, source management, import, editing, character memory, book actions, update/progress panels | Shared panel material switches from Frosted blur to Glass |
| Continue-reading tile, reading-progress rows, settings groups, source cards, repository result rows, editor preview, collection rows | Optional Glass with transparent Material container |
| Library collection/organize controls, account/back buttons, standalone search fields | Optional Glass; control geometry/actions retained |
| Font, category, replacement and audio utility routes | Shared capture provider; card/menu/dialog/toolbar opt-in |
| Book artwork, chapter text, paper swatches, selected chips, meters, text/icons | Their semantic colors remain; these are not backdrop surfaces |

Common helpers are `readerPanel`, `readerGlassCard`, `readerAppGlassCard`, `readerOptionalGlass`, `ReaderOptionalGlassCard` and `ReaderGlassDropdownMenu`. `ReaderGlassPanelContent` lets nested cards inherit the panel material; new popup roots use independent panel diffusion. Removed Discovery and unshipped IReader destinations are not newly exposed or restyled.

## Capture and performance

Haze receives a page capture at z=0 and, only in full mode, a lower paper-only capture at z=-1. A card inside the page uses the lower source rather than capturing its own text. Floating overlays outside the page can consume the recorded page. This follows Haze's default `Behind` source selection. Cards inside an already diffused panel reuse that panel instead of capturing the sharp page again. This avoids both a readability regression and redundant Glass passes on settings groups and voice cards. A separate popup still gets its own diffusion. No extra content-normal/refraction layers or chromatic aberration were added. Desktop and Android tests check mode switching, source placement, idle stability and readable controls. Actual APK screenshots and Choreographer measurements are used for device validation; independent shader demos are not evidence of shipping-app performance.

Sources: https://chrisbanes.github.io/haze/2.0.1/core-concepts/ and https://chrisbanes.github.io/haze/dev/effects/glass/ (Regular optics vary with surface size; small nested cards must not override a large panel’s diffusion).

## CapCut fixes

`JumpToParagraph` previously resumed only if playback had reached PLAYING. CapCut may still be LOADING when the user taps another paragraph. Stopping then canceled synthesis with no replacement, leaving the loading state stuck. Seeking now replaces either active or preparing audio, invalidates the old utterance and clears its loading/highlight markers. A deliberately paused session still stays paused. Engine selection and prepared-audio cache are retained.

CapCut can return HTTP 200 with JSON `ret=-6`. The common HTTP transport now recognizes this rejection, returns a typed safe error and shares a 60-second cooldown across requests made through the client, including lookahead jobs. It does not automatically resubmit rejected synthesis tasks. The visible message explains waiting and pressing play again, or selecting another voice. It does not skip story content. Already downloaded audio stays usable.

The upstream repository has reports of `-6` with `shark block only`: https://github.com/K07VN/capcut-tts-api/issues/5. This is evidence of a provider rejection, not an official statement of its exact policy or recovery time. A 60-second local pause does not guarantee the service will accept the next request. No identity rotation or block bypass was introduced. Native recovery tests use a deterministic WAV fixture and must not be described as proof of provider availability or voice quality.

## Validation

See the session verification report for final test counts, native results and release revision. No Samsung A9 or Redmi performance claim should be inferred from emulator measurements.

### Native acceptance after removing nested capture

Optimized QA APK on isolated API 36 emulator, actual Android FrameMetrics, warmed ART, no concurrent build. These are measured frame durations, not a physical-device FPS guarantee.

| Journey | Standard median / p95 / maximum | Full Glass median / p95 / maximum |
| --- | --- | --- |
| Shell navigation and account controls | 17.73 / 39.78 / 148.01 ms | 17.95 / 50.32 / 367.28 ms |
| Reader settings, listening and manual scroll | 18.31 / 51.59 / 234.81 ms | 19.59 / 55.78 / 200.56 ms |

Both journeys recorded zero frames over 700 ms in both modes. Full Glass still has extra rendering cost; it remains an opt-in, with the off switch immediately available. A previous full-mode account opening produced an 801.74 ms frame; that build was rejected. The accepted native suite has 11 passing checks, including real RenderNode Glass, nested diffusion, loading seek/recovery through Android MediaPlayer, mode persistence during real system narration, both themes and the two native journeys. Not measured on A9/Redmi.

A separate one-request live CapCut cache probe passed: first audio 1,895 ms, replay 95 / 99 ms, network request count stayed at one across three plays. This is one successful service sample, not a promise of sustained upstream availability.

Final checks: 375 desktop tests and 9 release-tool tests passed. The final utility-route guard keeps capture disabled when Glass is off, without remounting content. Two additional APK journeys passed on a 320dp phone viewport at 200% system font and on an 800dp tablet viewport, including the persisted switch, reading options and continued background narration. The switch test scrolls to the control when needed rather than assuming it is visible at large font sizes. No business data, source APIs or offline voice models were replaced.
