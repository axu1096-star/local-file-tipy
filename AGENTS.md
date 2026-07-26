# AGENTS.md

Filebox — a fully local, privacy-focused file organizer for Android (Jetpack
Compose). Single Gradle module `:app`.

## Identity mismatch (do not "fix" casually)
- Repo dir: `local-file-tipy`
- `settings.gradle.kts` `rootProject.name = "HelloApp"` (legacy)
- Package / applicationId: `com.example.filebox`
- App display name (default): 中文 `文件盒` (`values/strings.xml`); English fallback lives in `values-en/strings.xml`

Names disagree on record-keeping grounds only; treat Gradle/package names as
the source of truth for build output paths.

## Product invariants (must not regress)
- **No network permission, no telemetry.** See `aiTask/rules/privacy-invariants.md`.
- **No external storage permission.** Files enter only via SAF and are copied
  into `filesDir/managed/<yyyyMM>/<uuid>[.ext]`.
- **6 fixed categories + free tags.** See `aiTask/rules/category-mapping.md`.
- **Hierarchical tags (parent-child) + tree/list view mode.** See
  `aiTask/rules/tags-hierarchy.md`. `Tag.parentId` self-FK is `SET_NULL`
  (children promote to root), sibling names are unique per parent, cycle guard
  lives in `TagRepository.reparent`.
- **`allowBackup=false`** with empty backup/data-extraction rules.

## Toolchain (pinned, must match — see `aiTask/rules/dependency-lockstep.md`)
- JDK **17** (Temurin 17 in CI; AGP 8.5.2 requires it)
- Android Gradle Plugin **8.5.2**, Kotlin **1.9.24**, KSP **1.9.24-1.0.20**
- Compose BOM **2024.08.00**, Compose compiler ext **1.5.14** (locked to Kotlin 1.9.24)
- Hilt **2.51.1** (uses KSP, not kapt), Room **2.6.1**, Media3 **1.4.1**, Coil **2.6.0**
- `compileSdk` / `targetSdk` 34, `minSdk` **29** (Android 10+)
- `jvmTarget = "1.8"` in the module; do not raise without also changing
  `compileOptions`.

## Commands
Run from repo root; the wrapper is committed.

- Build (matches CI): `./gradlew assembleDebug --stacktrace`
- Release build: `./gradlew assembleRelease` (unsigned; `isMinifyEnabled = false`)
- Unit tests: `./gradlew testDebugUnitTest`
- Run a single test class: `./gradlew testDebugUnitTest --tests com.example.filebox.CategoryTest`
- Install on connected device: `./gradlew installDebug`
- Lint: `./gradlew :app:lint`
- Clean: `./gradlew clean`

If `gradlew` is not executable (fresh clone on Unix): `chmod +x gradlew`. CI
does this explicitly.

## Layout
- `app/src/main/java/com/example/filebox/`
  - `FileboxApp.kt` — `@HiltAndroidApp`
  - `MainActivity.kt` — single activity, hosts `FileboxNavHost`
  - `data/` — Room entities (`ManagedFile`, `Tag`, `FileTagCrossRef`), DAOs,
    `AppDatabase`, `FileRepository`, `TagRepository`
  - `domain/` — `Category`, `PreviewType`, `FileCopier` (pure JVM),
    `FileImporter` (SAF → copy → DB)
  - `di/DatabaseModule.kt` — Hilt module
  - `ui/home` / `ui/library` / `ui/detail` / `ui/detail/preview` / `ui/tags` /
    `ui/tools` / `ui/settings` / `ui/common` / `ui/theme`
- `app/src/main/res/`
  - `values/` + `values-en/` (default UI is Chinese via `values/strings.xml`;
    English fallback in `values-en/strings.xml`)
  - `xml/file_paths.xml` — FileProvider paths
  - `xml/data_extraction_rules.xml`, `xml/backup_rules.xml` — deny-all
- `app/src/test/java/com/example/filebox/` — JVM unit tests
  (`CategoryTest`, `FileCopierTest`), no Robolectric.
- `aiTask/rules/*.md` — repo-local AI context loaded via `opencode.json`
  `instructions`. Keep them in sync with the code.

## Framework quirks
- Hilt runs through **KSP** (`ksp("com.google.dagger:hilt-compiler:2.51.1")`).
  Do not add `kotlin-kapt`.
- Room compiler is on `ksp(...)`; schema export disabled.
- `FileProvider` authority is `com.example.filebox.fileprovider`. If package
  is ever renamed, update this and `AndroidManifest.xml` together.
- Non-transitive R classes (`android.nonTransitiveRClass=true`) — reference
  the module's own `R`, not a parent's.
- `FileCopier` is a pure JVM class (no Android imports) so it can be unit
  tested without Robolectric. `FileImporter` composes it with `ContentResolver`
  and the DAO — do not merge them.

## Testing
- Only JVM unit tests exist. Do not add Robolectric or `androidTest`
  without discussing — CI does not run instrumented tests.
- New MIME/extension additions must be covered in `CategoryTest`.

## CI
`.github/workflows/android.yml` runs on push/PR to `main`/`master`:
1. `./gradlew testDebugUnitTest --stacktrace` (fails the build on test failure)
2. Uploads `app/build/reports/tests/testDebugUnitTest`
3. `./gradlew assembleDebug --stacktrace`
4. Uploads `app/build/outputs/apk/debug/app-debug.apk`

Lint is not gated. Breaking lint will not fail CI; breaking build or tests will.

## Conventions
- Kotlin official style (`kotlin.code.style=official`).
- No formatter (ktlint/detekt/spotless) configured — do not add without asking.
- Default UI language is Chinese (`values/strings.xml`); English fallback in
  `values-en/strings.xml`.
- Strings must go through `stringResource(R.string.…)`; do not hardcode UI text.
