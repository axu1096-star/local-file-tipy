# Tag hierarchy & view mode

Filebox tags form a **self-referencing tree** (`Tag.parentId`) with per-parent
unique names. The tags screen renders either as a **tree** (indented, expand /
collapse) or a flat **list** — controlled by an in-VM `TagsViewMode`. Keep the
data model, VM projection, and UI toggle in sync; changing one alone will break
the others.

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

## UI projection

- `ui/tags/TagsViewModel.kt`
  - `TagsViewMode { TREE, LIST }` (in-memory only — no DataStore/prefs yet;
    resets to `TREE` on process death). If you add persistence, wire it here
    and update the privacy invariants doc if it needs a new storage location.
  - `TagNode(tag, depth, hasChildren, expanded)` is the flattened row model
    the screen consumes. Roots include orphaned tags whose `parentId` no
    longer exists (defensive against inconsistent state).
  - `state: StateFlow<TagsUiState>` = combine(tags, viewMode, expanded); do
    not add another source of truth for the visible list.
  - Actions: `setViewMode`, `toggleExpanded`, `expandAll`, `collapseAll`,
    `create(name, parentId)`, `rename`, `reparent`, `delete`.
  - `create(..., parentId)` auto-expands the parent so the new child is
    visible. `reparent(..., newParentId)` does the same for the new parent.

## Screen contract

- `ui/tags/TagsScreen.kt` signature: `TagsScreen(onBack, onOpenTag: (Long) -> Unit, viewModel)`.
  - `onOpenTag` is wired in `ui/FileboxNavHost.kt` to
    `Routes.libraryByTag(id)`; do not repurpose it for anything else.
  - TopBar actions (in this order): expand-all, collapse-all (tree mode only),
    view-mode toggle. FAB creates a **root** tag; child tags are created from
    the per-row overflow menu.
  - Per-row overflow (`MoreVert`) menu items must stay in this order:
    **Add child → Rename → Move → Delete**. UI tests / string keys assume it.
  - Tree indent is `depth * 20.dp`. When `showIndent = false` (list mode),
    the caret column is omitted entirely — do not just hide it, remove the
    space, or list rows will look mis-aligned.
- Move dialog (`MoveTagDialog`) must exclude the tag itself **and all its
  descendants** from candidate parents. The exclusion set is computed in the
  composable via BFS — mirror the repo-side cycle check there; both must
  agree, otherwise the UI will offer moves the repo silently rejects.

## Navigation

- `Routes.LIBRARY_BY_TAG` = `"libraryTag/{tagId}"`; helper `Routes.libraryByTag(id)`.
- Only `TagsScreen` currently navigates to this route. Do not add a second
  entry point without also deciding whether tag chips elsewhere (e.g., detail
  screen) should navigate too.
- `LibraryScreen` shows the tag name via `LibraryViewModel.tagName`, which
  observes `TagRepository.observeAll()` filtered by id. If you add a
  `findByIdFlow`, migrate this off the full-list scan.

## Filtering semantics (current limitation)

- `ManagedFileDao.observeByTag(tagId)` returns files **directly** tagged with
  `tagId` only. A parent-tag view does **not** currently include files of its
  descendants. If you introduce subtree filtering, do it as a new DAO method
  (e.g., `observeByTagSubtree`) backed by a recursive CTE, plus a new
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

## Invariants checklist (before merging tag-related changes)

1. `parent_id` foreign key still uses `SET_NULL`, never `CASCADE`.
2. Uniqueness index remains `(parent_id, name)`; do not restore the old global
   unique-on-name index.
3. Any code path that changes a tag's parent goes through
   `TagRepository.reparent` (cycle guard) — not `TagDao.updateParent` directly.
4. `TagsScreen(onOpenTag = ...)` is wired to `Routes.libraryByTag` in
   `FileboxNavHost`.
5. New string keys added to both `values/strings.xml` and
   `values-en/strings.xml`.
6. If DB schema changes further, bump `AppDatabase.version` and decide
   explicitly on migration vs. destructive fallback.
