# TrueVoice code

The Android app, standalone module, web laboratory, model assets and evaluation tools share this application root. Their relative runtime paths are preserved. See the [project overview](../README.md) and [documentation index](../documents/README.md).

## Web laboratory

Run python -m http.server 8080 from this directory, or use the bundled server.ps1. Open http://localhost:8080. Browser assets and model URLs resolve relative to index.html.

For a clean static hosting bundle, run node ops/build-web.mjs from here (or node code/ops/build-web.mjs from the repository root). It copies only index.html, favicon.svg, css/, js/, audio/ and models/ into the ignored .web-dist/ directory. The root vercel.json selects this build and output directory; Android source and local configuration are excluded from the static bundle. No deployment command is part of this change.

## Android

Open this directory as the Gradle project. Use JDK 17 and Android SDK 35. Put machine-specific and provider settings in an ignored local.properties here or in the process environment.

```sh
bash gradlew :app:testDebugUnitTest
bash gradlew :app:assembleDebug
```

Windows users can use gradlew.bat. Device integration checks still require the original permissions, model assets and a compatible physical Android device.

## Evaluation

The labelled benchmark and manifest stay in eval/. Paths in that manifest are relative to this directory. Its model tooling lives in modules/voice-clone-defence/VoiceCloneDefense/tools/. Install its optional dependencies and checkpoints before running python eval/benchmark.py. See [evaluation setup](eval/README.md).
