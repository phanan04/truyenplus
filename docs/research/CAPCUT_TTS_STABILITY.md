# Online CapCut/TikTok speech: stability and latency

The app's online adapter uses CapCut's asynchronous task protocol. A voice ID
containing `streaming` does not establish that this endpoint streams playable
speech. The current flow is submit → poll → download MP3 → native playback.
Android TTS, Piper and VieNeu remain separate and unchanged.

## Report and evidence

The user reports availability alternating between working and failing. The
reported failure was no longer occurring during the first live check. Three
host endpoint probes subsequently downloaded valid audio. No failing production
HTTP response was captured; throttling, networking or a provider outage remain
hypotheses, not an established root cause.

Cached provider samples on 2026-10-15 (synthetic Vietnamese text, not user books):

| Voice | Create | Result ready, including create | Download | Audio duration |
|---|---:|---:|---:|---:|
| Nhỏ Ngọt Ngào | 378 ms | 643 ms | 621 ms | 6288 ms |
| Nữ Phổ Thông | 327 ms | 588 ms | 719 ms | 7392 ms |
| Thanh Niên Tự Tin | 341 ms | 597 ms | 1077 ms | 6384 ms |

All reported `hit_cache=true`. These are neither cold synthesis measurements nor
Android first-audio measurements. The first Python download probe had a host CA
configuration error, corrected by using the macOS CA bundle; that is not evidence
of an Android/server failure. Native playback results are recorded separately.

## Implemented approach

- A dedicated persistent HTTP client with connect/socket/request timeouts;
  signed responses and story text do not enter shared reader HTTP caches/logs.
- One safe retry for transient polling/download failures. HTTP 429 instead
  establishes a shared cooldown from numeric Retry-After (1–60 seconds; five
  seconds without a usable hint), preventing queued jobs from flooding the provider.
  Task submissions are
  **not** blindly retried, because a timeout may occur after server acceptance.
  Cancellation always propagates; no automatic voice substitution or paragraph skip.
- Reject task audio with an error code/invalid-input flag, oversized responses,
  and HTML/non-MP3 downloads before handing data to MediaPlayer.
- Safe stage/status diagnostics instead of discarding every exception as a generic
  playback failure. Story text, signed audio URLs and task tokens are excluded.
- Up to three forthcoming sentence chunks, with at most two synthesis jobs in
  flight. Playback joins existing work rather than submitting duplicate work.
- Voice-local LRU of ready MP3 bytes, capped at 24 entries / 8 MiB. Pause/seek
  cancels pending work but retains ready audio; engine release clears everything.
  It is a session cache, not an unbounded disk cache.
- Every terminal speculative job, including a coroutine timeout, is removed from
  the pending map. A regression test reproduced canceled lookahead poisoning the
  next demand for the same text, then passed after cleanup moved to `finally`.
  This was a code-audit finding, not a captured production service failure.
- Sentence boundaries and playback speed remain unchanged; visual highlighting
  does not create requests, delay playback or add estimated timing.

Prefetch hides latency only when preparation stays ahead of consumption. It
cannot ensure uninterrupted playback through an outage or indefinitely when
speech generation is slower than the selected reading speed.

## Other options considered

| Option | Benefit | Tradeoff / decision |
|---|---|---|
| More/larger buffered chunks | More tolerance for variable response times | Bounded lookahead chosen; avoid flooding provider or filling RAM |
| Merge sentences into one audio file | Fewer submit/poll/download round trips | Longer first wait; no verified TTS sentence timings. Do not substitute estimated word/sentence timing |
| One task with multiple separate voice blocks | Amortizes submit/poll while preserving individual audio files | A host probe returned two correctly ordered audio items (6288/2304 ms); task ready in 2861 ms, first file download 431 ms. Shared completion waits for the batch. Not enabled without native multi-voice/cancellation/partial-result benchmarks; foreground synthesis remains independent |
| Progressive playback of the returned MP3 URL | May remove the full-download gate for first audio | Requires bounded caching, signed-URL/cancellation/error lifecycle work; it is file streaming, not live synthesis. Candidate if download remains dominant |
| Persistent player / Media3 playlist | Can reduce local player handoff overhead | Does not remove server task latency; measure the updated queue before replacing working playback |
| Regional proxy and persistent shared cache | Potentially fewer mobile round trips | Needs hosting, authentication and a deliberate policy for transmitted story text; no proxy deployed |
| Supported commercial speech API | Documented limits and service support | A separate provider/credentials/cost decision; not silently substituted |
| Word alignment model | Richer timing | Adds work without making audio arrive sooner; not used |

The upstream protocol client is a reference, not an availability guarantee:
https://github.com/K07VN/capcut-tts-api

## Android playback verification

The isolated API 36 QA app ran the production adapter and MediaPlayer for all
three online voices, including cancellation/resume, without another engine as a
fallback. The timeout case also passed.

A single-sentence cache trial at 2x produced first-audio callbacks at 7998 ms on
first generation, then 290 ms and 178 ms on replay after pause. All three plays
used **one** synthesis call in total. This demonstrates ready-cache reuse, not
an 8-second-to-178-ms improvement for previously unseen text. The first generation
includes networking, server processing and local startup; these components were
not individually measured on Android. Callback timing does not measure acoustic
silence inside the returned MP3. No physical Tab A9 latency claim is made.

The real controller also completed three Vietnamese paragraphs at both 1x and
2x on the isolated emulator (one run per engine/rate, same texts):

| Engine | First-audio callback at 1x | Between-paragraph callback gaps at 1x / 2x |
|---|---:|---|
| Android system | 357 ms | 193, 227 ms / 190, 86 ms |
| Piper | 1124 ms | 83, 30 ms / 72, 44 ms |
| VieNeu v3 Nano | 2260 ms | 50, 29 ms / 369, 37 ms |
| CapCut | 4600 ms | 132, 146 ms / 211, 264 ms |

CapCut first audio in the subsequent 2x run was 777 ms. Repeated text and provider
caching can affect that comparison; it is not a speed-dependent latency promise.
These short callback trials verify real playback and controller lookahead, not
long-session availability, acoustic silence or physical-device performance.
Piper download/install/speed/pause/resume and two Nano speakers also passed with
their production Android audio backends after the dependency upgrade.
