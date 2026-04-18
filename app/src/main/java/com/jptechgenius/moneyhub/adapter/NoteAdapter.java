package com.jptechgenius.moneyhub.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jptechgenius.moneyhub.data.local.entity.NoteEntity;
import com.jptechgenius.moneyhub.databinding.ItemNoteBinding;
import com.jptechgenius.moneyhub.util.DateUtils;
import java.util.ArrayList;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<NoteEntity> notes = new ArrayList<>();

    public void setNotes(List<NoteEntity> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNoteBinding binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new NoteViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteEntity note = notes.get(position);
        holder.binding.tvTitle.setText(note.getTitle());
        holder.binding.tvContent.setText(note.getBody());
        holder.binding.tvDate.setText(DateUtils.formatDate(note.getCreatedAt()));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        ItemNoteBinding binding;
        NoteViewHolder(ItemNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
