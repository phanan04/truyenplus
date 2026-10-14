# Vietnamese speech in Truyện+

Android speech remains the default. Two optional, locally synthesized voices use the pinned Sherpa-ONNX 1.13.8 Android runtime. Models are downloaded separately; neither model is embedded in the APK. No API key or server is needed after installation.

## Engine selection

| Candidate | Decision | Reason |
| --- | --- | --- |
| Android TextToSpeech | Keep as default and fallback | Uses installed Android voices and the existing foreground service. Voice quality depends on the installed provider. Actual `onRangeStart` offsets drive sentence highlighting. |
| Sherpa-ONNX | Integrate as runtime | Official Kotlin/Android JNI API supports the two tested Vietnamese model families. It is a runtime, not a separate voice. |
| Piper VAIS1000 medium INT8 | Optional download, about 21 MB | One Vietnamese voice; lower memory use in the measured short samples. Loaded through Sherpa VITS, not a Python/Piper process. |
| Supertonic 3 INT8 | Optional download, about 123 MiB | Vietnamese, ten speakers; higher memory use. Three generation steps are used to reduce latency. Original Supertonic and Supertonic 3 are not interchangeable. |
| Edge TTS | Excluded | Already removed after repeated playback failures. A consumer online endpoint does not provide the reliability needed for background reading. |

