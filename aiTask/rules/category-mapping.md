# Category mapping (authoritative)

The six categories are **fixed**. Do not add or rename them without updating
`Category.kt`, all `values-*/strings.xml`, and the UI icon map in
`ui/common/CategoryUi.kt` together.

| Category   | MIME rules                                                                                                                  | Extension fallback (used when MIME is `application/octet-stream` or missing) |
| ---------- | --------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `IMAGE`    | `image/*`                                                                                                                   | jpg, jpeg, png, gif, bmp, webp, heic, heif, svg                              |
| `VIDEO`    | `video/*`                                                                                                                   | mp4, mkv, mov, avi, webm, 3gp, flv, wmv                                      |
| `AUDIO`    | `audio/*`                                                                                                                   | mp3, wav, flac, aac, ogg, m4a, opus, amr                                     |
| `DOCUMENT` | `text/*`, `application/pdf`, `application/rtf`, `application/msword`, MS Office `openxmlformats-*`, OpenDocument `oasis.opendocument.*` | pdf, txt, md, rtf, doc, docx, xls, xlsx, ppt, pptx, odt, ods, odp, csv, log, json, xml, html, htm |
| `ARCHIVE`  | `application/zip`, `application/x-tar`, `application/x-7z-compressed`, `application/x-rar-compressed`, `application/vnd.rar`, `application/gzip`, `application/x-bzip2` | zip, tar, gz, tgz, bz2, 7z, rar, xz                                          |
| `OTHER`    | anything else                                                                                                               | anything else                                                                |

## Preview mapping (`PreviewType`)
Inline preview is supported for these types only. Everything else falls back
to system `ACTION_VIEW` via FileProvider.

| PreviewType | Trigger                                                                            |
| ----------- | ---------------------------------------------------------------------------------- |
| `IMAGE`     | MIME starts with `image/`, or extension is an IMAGE extension                      |
| `VIDEO`     | MIME starts with `video/`, or extension is a VIDEO extension                       |
| `AUDIO`     | MIME starts with `audio/`, or extension is an AUDIO extension                      |
| `TEXT`      | `text/*`, `application/json`, `application/xml`, or a known code/text extension    |
| `NONE`      | everything else                                                                    |

## Invariants
- MIME check always runs first; extension is only a fallback.
- Extension comparison is lowercase and limited to 10 characters — longer
  suffixes are treated as "no extension" to avoid path-injection edge cases.
- Text preview is truncated to **200 KB** to avoid OOM on huge logs.
- When adding new extensions, keep the two tables (`Category.fromExtension` and
  the preview extension sets) consistent.
