# Tag hierarchy & view mode

Filebox tags form a **self-referencing tree** (`Tag.parentId`) with per-parent
unique names. Tag management renders as either a **tree** (nested, expand /
collapse) or a flat **list** — controlled by an in-VM `TagsViewMode`. A
separate **tag browse** screen shows a single tag's direct children + directly
attached files with an inline preview pane. Keep the data model, VM
projection, and UI toggle in sync; changing one alone will break the others.

## Data model (source of truth)

- `data/entity/Tag.kt`
  - `parentId: Long?` with `@ColumnInfo("parent_id")` and self-referencing
    `ForeignKey(onDelete = SET_NULL)` — deleting a parent **promotes its
    children to root**, it does not cascade.
  - Uniqueness index is `("parent_id", "name")`, i.e. **unique per parent**,
    not globally. Two sibling tags cannot share a name; a root and a nested
    tag can.
  - `@Index("parent_id")` for tree traversal queries.
- `data/db/AppDatabase.kt` — `version = 2`. `DatabaseModule` uses
  `fallbackToDestructiveMigration()`; a v1→v2 SQL migration has **not** been
  written. Any bump beyond v2 must decide explicitly whether to keep the
  destructive fallback.
- `data/db/TagDao.kt` — hierarchy-aware helpers:
  - `findByNameInParent(name, parentId)` (nullable `parentId`)
  - `updateParent(id, parentId)`
- `data/repo/TagRepository.kt`
  - `createOrGet(name, parentId = null, colorArgb = null)`
  - `reparent(id, newParentId)` — returns `false` and does nothing if the move
    would create a cycle (target is the tag itself or one of its descendants).
    All UI paths that move a tag **must** call `reparent`, never `updateParent`
    directly, or the cycle guard is bypassed.

## Management screen (`TagsScreen`, `Routes.TAGS`)

`TagsScreen` is **tag-only** — it does not surface files or previews. Its job
is CRUD + hierarchy editing.

- `ui/tags/TagsViewModel.kt`
  - `TagsViewMode { TREE, LIST }` (in-memory only — no DataStore/prefs yet;
    resets to `TREE` on process death). If you add persistence, wire it here
    and update the privacy invariants doc if it needs a new storage location.
  - `TagRow(tag, depth, hasChildren, fileCount, expanded)` is the sole row
    type. `fileCount` is the number of files **directly** tagged with this
    tag (computed from `FileRepository.observeAllWithTags()`); it is a
    read-only badge — the management screen never renders file rows.
    `hasChildren` reflects **child tags only**.
  - `state: StateFlow<TagsUiState>` = combine(tags, filesWithTags, viewMode,
    expanded); do not add another source of truth for the visible list.
  - Roots include orphaned tags whose `parentId` no longer exists (defensive
    against inconsistent state).
  - Actions: `setViewMode`, `toggleExpanded`, `expandAll`, `collapseAll`,
    `create(name, parentId)`, `rename`, `reparent`, `delete`.
  - `create(..., parentId)` auto-expands the parent so the new child is
    visible. `reparent(..., newParentId)` does the same for the new parent.
- `ui/tags/TagsScreen.kt` signature: `TagsScreen(onBack, onOpenBrowse: (Long) -> Unit, viewModel)`.
  - `onOpenBrowse` is wired in `ui/FileboxNavHost.kt` to `Routes.tagBrowse(id)`.
  - TopBar actions (in order): expand-all, collapse-all (tree mode only),
    view-mode toggle. FAB creates a **root** tag; child tags are created from
    the per-row inline `+` button (tree mode) or the overflow menu.
  - **Row click** in TREE mode toggles expand/collapse (leaves without
    children fall through to `onOpenBrowse`). Row click in LIST mode opens
    the browse screen. Explicit navigation to the browse screen is always
    available via the overflow menu.
  - Per-row overflow (`MoreVert`) menu items must stay in this order:
    **Open browse → Add child → Rename → Move → Delete**. UI/UX assumptions
    and any future UI tests depend on it.
  - Tree indent is `depth * 16.dp`.
- Move dialog (`MoveTagDialog`) must exclude the tag itself **and all its
  descendants** from candidate parents. The exclusion set is computed in the
  composable via BFS — mirror the repo-side cycle check there; both must
  agree, otherwise the UI will offer moves the repo silently rejects.

## Tag browse screen (`TagBrowseScreen`, `Routes.TAG_BROWSE`)

`TagBrowseScreen` is the **single-tag browsing view** — it lists the tag's
**first-level child tags** and **directly attached files only** (no
recursion), plus an inline preview pane on the right.