Sources: [Sherpa Vietnamese models](https://k2-fsa.github.io/sherpa/onnx/tts/all/Vietnamese/index.html), [Supertonic 3 Vietnamese and Android API](https://k2-fsa.github.io/sherpa/onnx/tts/all/Vietnamese/supertonic-3-vi.html), [Sherpa runtime release](https://github.com/k2-fsa/sherpa-onnx/releases/tag/v1.13.8), [Piper voice model card](https://huggingface.co/rhasspy/piper-voices/blob/main/vi/vi_VN/vais1000/medium/MODEL_CARD), [Piper engine](https://github.com/OHF-Voice/piper1-gpl), [Supertonic project](https://github.com/supertone-oss-archive/supertonic), [Edge client](https://github.com/rany2/edge-tts).

## Architecture

- `OfflineVoiceCatalog`, `OfflineVoiceStore` and `OfflineVoiceConfig` are domain contracts. The existing `TTSEngine` event protocol and `TTSController` retain chapter/paragraph navigation.
- `AndroidOfflineVoiceStore` streams fixed official HTTPS archives, checks exact size and SHA-256, rejects unsafe tar entries and installs atomically into app-private, non-backed-up storage. Only verified installed models can be selected. Downloads can be cancelled and models removed.
- `VoiceAudioBuffer` retains only the upcoming sentence. Playback awaits existing synthesis rather than generating the same sentence twice; pausing cancels consumed pending work.
- `AndroidOfflineTtsEngine` serializes native inference on a worker thread. Playback and auditions share one loaded model, bounded sentence PCM and one pending sentence. It uses mono PCM16 `AudioTrack` at the generated sample rate and limits peaks to prevent digital clipping.
- `TTSEngineFactory.createNativeEngine()` selects the verified on-device model, or the existing Android provider. `EngineType.NATIVE` remains the on-device family, so notification, audio focus, chapter continuation and reader progress do not gain competing playback paths. Initialization/synthesis failures fall back to Android TTS; `engineName` reports the engine actually starting playback.
- The voice manager allows download, delete, audition and speaker selection. Audition pauses active narration, uses the same runtime, and resumes the retained chapter when dismissed. Desktop/iOS explicitly return unsupported for this Android runtime.
- Sentence positions are real engine ranges, mapped to original UTF-16 chapter offsets. Android range callbacks may provide finer positions; offline voices report sentence chunks when `AudioTrack` starts. No guessed word timer is used.
- Reading controls sample the chapter backdrop; scrolling cards use a static translucent gradient. Sampling is suspended during user scrolling. The player and font panel inherit paper/ink colors. Panel state allows one settings overlay at a time.
- The session-owned timer counts only actual `PLAYING` time, pauses during `PAUSED`/`LOADING`, and survives the reader screen being disposed. It does not claim persistence after Android kills the entire application process.

## Measurement protocol and limits

Physical device: Redmi Note 11 (2201117TG/spes), Android 13/API 33, ARM64. The initial JNI probe synthesized two fixed Vietnamese sentences per model using two CPU threads. It is not a long-chapter benchmark and does not establish Tab A9 performance or subjective voice quality.

| Model | Initialization | Synthesis | Audio length | Real-time factor | Observed native heap |
| --- | --- | --- | --- | --- | --- |
| Piper INT8 | 3.65 s | 3.17–3.23 s | 4.21–4.30 s | about 0.75 | 124–210 MB |
| Supertonic 3 INT8, 5 steps | 1.79 s | 10.60–11.43 s | 8.89–9.63 s | about 1.19 | 376–384 MB |
| Supertonic 3 INT8, 3 steps | 2.22 s | 7.03–7.52 s | 8.89–9.63 s | about 0.79 | 376–384 MB |

The final production Android engine was also exercised through the built APK on the physical Redmi, using `AudioTrack` rather than only generating WAV files. Both models emitted actual Started/RangeStarted/Completed events and passed 2x speed, pitch 1.25, cancellation and resumed playback. Piper's test sessions completed in 3.11 s / 2.45 s / 3.48 s; Supertonic's in 6.67 s / 3.63 s / 8.56 s. These include synthesis and playback of different short texts, so they are not directly comparable synthesis benchmarks.

That test exposed a real JNI ABI failure: an indy/SAM Kotlin callback lacks the `invoke(float[]): Integer` method requested by Sherpa. A named typed callback and precise R8 keep rules fixed it; both production-engine tests were rerun successfully. All five Android QA tests passed on the API 36 emulator: official Piper HTTPS download/SHA-256/install/select/playback, Supertonic speaker 0 and 9 playback, Android system playback, automatic chapter continuation plus background/pause/resume/timer expiration, and the real reader UI. The UI test covers system fonts, settings dismissal/exclusivity, compact chapter controls, actual voice audition/selection, spoken sentence metadata and manual scroll/Return. Forty-five selected desktop tests passed, including nine responsive windows and metadata backpressure. Engine adapters discard old range metadata rather than rejecting the final completion when a reader is busy. Player labels use actual engine metadata; falling back releases the unused model lease. Full application installation on the Redmi was blocked by MIUI's USB-install setting; its existing app/library were not replaced. The device's engine proof ran in a dedicated temporary directory.

Real-time factor is synthesis time divided by generated duration; lower is faster. Warm model reuse avoids reinitializing for every sentence. A peak over 1.0 was observed in raw Supertonic output, motivating normalization before PCM16 conversion.

`android/src/qaTest` contains device checks for official download/install, real audio start/completion/ranges, cancellation and resumed playback, model switching, automatic chapter continuation and timer lifecycle. Build with `-PtruyenplusQa=true` to use `.debug.qa`, never the user's existing `.debug` application. Local logs and audio samples are in ignored `.opencode/tts-research` and `.opencode/release-tools`; device results must be read before marking a model validated.

## Distribution references

The runtime is [Apache-2.0](https://github.com/k2-fsa/sherpa-onnx/blob/master/LICENSE). Its third-party native dependencies retain their upstream notices. The VAIS1000 model card identifies a [CC BY 4.0 dataset](https://creativecommons.org/licenses/by/4.0/); [Piper voice licenses](https://github.com/OHF-Voice/piper1-gpl/blob/main/docs/VOICES.md) are model-specific. Supertonic source is MIT, while [Supertonic 3 weights](https://huggingface.co/Supertone/supertonic-3) use OpenRAIL-M. Model archives and original notices are obtained from the linked upstream releases, without repackaging weights into Truyện+.
