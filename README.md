# Filebox / 文件盒

A fully local, privacy-first file organizer for Android. Import files via SAF,
tag them freely, browse by 6 fixed categories, and preview images / videos /
audio / text — all without any network permission.

- Language: Kotlin + Jetpack Compose
- Storage: files copied to app-private `filesDir/managed/`, indexed by Room
- Privacy: no INTERNET, no external-storage permissions, `allowBackup=false`

See `AGENTS.md` for build/test commands and repo conventions.
