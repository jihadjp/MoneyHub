package com.jptechgenius.moneyhub.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jptechgenius.moneyhub.databinding.ItemCurrencyBinding;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

public class CurrencyAdapter extends RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder> {

    private List<Currency> currencies = new ArrayList<>();
    private OnCurrencyClickListener listener;

    public interface OnCurrencyClickListener {
        void onCurrencyClick(Currency currency);
    }

    public CurrencyAdapter(OnCurrencyClickListener listener) {
        this.listener = listener;
    }

    public void setCurrencies(List<Currency> currencies) {
        this.currencies = currencies;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CurrencyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCurrencyBinding binding = ItemCurrencyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new CurrencyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CurrencyViewHolder holder, int position) {
        Currency currency = currencies.get(position);
        holder.binding.tvCurrencyName.setText(currency.getDisplayName());
        holder.binding.tvCurrencyCode.setText(currency.getCurrencyCode());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCurrencyClick(currency);
            }
        });
    }

    @Override
    public int getItemCount() {
        return currencies.size();
    }

    static class CurrencyViewHolder extends RecyclerView.ViewHolder {
        ItemCurrencyBinding binding;
        CurrencyViewHolder(ItemCurrencyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
