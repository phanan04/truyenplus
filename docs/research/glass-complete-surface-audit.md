# Glass coverage: controls and secondary flows

This follow-up extends the existing `truyenplus_full_glass` preference. It does not add another appearance mode. The preference is still off by default and disabling it restores the previous Material controls and existing floating Glass surfaces.

## Remaining gaps found in v2.0.53

The main panels and cards already used the shared material, but many children were still plain Material controls: outlined voice/music choices, model/provider chips, form fields, update/import/export actions, collection actions, reader customization choices and speed presets. Secondary utility toolbars, category-rule surfaces and temporary feedback were inconsistent. The EPUB export panel had an opaque book metadata card.

## Surface mapping after the follow-up

| Flow | Shared treatment |
| --- | --- |
| Home, library, repository, search, account | Existing panel/card material, Glass action buttons, circular actions, collection/scope controls and recent-search actions |
| Book details and table of contents | Glass read/listen action area, chapter-list entry, searchable input, filter chips and current-chapter rows |
| Reading and listening | Existing player/header/footer material; matching typography choices, tabs, speed presets/adjuster and voice/music selectors |
| Voice management | Matching offline/online cards, voice choices, rename input, download/preview/use actions |
| Import and app updates | Existing panel material plus matching actions, address inputs and feedback |
| Chapter editing, replacement rules, character memory | Matching forms, provider/mode/model choices, preview panels and save/apply actions |
| Collections/categories, fonts, audio and replacement utility routes | Matching cards, toolbars, circular actions, rule surfaces, form controls and floating add actions |
| Export EPUB | Diffused panel, inherited metadata/options material and matching formatting/export actions |
| Snackbar messages | One diffused feedback surface with readable message, dismiss and action colors |

`ReaderGlassControls.kt` is the shared adapter layer. It keeps Material input handling, IME options, validation, focus outlines, click/disabled semantics, chip selection and touch target behavior. Only the full-mode material changes. Selected controls retain an explicit tint/border and semantic state; error colors remain meaningful.

`ReaderGlassGroupSurface`, `ReaderOptionalGlassCard` and `ReaderGlassPanelColumn` share diffusion with descendants. Child controls do not independently recapture the raw page behind a panel. Standalone controls use the existing lower-z capture. Buttons, inputs and icon controls pass their rounded geometry to the existing Haze 2 Glass renderer rather than using a mismatched fixed capsule.

Chapter text, book artwork, progress data, color-paper samples and state indicators remain sharp. Empty page space is the page background, not a separate transparent window. Android system pickers, permission/install dialogs and the keyboard are controlled by Android and are outside app styling.

## Verification

The control regression checks real rendered on/off pixels, exact off restoration, disabled semantics, selection, text editing and retained composition across material changes. Existing screen and native Android journeys cover reader/listening actions, both themes, persisted preference and frame durations. Final measurements and screenshot evidence are recorded in the session verification notes; emulator measurements are not certification on A9/Redmi hardware.

### Measured native frame durations

Optimized QA APK, API 36 emulator, warmed ART, no concurrent build. These describe the measured journeys, not A9/Redmi certification.

| Journey | Standard median / p95 / max | Full Glass median / p95 / max |
| --- | --- | --- |
| Navigation and account | 17.83 / 38.77 / 250.82 ms | 19.58 / 57.29 / 283.88 ms |
| Reader, options and scrolling while speaking | 18.66 / 60.02 / 368.31 ms | 22.92 / 68.21 / 367.91 ms |

Both modes recorded zero frames over 700 ms. Full coverage has additional rendering cost and remains optional. Nested controls reuse diffusion to avoid redundant Glass captures. Disabling full mode restores the previous appearance; it does not stop background narration.

377 desktop tests passed with zero failures, errors or skips; 9 release-tool tests passed. The Android RenderNode tests, persisted preference during real system narration, light/dark panels and both native journeys passed. The multi-book navigation test first lacked its required second-book fixture; installing the existing QA fixtures made the complete navigation journey pass without a product-code change. Actual APK screenshots were inspected for contrast and panel diffusion.

The final Android checks also passed on a 320dp phone viewport with 200% system font and on an 800dp tablet viewport, including continued narration and immediate mode restoration. Total: 12 successful Android checks after the fixture correction. Screenshot evidence comes from the actual optimized QA APK, including the new control materials; no independent preview was used as release evidence.
