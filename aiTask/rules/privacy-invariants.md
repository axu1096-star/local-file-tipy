# Privacy invariants (non-negotiable)

These rules define Filebox's "fully private, local-only" contract. Every change
must uphold them. If a proposed change would break any invariant, stop and ask
the user before proceeding.

## Permissions
- `AndroidManifest.xml` must declare **no** permissions. In particular:
  - No `android.permission.INTERNET`
  - No `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`
  - No `READ_MEDIA_IMAGES` / `_VIDEO` / `_AUDIO`
  - No `POST_NOTIFICATIONS` unless the user explicitly asks for it
- Files enter the app only via the Storage Access Framework
  (`ActivityResultContracts.OpenMultipleDocuments`). Do not access user files
  via any other API.

## Storage
- Every imported file is **copied** into the app-private managed directory:
  `context.filesDir/managed/<yyyyMM>/<uuid>[.ext]`.
- The original `Uri` is stored in `ManagedFile.sourceUri` only as an audit
  reference. Do not call `takePersistableUriPermission`.
- Never write outside `filesDir` (no cache exfiltration, no MediaStore inserts).

## Backup and transfer
- `android:allowBackup="false"`
- `res/xml/data_extraction_rules.xml` and `res/xml/backup_rules.xml` must
  exclude every domain (`root`, `file`, `database`, `sharedpref`, `external`).
- Do not enable auto-backup or cross-device transfer.

## Dependencies
- Do not add any library that requires network permissions or that phones home
  (analytics, crash reporters, ad SDKs, remote config).
- Media playback stays offline: Media3 ExoPlayer only, backed by local `File`.
- Coil is used only against local `File` sources.

## File sharing outside the app
- Sharing is done via the app's `FileProvider`
  (authority `com.example.filebox.fileprovider`) with
  `FLAG_GRANT_READ_URI_PERMISSION`. Never share raw file paths.

## When in doubt
Prefer keeping data inside the app private directory and refuse the change if
it would leak file contents, paths, or metadata outside the device.
