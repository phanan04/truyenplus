# CapCut TTS feasibility — 2026-10-15

Reviewed upstream source, not installed/executed: [repository](https://github.com/K07VN/capcut-tts-api), [client.py](https://github.com/K07VN/capcut-tts-api/blob/main/capcut_tts_api/client.py), [models.py](https://github.com/K07VN/capcut-tts-api/blob/main/capcut_tts_api/models.py), [configuration](https://github.com/K07VN/capcut-tts-api/blob/main/capcut_tts_api/config.py).

This is an online Python client, not local model weights or an Android engine.
It submits an SSML task, polls a second endpoint and returns generated audio.
An Android integration would need a Kotlin/network adapter and audio playback, or
an authenticated service hosting the Python client. Shipping Python just for this
feature would add a runtime without making it offline.

The client signs requests using a desktop protocol profile. The app adapter uses
its own randomly generated installation identity; it ships no captured account,
session or default upstream device ID. After the user explicitly approved online
integration, a live probe submitted one Vietnamese sample successfully: create
0.40s, completed result 1.93s. This is one network sample, not a long-session or
Android playback measurement. No personal chapter was uploaded by that probe.

TTS requests set subtitle timestamps off. The SDK's parsed word timestamps belong
to STT; they do not prove word-level timing is available for TTS. The open
[waiting-result issue](https://github.com/K07VN/capcut-tts-api/issues/9) reports a
success-status mismatch, consistent with the current client's exact status check.

Implementation: direct Kotlin/Ktor adapter behind the domain CapCutSpeechSource,
Android MediaPlayer engine, the existing TTSEngine lifecycle and one-sentence
lookahead. No Python runtime or server is bundled. Three catalog voices appear
in the existing voice manager with explicit online labeling, preview and use.
Speed/pitch are applied during native audio playback, so changing speed does not
resubmit generated audio. Pending network work and temporary audio are canceled
on seek/pause/release. Actual Android playback verification is recorded separately;
do not infer reliability from the one successful endpoint probe.

Android verification: the three catalog voices actually downloaded and played
through MediaPlayer, with sentence ranges, explicit cancellation and resume. The
online engine did not fall back to a different voice. A simulated request timeout
produced a terminal error as expected. Individual request times varied; a passing
voice test is not a guarantee of stable network latency.
