# Repository structure

Application source, runtime assets, build manifests, tests and operational tooling live in code/. Project documentation lives in documents/, with project-plan/ reserved for planning. The root README and the license, GitHub metadata and root Vercel configuration remain at repository level.

## Path changes

Gradle, local.properties and evaluation manifests use code/ as their application root. The root Vercel configuration builds only the web allowlist into code/.web-dist/. Open code/ as the Android project; existing provider settings stay local.

Original runtime and asset subdirectories retain their relative arrangement inside code/.

Existing tracked Python bytecode and the Civic SQLite database, where applicable, are removed from the published source tree and retained locally under ignored code/ paths. Moving a database into an ignored directory does not erase earlier Git history. No credentials, local databases or generated caches are added.

## Verification scope

The verification scope is the file moves, static web bundle, JavaScript/Python syntax, asset paths and documentation links. Android compilation, device behavior, neural-model accuracy and the full optional benchmark are separate checks; no local Android SDK is available for this reorganization.

This change updates repository layout and configuration. It does not issue a deployment command or change external service settings. Existing GitHub-integrated hosts can still run their own configured automation when source is pushed.

Checked locally on 2 November 2026: The static web bundle built successfully. Custom JavaScript syntax checks passed, and nine referenced HTML/sample/model assets resolved. Documentation links resolved. Android compilation/device tests and the optional benchmark were not run locally because the Android SDK and optional model toolchain are unavailable.
