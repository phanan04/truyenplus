# TTS latency verification — 15 October 2026

Measured on the isolated Android API36 x86_64 emulator, two cores and 2GB guest
RAM, after the host build finished. This is not a physical Samsung Tab A9 result.
The real TTSController loads three Vietnamese paragraphs, plays them without
manual intervention, and checks the actual engine identity to reject fallback.
Each row contains two Completed→Started paragraph gaps. Timings are engine
playback events, not a waveform measurement of every silence inside a voice.

| Engine | Speed | First requested audio after engine ready | Paragraph gaps |
|---|---:|---:|---:|
| Android system TTS | 1x | 221ms | 91 / 59ms |
| Android system TTS | 2x | 537ms | 84 / 52ms |
| Piper int8 | 1x | 1197ms | 75 / 27ms |
| Piper int8 | 2x | 496ms | 42 / 39ms |
| VieNeu v3 Nano, Minh Quân | 1x | 2503ms | 35 / 41ms |
| VieNeu v3 Nano, Minh Quân | 2x | 1367ms | 65 / 35ms |
| CapCut, Nhỏ Ngọt Ngào | 1x | 3670ms | 115 / 100ms |
| CapCut, Nhỏ Ngọt Ngào | 2x | 637ms | 98 / 96ms |

The first-audio column excludes model download and engine initialization. Repeated
runs may benefit from warm native/provider caches. These short samples establish
working continuity; they are not a percentile, battery or long-session benchmark.

The controller correctly enters LOADING at each handoff, even for a 27–115ms gap.
The UI previously displayed all of those transitions immediately. It now waits
350ms before displaying preparation feedback, while pause remains immediately
available. Actual longer waits still show loading. This changes feedback, not the
recorded playback state or timer policy.

Offline and online engines prepare one next sentence while current audio plays.
Natural paragraph completion retains that preparation; pause/seek/voice replacement
cancel it. CapCut generates at 1x and applies chosen speed/pitch in MediaPlayer,
so a speed change does not require another network synthesis request.

Native controls now reapply speed/pitch when an engine is replaced. Request/audio
startup timeouts report errors instead of leaving a loading coroutine abandoned.
Terminal online errors preserve the current paragraph for retry instead of
skipping unread text. Deliberate cancellation remains silent.

## Configuration and network impact

Offline synthesis depends on CPU, the selected model, temperature, memory pressure
and concurrent work. Higher playback speed leaves less time to prepare the next
sentence. Buffering cannot guarantee continuity if sustained synthesis is slower
than consumption. Piper and Nano remain separate choices; no physical A9 ranking
is inferred from this emulator.

System TTS depends on the installed Android engine and voice, which may itself
use the network. CapCut additionally depends on server queue/network/CDN response.
All three exposed CapCut voices passed actual download/play, cancellation and
resume. One resume test took20.97s including audio playback, showing that this
provider can vary substantially; it is an optional online choice, not the default.

Evidence: `.opencode/reports/reader46-audio-all-latency-metrics.log`,
`reader46-audio-capcut-voices-metrics.log`, `reader46-real-audio.log`.
