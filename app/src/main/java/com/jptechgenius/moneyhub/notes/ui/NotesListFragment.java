package com.jptechgenius.moneyhub.notes.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.jptechgenius.moneyhub.R;
import com.jptechgenius.moneyhub.notes.model.Label;
import com.jptechgenius.moneyhub.notes.model.Note;
import com.jptechgenius.moneyhub.notes.model.NoteStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesListFragment extends Fragment {

    private NotesViewModel viewModel;
    private NotesAdapter   adapter;

    // ── Views ─────────────────────────────────────────────────────────────
    private RecyclerView                 recyclerView;
    private EditText                     searchEditText;
    private ChipGroup                    filterChipGroup;
    private LinearLayout                 emptyStateLayout;
    private TextView                     emptyStateTitle;
    private ImageView                    emptyStateImage;
    private ExtendedFloatingActionButton fabAdd;

    // ── State ─────────────────────────────────────────────────────────────
    private long       activeLabelFilterId = -1L;
    private NoteStatus currentView         = NoteStatus.ACTIVE;

    // ── Multi-select ──────────────────────────────────────────────────────
    private final Set<Long> selectedIds = new HashSet<>();
    private ActionMode      actionMode  = null;

    // ── Activity result launchers ─────────────────────────────────────────

    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) viewModel.exportNotes(uri, ok ->
                            showSnackbar(Boolean.TRUE.equals(ok) ? "Backed up successfully" : "Backup failed"));
                }
            });

    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) viewModel.importNotes(uri, ok ->
                            showSnackbar(Boolean.TRUE.equals(ok) ? "Restored successfully" : "Restore failed"));
                }
            });

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
        bindViews(view);
        setupRecyclerView();
        setupSearch();
        setupFab();
        observeViewModel();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

    // ── View binding ──────────────────────────────────────────────────────

    private void bindViews(View root) {
        recyclerView     = root.findViewById(R.id.recyclerView);
        searchEditText   = root.findViewById(R.id.searchEditText);
        filterChipGroup  = root.findViewById(R.id.filterChipGroup);
        emptyStateLayout = root.findViewById(R.id.emptyStateLayout);
        emptyStateTitle  = root.findViewById(R.id.emptyStateTitle);
        emptyStateImage  = root.findViewById(R.id.emptyStateImage);
        fabAdd           = root.findViewById(R.id.fabAddButton);

        ImageView menuIcon = root.findViewById(R.id.overflowMenu);
        if (menuIcon != null) menuIcon.setOnClickListener(this::showPopupMenu);
    }

    // ── RecyclerView ──────────────────────────────────────────────────────

    private void setupRecyclerView() {
        adapter = new NotesAdapter(
                this::onNoteClicked,
                note -> { onNoteLongClicked(note); return true; }
        );
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    // ── Search ────────────────────────────────────────────────────────────

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ── FAB ───────────────────────────────────────────────────────────────

    private void setupFab() {
        fabAdd.setOnClickListener(v -> openNoteEditor(null));
    }

    // ── ViewModel observation ─────────────────────────────────────────────

    private void observeViewModel() {
        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            updateEmptyState(notes);
        });

        viewModel.getLabels().observe(getViewLifecycleOwner(), this::buildLabelFilterChips);

        viewModel.getCurrentView().observe(getViewLifecycleOwner(), status -> {
            if (status != null && status != currentView) {
                currentView = status;
            }
            updateEmptyState(adapter.getCurrentList());
        });
    }

    // ── Label filter chips ────────────────────────────────────────────────

    private void buildLabelFilterChips(List<Label> labels) {
        if (filterChipGroup == null) return;
        filterChipGroup.removeAllViews();
        if (labels == null || labels.isEmpty()) {
            filterChipGroup.setVisibility(View.GONE);
            return;
        }
        filterChipGroup.setVisibility(View.VISIBLE);

        Chip allChip = makeFilterChip("All", -1L);
        allChip.setChecked(activeLabelFilterId == -1L);
        filterChipGroup.addView(allChip);

        for (Label label : labels) {
            Chip chip = makeFilterChip(label.getName(), label.getId());
            chip.setChecked(activeLabelFilterId == label.getId());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> confirmDeleteLabel(label));
            filterChipGroup.addView(chip);
        }
    }

    private Chip makeFilterChip(String text, long labelId) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(false);
        chip.setOnClickListener(v -> {
            activeLabelFilterId = labelId;
            viewModel.setFilterLabel(labelId < 0 ? -1L : labelId);
            for (int i = 0; i < filterChipGroup.getChildCount(); i++) {
                ((Chip) filterChipGroup.getChildAt(i)).setChecked(
                        filterChipGroup.getChildAt(i) == chip);
            }
        });
        return chip;
    }

    private void confirmDeleteLabel(Label label) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete label?")
                .setMessage("\"" + label.getName() + "\" will be removed from all notes.")
                .setPositiveButton("Delete", (d, w) -> {
                    viewModel.deleteLabel(label);
                    if (activeLabelFilterId == label.getId()) {
                        activeLabelFilterId = -1L;
                        viewModel.setFilterLabel(-1L);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Empty state ───────────────────────────────────────────────────────

    private void updateEmptyState(List<Note> notes) {
        boolean empty = notes == null || notes.isEmpty();
        emptyStateLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (empty) {
            if (currentView == NoteStatus.TRASH) {
                emptyStateTitle.setText("Trash is empty");
                emptyStateImage.setImageResource(R.drawable.ic_empty_trash);
            } else if (currentView == NoteStatus.ARCHIVED) {
                emptyStateTitle.setText("Nothing archived yet");
                emptyStateImage.setImageResource(R.drawable.ic_empty_archive);
            } else {
                emptyStateTitle.setText("Your notes will appear here");
                emptyStateImage.setImageResource(R.drawable.ic_empty_notes);
            }
        }
    }

    // ── Note click ────────────────────────────────────────────────────────

    private void onNoteClicked(Note note, View sharedElement) {
        if (actionMode != null) {
            toggleSelection(note);
        } else {
            openNoteEditor(note);
        }
    }

    // ── Multi-select ──────────────────────────────────────────────────────

    private void onNoteLongClicked(Note note) {
        if (actionMode == null) startActionMode();
        toggleSelection(note);
    }

    private void toggleSelection(Note note) {
        if (selectedIds.contains(note.getId())) {
            selectedIds.remove(note.getId());
        } else {
            selectedIds.add(note.getId());
        }
        adapter.setSelectedIds(new HashSet<>(selectedIds));

        if (selectedIds.isEmpty()) {
            if (actionMode != null) actionMode.finish();
        } else if (actionMode != null) {
            actionMode.setTitle(selectedIds.size() + " selected");
        }
    }

    private void startActionMode() {
        actionMode = requireActivity().startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Context-appropriate actions based on current view
                if (currentView == NoteStatus.ACTIVE) {
                    menu.add(0, R.id.action_archive_selected, 0, "Archive")
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    menu.add(0, R.id.action_delete_selected, 1, "Delete")
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                } else if (currentView == NoteStatus.ARCHIVED) {
                    menu.add(0, R.id.action_restore_selected, 0, "Unarchive")
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    menu.add(0, R.id.action_delete_selected, 1, "Delete")
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                } else if (currentView == NoteStatus.TRASH) {
                    menu.add(0, R.id.action_restore_selected, 0, "Restore")
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    menu.add(0, R.id.action_delete_selected, 1, "Delete forever")
                            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                }
                menu.add(0, R.id.action_select_all, 2, "Select All")
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_delete_selected) {
                    confirmBulkDelete(); return true;
                } else if (id == R.id.action_archive_selected) {
                    performBulkArchive(); return true;
                } else if (id == R.id.action_restore_selected) {
                    performBulkRestore(); return true;
                } else if (id == R.id.action_select_all) {
                    selectAll(); return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                clearSelection();
            }
        });
    }

    private void selectAll() {
        List<Note> current = adapter.getCurrentList();
        if (current == null) return;
        for (Note n : current) selectedIds.add(n.getId());
        adapter.setSelectedIds(new HashSet<>(selectedIds));
        if (actionMode != null) actionMode.setTitle(selectedIds.size() + " selected");
    }

    private void clearSelection() {
        selectedIds.clear();
        adapter.setSelectedIds(new HashSet<>());
    }

    private void confirmBulkDelete() {
        int count = selectedIds.size();
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete " + count + " note" + (count > 1 ? "s" : "") + "?")
                .setMessage(currentView == NoteStatus.TRASH
                        ? "These notes will be permanently deleted. This cannot be undone."
                        : "These notes will be moved to Trash.")
                .setPositiveButton("Delete", (d, w) -> {
                    List<Note> all = adapter.getCurrentList();
                    if (all == null) return;

                    // Snapshot selected notes before deleting (needed for undo)
                    List<Note> toDelete = new ArrayList<>();
                    for (Note n : all) {
                        if (selectedIds.contains(n.getId())) toDelete.add(n);
                    }

                    // Perform the action
                    for (Note n : toDelete) {
                        if (currentView == NoteStatus.TRASH) viewModel.deleteNotePermanently(n);
                        else viewModel.trashNote(n);
                    }

                    if (actionMode != null) actionMode.finish();

                    // Permanent delete → no undo, just a message
                    if (currentView == NoteStatus.TRASH) {
                        showSnackbar(count + " note" + (count > 1 ? "s" : "") + " permanently deleted");
                        return;
                    }

                    // Trash → show Undo snackbar
                    String msg = count + " note" + (count > 1 ? "s" : "") + " moved to Trash";
                    if (getView() != null) {
                        requireActivity().runOnUiThread(() ->
                                Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG)
                                        .setAction("Undo", v -> {
                                            for (Note n : toDelete) viewModel.restoreNote(n);
                                            showSnackbar("Restored " + toDelete.size()
                                                    + " note" + (toDelete.size() > 1 ? "s" : ""));
                                        })
                                        .show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performBulkArchive() {
        List<Note> all = adapter.getCurrentList();
        if (all == null) return;

        List<Note> toArchive = new ArrayList<>();
        for (Note n : all) {
            if (selectedIds.contains(n.getId())) toArchive.add(n);
        }

        for (Note n : toArchive) viewModel.archiveNote(n);

        int count = toArchive.size();
        if (actionMode != null) actionMode.finish();

        String msg = count + " note" + (count > 1 ? "s" : "") + " archived";
        if (getView() != null) {
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), msg, Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> {
                                for (Note n : toArchive) viewModel.restoreNote(n);
                                showSnackbar("Restored " + toArchive.size()
                                        + " note" + (toArchive.size() > 1 ? "s" : ""));
                            })
                            .show());
        }
    }

    private void performBulkRestore() {
        List<Note> all = adapter.getCurrentList();
        if (all == null) return;

        List<Note> toRestore = new ArrayList<>();
        for (Note n : all) {
            if (selectedIds.contains(n.getId())) toRestore.add(n);
        }

        for (Note n : toRestore) viewModel.restoreNote(n);

        int count = toRestore.size();
        if (actionMode != null) actionMode.finish();

        showSnackbar(count + " note" + (count > 1 ? "s" : "") + " restored");
    }

    // ── Single note long-press options ────────────────────────────────────

    private void showNoteOptionsMenu(Note note) {
        String[] options;
        if (currentView == NoteStatus.TRASH) {
            options = new String[]{"Restore", "Delete permanently"};
        } else if (currentView == NoteStatus.ARCHIVED) {
            options = new String[]{"Unarchive", "Move to Trash"};
        } else {
            options = new String[]{"Archive", "Move to Trash"};
        }

        new AlertDialog.Builder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (currentView == NoteStatus.TRASH) {
                        if (which == 0) viewModel.restoreNote(note);
                        else confirmPermanentDelete(note);
                    } else if (currentView == NoteStatus.ARCHIVED) {
                        if (which == 0) viewModel.restoreNote(note);
                        else viewModel.trashNote(note);
                    } else {
                        if (which == 0) viewModel.archiveNote(note);
                        else {
                            viewModel.trashNote(note);
                            Snackbar.make(requireView(), "Moved to Trash", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", v -> viewModel.restoreNote(note)).show();
                        }
                    }
                }).show();
    }

    private void confirmPermanentDelete(Note note) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete permanently?")
                .setMessage("This note will be deleted forever.")
                .setPositiveButton("Delete", (d, w) -> viewModel.deleteNotePermanently(note))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Overflow popup menu ───────────────────────────────────────────────

    private void switchView(NoteStatus status) {
        if (currentView == status) return;
        currentView = status;
        viewModel.setCurrentView(status);
        clearSelection();
        fabAdd.setVisibility(status == NoteStatus.ACTIVE ? View.VISIBLE : View.GONE);
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.notes_list_menu, popup.getMenu());

        // Show checkmark on currently active view
        Menu m = popup.getMenu();
        m.findItem(R.id.action_active)
                .setTitle((currentView == NoteStatus.ACTIVE   ? "✓  " : "      ") + "Notes");
        m.findItem(R.id.action_archived)
                .setTitle((currentView == NoteStatus.ARCHIVED ? "✓  " : "      ") + "Archive");
        m.findItem(R.id.action_trash)
                .setTitle((currentView == NoteStatus.TRASH    ? "✓  " : "      ") + "Trash");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_active)           { switchView(NoteStatus.ACTIVE);   return true; }
            else if (id == R.id.action_archived)    { switchView(NoteStatus.ARCHIVED); return true; }
            else if (id == R.id.action_trash)       { switchView(NoteStatus.TRASH);    return true; }
            else if (id == R.id.action_export)      { launchExport();                  return true; }
            else if (id == R.id.action_import)      { launchImport();                  return true; }
            else if (id == R.id.action_empty_trash) { confirmEmptyTrash();             return true; }
            else if (id == R.id.action_manage_labels){ showManageLabelsDialog();       return true; }
            return false;
        });
        popup.show();
    }

    private void confirmEmptyTrash() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Empty Trash?")
                .setMessage("All trashed notes will be permanently deleted.")
                .setPositiveButton("Empty", (d, w) -> viewModel.emptyTrash())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchExport() {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, "notes_backup.json");
        exportLauncher.launch(i);
    }

    private void launchImport() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        importLauncher.launch(i);
    }

    // ── Label management ──────────────────────────────────────────────────

    private void showManageLabelsDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("New label name");
        input.setPadding(48, 24, 48, 8);
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Label")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) viewModel.addLabel(new Label(name, randomLabelColor()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String randomLabelColor() {
        String[] p = {"#E8F5E9","#E3F2FD","#FFF9C4","#FCE4EC","#F3E5F5","#E0F7FA","#FBE9E7","#F1F8E9"};
        return p[(int) (Math.random() * p.length)];
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private void openNoteEditor(@Nullable Note note) {
        NoteEditorFragment fragment = note != null
                ? NoteEditorFragment.newInstance(note)
                : NoteEditorFragment.newInstance();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,  android.R.anim.fade_out,
                        android.R.anim.fade_in,  android.R.anim.fade_out)
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private void showSnackbar(String msg) {
        if (getView() != null)
            requireActivity().runOnUiThread(() ->
                    Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT).show());
    }
}