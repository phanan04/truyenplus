# Apple Books checkpoint 25 → Truyện+

User request: replace the existing UI with the supplied local reference package. The implementation follows the corresponding native screens and preserves the app's existing reading, listening and source workflows.

## Reference authority

`/Users/pan/Downloads/apple_books_checkpoint25_visual_qa_230screens/`

230 unique numbered screen states (000–229), 3 overridden exports, and a component sheet. The original screenshots take precedence over reconstructed SVG when they differ. SVG text and shapes are useful measurements, not executable UI or interaction specifications. No supplied Python scripts are executed.

## Screen and flow mapping

| Reference | Production screen / behavior |
| --- | --- |
| Home 007–030 | Home: continue the actual last-read book, actual pinned/local shelves, real chapter progress |
| Library 031–064 | Library: covers first, collection/sort/selection menu, grid/list, import, persisted filters |
| Search 074–080 | Search workspace: bottom search capsule, library/repository scope, real recent queries and results |
| Book details 081–100 | Actual EPUB metadata/cover/description, read/listen/export/contents/metadata repair |
| Reader 119–136 | Paper and chapter content, quiet header, floating bottom tools and actual chapter position |
| Reader navigation 137–145 | Contents and reading actions, existing chapter navigation/bookmarks |
| Appearance 146–163 | Quick type capsule, brightness, named paper cards in 3×2 arrangement, full-width customization |
| Typography 164–179 | Existing system fonts, alignment, line/paragraph spacing, margins, reading mode |
| Advanced reader 180–194 | Existing scroll, immersive mode, search and accessibility behavior |
| Actions 195–206 | Book/collection actions in shared contextual menus and sheets |
| Account and remaining states | Settings, language/theme, voice/model management, updates and editing tools |

The Drive repository remains search-first. It never downloads thousands of EPUBs to draw a store-like catalog. TTS keeps the current Android/Piper/VieNeu/CapCut engine, buffering, seek, progress, background timer and offline models. No fake store, prices, ratings, categories, download counts or daily reading goals are added.

## Measured visual rules

393×852pt source: 32pt main gutter, 34pt editorial page title, 24pt section title, 156pt shelf covers (ratio .64), 242–244×80pt continue tile, 62pt floating navigation/search, 24pt navigation icons and 10pt captions. Source 146 uses a full-width 36pt-radius sheet, 48pt segmented type controls, three 110×92pt theme cards per row and a full-width 48pt Customize capsule.

48dp minimum interaction targets remain available on Android. Large text, narrow/landscape windows and tablet layouts adapt rather than scaling a fixed image. Reference colors follow the actual selected paper and the app's light/dark theme. Glass belongs to containers; controls within a material reuse its capture rather than adding nested blur/borders. No bitmap screenshot is used as the app UI.

## Verification

Production Compose geometry/action tests, rendered production screens, Android installed-app navigation/read/listen flows and responsive screenshots. Performance must be measured on the installed runtime; emulator measurements are not Samsung A9 device guarantees. Existing business-logic tests remain in place.

## Implemented structure

- Home keeps the reference's compact artwork-colored continue tile and cover shelves. Real reading progress has a small header shortcut and a semicircle instead of the previous oversized dashboard ring.
- Library uses the reference header actions, cover proportions, tighter vertical rhythm and shared contextual menus. Collection, grid/list, import, sorting and selection callbacks are retained.
- Detail uses a 186pt cover with the source's .64 aspect ratio, artwork-colored hero, centered metadata and balanced named Read/Listen actions. Large text stacks actions; the chapter sheet uses the actual book cover, title and chapter positions.
- Reader uses a quiet paper-backed caption with trailing close, a small lower toolbar and the reference's expandable vertical reading menu. Contents, previous/next, bookmark, auto-scroll, appearance and listening remain connected. Opening another panel closes the menu.
- Appearance uses the full-width rounded sheet, divided type capsule, brightness, six named paper cards and a full-width Customize action. All fourteen saved paper IDs and advanced typography/scroll controls remain accessible. Narrow, short and large-text windows page the paper choices without clipping Customize.
- Shared navigation, sheets, contextual menus and sheet close controls follow the measured proportions. Inline text/icon controls reuse their parent's glass rather than add independent lenses. Android system fonts and 48dp interaction targets are retained; supplied screenshots are not rasterized into interactive controls.
- The mutable Glass preference tracks its consumers instead of invalidating the complete provider subtree. A composition regression compares it with a static-provider control and verifies that unrelated content stays untouched while the material changes.

Final local QA screenshots and reports are under `.opencode/reports/checkpoint25-final/` and are excluded from application assets. QA book fixtures, test classes and downloaded voice weights do not ship in release APKs.

## Verification results — 2026-10-16

388 desktop tests passed with zero failures, errors or skips. Nine release-integrity tests passed. The optimized Android QA app and instrumentation APK built successfully after the final material-invalidation change.

21 installed-Android checks passed: five RenderNode/material checks, root navigation/search/details, both native performance journeys, real Piper installation/playback, independent app/paper themes, appearance layout, exclusive reader panels, direct paragraph seek, chapter switching, immersive scrolling, listening re-entry, voice selection, light/dark utility sheets, expandable reader menu, 320dp phone with 200% system text and an 800dp tablet. Sixty captured screenshots were collected; the source-mapped screens, independent paper colors and responsive sheets were inspected.

The first CI run subsequently caught an English-only 360dp detail layout failure: “Continue reading” wrapped to two lines, making Read 60dp versus Listen 48dp. This was reproduced locally with `JAVA_TOOL_OPTIONS='-Duser.language=en -Duser.country=US'`. Detail actions now use compact localized Read/Continue labels and one-line text, while accessibility retains the complete reading action description. The existing regression covers both first-read and resume states without relaxing its 48dp balance requirement. After this wording-only correction, all 388 tests passed again in each explicit English and Vietnamese locale; native screenshots/timing above precede that final label correction.

Final installed-runtime timing on the API 36 x86 emulator (1080×2400, 480dpi, system font 100%; milliseconds):

| Mode / journey | Frames | Median | P95 | Maximum | Frames >700ms |
| --- | ---: | ---: | ---: | ---: | ---: |
| Standard / navigation and account | 589 | 18.06 | 51.49 | 165.34 | 0 |
| Standard / reader, settings, listening and scroll | 673 | 22.09 | 84.20 | 353.03 | 0 |
| Full Glass / navigation and account | 563 | 18.09 | 54.44 | 401.86 | 0 |
| Full Glass / reader, settings, listening and scroll | 818 | 20.56 | 67.59 | 234.35 | 0 |

These are native `FrameMetrics`, not a Compose test-clock estimate or a 60fps guarantee. Early runs before the final invalidation correction recorded 1–3 frames above 700ms; their reports remain under `checkpoint25/`, including the repeated failures. Final timing is under `checkpoint25-final/qa-performance/` after a fresh emulator start. Host scheduling and warm-up affect comparisons, so the measurements do not establish a device-wide speedup or certify Samsung A9/Redmi performance. The slow 500µs method-sampling attempt produced no usable trace and is excluded from timing results.
