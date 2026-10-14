# VieNeu v3 Nano Android

Uses the official 11 preset voices and the official sea-g2p C API. No Python runtime,
server, API key or cloning model is required by the app. Models and the pronunciation
dictionary download separately (346,903,105 bytes) with pinned SHA-256 checks.

The adapter runs four ONNX graphs, 8 Euler steps, sway -1, CFG 3, 24 kHz mono.
Speech generation is off the UI thread and shared with voice previews. Sentence
highlighting uses actual AudioTrack playback; word timestamps are not fabricated.

Build requirements: Android NDK 28+, Python 3, Rust 1.89+ with targets
`aarch64-linux-android`, `x86_64-linux-android`, `armv7-linux-androideabi` and `i686-linux-android`. Run:

```
python3 scripts/build-vieneu-native.py
```

Libraries are built into `android/src/main/jniLibs`, ignored by Git. Android Gradle
invokes the script before packaging. The existing sherpa-onnx dependency supplies
ONNX Runtime, so the APK does not contain a second runtime.

Upstream code and licenses are retained under `sea-g2p`, `ort` and `NOTICE`.
The mobile adapter does not claim the upstream desktop RTF on phones/tablets.
