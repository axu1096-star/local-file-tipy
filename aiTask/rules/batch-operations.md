# Batch operations (multi-select)

`LibraryScreen` and `HomeScreen`'s **recent list** support a multi-select
"batch mode" for bulk **add tag**, **remove tag**, and **delete files**.
Keep the two screens' behavior in lockstep — they share the selection UX and
the same repository/dialog helpers.

## Entry & exit

- **Enter**: long-press any file row (`Modifier.combinedClickable`
  `onLongClick`) selects that row and turns on selection mode.
- **In selection mode**: a tap toggles the row (checkbox); it does **not**
  open the file/detail.
- **Exit**: the contextual top bar's close (X) icon, or deselecting the last
  row (selection auto-clears to normal mode when the set becomes empty).
- Selection state is **in-memory only** (per `ViewModel`), like
  `TagsViewMode`; it resets on process death. Do not persist it without
  updating `privacy-invariants.md`.

## Contextual top bar

When `selectionMode` is true, both screens swap their normal `TopAppBar` for
`SelectionTopBar` (declared **once** in
`ui/library/LibraryScreen.kt`, `internal`, reused by Home — do not fork it).
Home also hides its FAB while selecting.

Action order (must stay consistent): **close → select-all → save
(`Icons.Filled.SaveAlt`) → add-tag (`Icons.Filled.Label`) → remove-tag
(`Icons.Filled.LabelOff`) → delete (`Icons.Filled.Delete`)**. Title shows
`R.string.batch_selected` with the count. Save/tag/delete actions are disabled
when the selection is empty.

## Dialogs

- Add/remove tag opens the shared `BatchTagDialog`
  (`ui/common/BatchTagDialog.kt`) — a tag picker over `allTags`. `TagDialogMode`
  (`ADD` / `REMOVE`, `internal` in `LibraryScreen.kt`) selects the title and
  which VM action fires.
- Delete always shows an `AlertDialog` confirming the count
  (`R.string.batch_delete_confirm`, takes `%1$d`). Deleting removes the DB row
  **and** the stored copy in `filesDir` (via `FileRepository.deleteAll` →
  `delete`).

## Data model (source of truth)

- `data/repo/FileRepository.kt`
  - `addTagToFiles(fileIds, tagId)` — loops `dao.addTagCrossRef` (IGNORE on
    conflict, so re-adding an existing tag is a no-op).
  - `removeTagFromFiles(fileIds, tagId)` — loops `dao.removeTagCrossRef`.
  - `deleteAll(files)` — loops the existing single-file `delete`, so each file's
    stored copy is deleted too.
  - `exportTo(files, treeUri, onProgress)` — batch export via SAF
    `OpenDocumentTree` to a user-chosen folder; uses `FileExporter` under the
    hood. See `aiTask/rules/privacy-invariants.md` for the privacy exception.
  - No new DAO methods or schema changes were needed; `AppDatabase.version`
    is unchanged.
- ViewModels (`LibraryViewModel`, `HomeViewModel`) each hold:
  - `selectionMode: StateFlow<Boolean>`, `selectedIds: StateFlow<Set<Long>>`,
    `allTags: StateFlow<List<Tag>>` (sorted by name).
  - Actions: `enterSelection(id)`, `toggleSelection(id)`, `selectAll()`,
    `clearSelection()`, `addTagToSelected(tagId)`, `removeTagFromSelected(tagId)`,
    `deleteSelected()`, `saveSelectedTo(treeUri)`. Each mutating action clears
    the selection when done.
  - `selectAll()` selects the currently visible list (Library: filtered
    `files`; Home: `recent`).
  - `export: StateFlow<ExportUiState>` (`ui/common/ExportUiState.kt`) drives the
    real-time progress dialog (`ui/common/ExportProgressDialog.kt`);
    `consumeExportResult()` clears the one-shot result after the Toast shows.
    `saveSelectedTo(treeUri)` runs `FileRepository.exportTo` with a progress
    callback that updates `export`.

## Export (save to device)

- Entry points: the `SaveAlt` action in `SelectionTopBar` (both screens) and the
  `SaveAlt` action in `FileDetailScreen`'s top bar (single file →
  `FileDetailViewModel.exportTo`).
- Uses SAF `ActivityResultContracts.OpenDocumentTree()` — user picks a
  destination folder; no permissions, no network. This is the **only** path
  that writes outside `filesDir`; see `aiTask/rules/privacy-invariants.md`.
- `domain/FileExporter.kt` copies each file's stored copy to the chosen tree via
  `DocumentFile.createFile(mime, displayName)` + `openOutputStream`. Target names
  use the original `displayName`; on collision it appends ` (n)` before the
  extension (pure-JVM `FileExporter.uniqueName`, unit-tested).
- Result is reported via `Toast` (`batch_save_done` / `batch_save_partial`).

## Strings

Added under the `batch_*` prefix in **both** `values/strings.xml` and
`values-en/strings.xml`:

- `batch_selected` (takes `%1$d`)
- `batch_add_tag`, `batch_remove_tag`, `batch_select_all`, `batch_exit`
- `batch_delete_confirm` (takes `%1$d`)
- `batch_pick_tag_add`, `batch_pick_tag_remove`, `batch_no_tags`
- `batch_save`, `batch_save_progress` (takes `%1$d`, `%2$d`),
  `batch_save_done` (takes `%1$d`), `batch_save_partial` (takes `%1$d`, `%2$d`)
- `detail_save` (single-file save in the detail screen)

## Invariants checklist

1. Both `LibraryScreen` and `HomeScreen` recent list expose the same batch
   actions via the shared `SelectionTopBar`.
2. Batch delete goes through `FileRepository.deleteAll`/`delete` so stored
   copies inside `filesDir` are removed — never leave orphaned files.
3. Batch tag add/remove uses `addTagToFiles`/`removeTagFromFiles`; do not call
   `replaceTags` (which would clobber a file's other tags).
4. Selection state stays in-memory in the VM.
5. New string keys land in both locales in the same commit.
6. Batch export goes through `FileRepository.exportTo` → `FileExporter`, only
   to a user-chosen SAF tree; never write to a hardcoded/external path.
