package com.jptechgenius.moneyhub.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jptechgenius.moneyhub.data.local.entity.DebtEntity;
import com.jptechgenius.moneyhub.databinding.ItemDebtBinding;
import java.util.ArrayList;
import java.util.List;

public class DebtAdapter extends RecyclerView.Adapter<DebtAdapter.DebtViewHolder> {

    private List<DebtEntity> debts = new ArrayList<>();

    public void setDebts(List<DebtEntity> debts) {
        this.debts = debts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDebtBinding binding = ItemDebtBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new DebtViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        DebtEntity debt = debts.get(position);
        holder.binding.tvPersonName.setText(debt.getPersonName());
        holder.binding.tvAmount.setText(String.valueOf(debt.getAmount()));
        holder.binding.tvType.setText(debt.getDebtType());
    }

    @Override
    public int getItemCount() {
        return debts.size();
    }

    static class DebtViewHolder extends RecyclerView.ViewHolder {
        ItemDebtBinding binding;
        DebtViewHolder(ItemDebtBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