- `ui/tags/TagBrowseViewModel.kt`
  - Reads `tagId` from `SavedStateHandle`.
  - State: `{ tagId, currentTag, parentTag, childTags, files, selectedFile }`.
  - `childTags` = tags whose `parentId == tagId`, sorted by name.
  - `files` = files directly tagged with `tagId` (from
    `FileRepository.observeAllWithTags()`), sorted by `addedAt` desc. It does
    **not** include files of descendant tags — see "Filtering semantics" below.
  - `selectFile(id?)`, `resolveFile(file)` (delegates to `FileRepository`).
- `ui/tags/TagBrowseScreen.kt` signature: `TagBrowseScreen(onBack,
  onOpenChildTag: (Long) -> Unit, onOpenFile: (Long) -> Unit, viewModel)`.
  - `onOpenChildTag` is wired to `Routes.tagBrowse(id)` — clicking a child
    tag drills into the next level. Do not repurpose it for anything else.
  - `onOpenFile` is wired to `Routes.detail(id)`.
  - Layout: **two-pane split**. Left pane is a `LazyColumn` with child-tag
    rows first, then a divider, then file rows. Right pane is the preview,
    bound to `state.selectedFile`. Clicking a file row selects it (preview);
    the row's open icon and the preview header's open icon both navigate to
    `Routes.detail(id)`.
  - Empty state when neither child tags nor files exist:
    `stringResource(R.string.tags_browse_empty)`.

## Navigation

- `Routes.TAG_BROWSE` = `"tagBrowse/{tagId}"`; helper `Routes.tagBrowse(id)`.
  Entry points:
  - Home screen tag chip (`HomeScreen`'s `onOpenTag`) — this is the primary
    entry point for browsing files under a tag.
  - `TagsScreen` overflow "Open browse" menu item, or row click.
  - `TagBrowseScreen` itself when the user drills into a child tag.
- `Routes.LIBRARY_BY_TAG` = `"libraryTag/{tagId}"`; helper
  `Routes.libraryByTag(id)`. **Currently no UI surface navigates here** — it
  is intentionally retained for future entry points (e.g., tag chips in the
  detail screen or a "view in library" affordance in `TagBrowseScreen`). Do
  not remove the route or the `LibraryFilter.OfTag` branch without a
  replacement plan.
- `LibraryScreen` shows the tag name via `LibraryViewModel.tagName`, which
  observes `TagRepository.observeAll()` filtered by id. If you add a
  `findByIdFlow`, migrate this off the full-list scan.

## Filtering semantics (current limitation)

- `ManagedFileDao.observeByTag(tagId)` returns files **directly** tagged with
  `tagId` only. A parent-tag view does **not** currently include files of its
  descendants. The same holds for `TagBrowseViewModel.files`. If you
  introduce subtree filtering, do it as a new DAO method (e.g.,
  `observeByTagSubtree`) backed by a recursive CTE, plus a new
  `LibraryFilter` variant — do not silently change `observeByTag`'s meaning,
  it's referenced from `FileRepository` and `LibraryViewModel`.

## Strings

All tag UI text lives under the `tags_*` prefix in both `values/strings.xml`
and `values-en/strings.xml`. Keep both locales in sync in the same commit:

- `tags_title`, `tags_new`, `tags_name`, `tags_empty`
- `tags_rename`, `tags_add_child`, `tags_new_child_of` (takes parent name)
- `tags_move`, `tags_move_title` (takes tag name), `tags_no_parent`
- `tags_delete_confirm` (takes tag name)
- `tags_view_tree`, `tags_view_list`, `tags_expand_all`, `tags_collapse_all`
- `tags_open_browse` (management overflow menu → tag browse)
- `tags_browse_title` (fallback title when the current tag is unknown)
- `tags_browse_empty`
- `tags_preview_hint` (right-pane placeholder in the browse screen)

## Invariants checklist (before merging tag-related changes)

1. `parent_id` foreign key still uses `SET_NULL`, never `CASCADE`.
2. Uniqueness index remains `(parent_id, name)`; do not restore the old global
   unique-on-name index.
3. Any code path that changes a tag's parent goes through
   `TagRepository.reparent` (cycle guard) — not `TagDao.updateParent` directly.
4. `HomeScreen`'s tag chip and `TagsScreen`'s "Open browse" both navigate to
   `Routes.tagBrowse`. `Routes.LIBRARY_BY_TAG` remains registered but has no
   UI entry point.
5. `TagsScreen` renders tags only (no file rows, no preview pane).
   `TagBrowseScreen` renders one tag's first-level children + directly
   attached files only (no recursion).
6. New string keys added to both `values/strings.xml` and
   `values-en/strings.xml`.
7. If DB schema changes further, bump `AppDatabase.version` and decide
   explicitly on migration vs. destructive fallback.
