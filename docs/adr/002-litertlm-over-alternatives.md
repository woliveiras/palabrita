# ADR 002 — LiteRT-LM for On-Device LLM Inference

## Context

Palabrita requires on-device LLM inference to generate word puzzles and power post-game chat without any network dependency. Several runtimes exist for running LLMs on Android. We need to choose one.

### Options Evaluated

| Runtime | Vendor | Model Support | Android Integration | Status |
|---|---|---|---|---|
| **LiteRT-LM** | Google DeepMind | Gemma 3 1B, Gemma 4 2B | First-party, Jetpack-aligned | GA |
| MediaPipe LLM Inference | Google | Gemma 2B, Gemma 7B | Higher-level abstraction | Deprecated in favour of LiteRT-LM |
| ONNX Runtime | Microsoft | Wide (any ONNX model) | Community bindings | Mature, generic |
| llama.cpp (JNI) | Community | Llama, Mistral, quantized | Manual JNI bridge required | No official Android SDK |
| ML Kit GenAI | Google | Gemma Nano only | Very high-level | Limited (Pixel-only via AICore) |

## Decision

Use **LiteRT-LM** (formerly MediaPipe LLM Inference, rebranded 2025) with Gemma 4 E2B and Gemma 3 1B models.

## Rationale

1. **Official Gemma support** — LiteRT-LM is the Google-recommended runtime for Gemma models on Android. The model weights, tokenizer, and GPU/NPU acceleration are all validated for this runtime.

2. **Hardware acceleration** — supports GPU delegate and, where available, NPU via Android NNAPI. No manual configuration needed; the SDK selects the best backend automatically.

3. **Play Asset Delivery integration** — model files distributed as asset packs are consumed directly by LiteRT-LM without format conversion.

4. **Jetpack-aligned lifecycle** — the SDK integrates cleanly with `ViewModel` and `coroutines`. Sessions are managed as closeable resources, suitable for our `StateMachine`-based engine lifecycle.

5. **Streaming tokens** — supports token-by-token streaming via a callback interface, enabling the real-time chat UI in `feature/chat`.

6. **Active maintenance** — backed by Google DeepMind, aligned with the Gemma model release cadence. MediaPipe's LLM Inference API has been officially superseded by LiteRT-LM.

## Trade-offs

- **Gemma-centric**: LiteRT-LM is optimised for Gemma. Running other model families requires format conversion and is unsupported.
- **SDK size**: adds ~4 MB to APK. Acceptable given the model itself is 1–2 GB.
- **API surface**: lower-level than ML Kit GenAI, but this gives us control over prompt engineering and session lifecycle.

## Consequences

- `core/ai` wraps `LlmEngineManager` behind a `PuzzleGenerator` interface, isolating the rest of the codebase from the SDK.
- If a future runtime becomes the recommended Gemma runtime, only `core/ai` needs to change.
- ONNX Runtime remains an option if non-Gemma models are needed in a future release.
