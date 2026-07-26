# Tag preview (TagBrowse) locator map

Companion to `aiTask/rules/tags-hierarchy.md`, which owns the contracts and
invariants for both `TagsScreen` and `TagBrowseScreen`. This file is a
**file:line index** — use it to jump straight to the code. Do not restate the
contracts here; edit `tags-hierarchy.md` instead.

## Screens & VMs

- Screen composable: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseScreen.kt:63`
- Left-pane list builder (`BrowseList`): `app/src/main/java/com/example/filebox/ui/tags/TagBrowseScreen.kt:118`
- Child-tag row: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseScreen.kt:165`
- File row: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseScreen.kt:203`
- Right-pane preview: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseScreen.kt:253`
- No-preview fallback: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseScreen.kt:335`
- ViewModel: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseViewModel.kt:29`
- UI state model: `app/src/main/java/com/example/filebox/ui/tags/TagBrowseViewModel.kt:19`
- `state` combine (children + direct files + selectedFile projection):
  `app/src/main/java/com/example/filebox/ui/tags/TagBrowseViewModel.kt:44`

## Navigation wiring

- Route constant + helper: `app/src/main/java/com/example/filebox/ui/FileboxNavHost.kt:25`, `:31`
- Home chip → `tagBrowse`: `app/src/main/java/com/example/filebox/ui/FileboxNavHost.kt:44`
- `TagsScreen` → `tagBrowse`: `app/src/main/java/com/example/filebox/ui/FileboxNavHost.kt:91`
- `TAG_BROWSE` composable registration: `app/src/main/java/com/example/filebox/ui/FileboxNavHost.kt:94`

## Callers that hand off to TagBrowse

- Home tag chip: `app/src/main/java/com/example/filebox/ui/home/HomeScreen.kt:208`
  (chip `onClick` → `onOpenTag` → `Routes.tagBrowse(id)` in NavHost).
- Management overflow "Open browse": search `R.string.tags_open_browse` in
  `app/src/main/java/com/example/filebox/ui/tags/TagsScreen.kt`.

## Reused preview components (import; do not fork)

- Image: `app/src/main/java/com/example/filebox/ui/detail/preview/ImagePreview.kt`
- Video: `app/src/main/java/com/example/filebox/ui/detail/preview/VideoPreview.kt`
- Audio: `app/src/main/java/com/example/filebox/ui/detail/preview/AudioPreview.kt`
- Text (200 KB cap — see `category-mapping.md`):
  `app/src/main/java/com/example/filebox/ui/detail/preview/TextPreview.kt`
- External open (FileProvider): `app/src/main/java/com/example/filebox/ui/detail/ExternalOpen.kt`
- Preview type resolver: `app/src/main/java/com/example/filebox/domain/PreviewType.kt`

## Data sources

- Tags stream: `TagRepository.observeAll()` —
  `app/src/main/java/com/example/filebox/data/repo/TagRepository.kt:11`
- Files+tags stream: `FileRepository.observeAllWithTags()` —
  `app/src/main/java/com/example/filebox/data/repo/FileRepository.kt:19`
  (`TagBrowseViewModel` filters in memory for `tagId`).
- Direct-tag DAO (**not** currently used by TagBrowse):
  `ManagedFileDao.observeByTag(tagId)` in
  `app/src/main/java/com/example/filebox/data/db/ManagedFileDao.kt`.

## See also

- Contracts, invariants, strings, filtering semantics: `aiTask/rules/tags-hierarchy.md`
- Preview type / MIME rules: `aiTask/rules/category-mapping.md`
- FileProvider / external-open policy: `aiTask/rules/privacy-invariants.md`
