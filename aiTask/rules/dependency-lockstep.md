# Dependency lockstep

Several plugins/libraries in this repo are version-coupled. Bumping one without
the others will cause compile failures. Change them together or not at all.

## Kotlin toolchain group
Bump these four in the same commit:

| Component               | Current      | Why coupled                        |
| ----------------------- | ------------ | ---------------------------------- |
| Kotlin                  | `1.9.24`     | Base                               |
| KSP                     | `1.9.24-1.0.20` | Must match Kotlin exactly       |
| Compose compiler ext.   | `1.5.14`     | Pinned to Kotlin 1.9.24 by JetBrains lookup table |
| Compose BOM             | `2024.08.00` | Must contain a runtime compatible with the compiler ext. above |

Reference: https://developer.android.com/jetpack/androidx/releases/compose-kotlin

## Hilt
- `com.google.dagger:hilt-android` and `hilt-compiler` versions must be
  identical (`2.51.1`).
- Hilt uses KSP here (not kapt). Do not add `kotlin-kapt` — it will double-run
  annotation processing and slow the build.

## Room
- `androidx.room:room-runtime`, `room-ktx`, and `room-compiler` share one
  version (`2.6.1`). `room-compiler` goes on `ksp(...)`, not `annotationProcessor`.

## Media3
- All `androidx.media3:media3-*` artifacts share one version (`1.4.1`).

## Android Gradle Plugin
- AGP `8.5.2` requires JDK 17. The CI workflow pins Temurin 17. Do not bump AGP
  above 8.5.x without also verifying Kotlin/KSP compatibility.

## What NOT to change casually
- `jvmTarget = "1.8"` and `sourceCompatibility = 1.8` in `app/build.gradle.kts`.
  Bumping to 17 requires simultaneous changes to `compileOptions` and may break
  older device runtimes.
- `minSdk = 29`. Lowering re-introduces legacy storage code paths this project
  intentionally avoids.
