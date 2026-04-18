package com.jptechgenius.moneyhub.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.jptechgenius.moneyhub.R;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import com.jptechgenius.moneyhub.databinding.ItemTransactionBinding;
import com.jptechgenius.moneyhub.util.DateUtils;
import java.util.Locale;

/**
 * RecyclerView adapter for transaction items on the Home screen.
 * Uses ListAdapter with DiffUtil for efficient list updates.
 */
public class TransactionAdapter
        extends ListAdapter<TransactionEntity, TransactionAdapter.ViewHolder> {

    // Colors (hex strings — move to resources in production)
    private static final String COLOR_INCOME   = "#2E7D32"; // Green
    private static final String COLOR_EXPENSE  = "#C62828"; // Red
    private static final String COLOR_TRANSFER = "#1565C0"; // Blue

    public interface OnItemClickListener {
        void onItemClick(TransactionEntity transaction);
    }

    private final OnItemClickListener listener;

    public TransactionAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<TransactionEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TransactionEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull TransactionEntity o,
                                               @NonNull TransactionEntity n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TransactionEntity o,
                                                  @NonNull TransactionEntity n) {
                    return o.getAmount() == n.getAmount()
                            && o.getCategory().equals(n.getCategory())
                            && o.getDateMillis() == n.getDateMillis();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTransactionBinding b = ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTransactionBinding b;

        ViewHolder(ItemTransactionBinding binding) {
            super(binding.getRoot());
            b = binding;
        }

        void bind(TransactionEntity t) {
            // Determine color based on type
            String colorHex;
            int iconRes;
            switch (t.getType().toLowerCase(Locale.ROOT)) {
                case "expense":
                    colorHex = COLOR_EXPENSE;
                    iconRes  = R.drawable.ic_arrow_down_circle;
                    break;
                case "transfer":
                    colorHex = COLOR_TRANSFER;
                    iconRes  = R.drawable.ic_transfer;
                    break;
                default: // income
                    colorHex = COLOR_INCOME;
                    iconRes  = R.drawable.ic_arrow_up_circle;
                    break;
            }

            int color = Color.parseColor(colorHex);

            b.tvType.setText(t.getType());
            b.tvType.setTextColor(color);
            b.tvCategory.setText(t.getCategory());
            b.tvReason.setText(t.getReason() != null ? t.getReason() : "");
            b.tvDatetime.setText(DateUtils.formatDateTime(t.getDateMillis()));
            b.tvAmount.setText(String.format(Locale.US, "$ %.2f", t.getAmount()));
            b.tvAmount.setTextColor(color);
            b.ivTypeIcon.setImageResource(iconRes);
            b.ivTypeIcon.setImageTintList(ColorStateList.valueOf(color));

            // Item click
            itemView.setOnClickListener(v -> listener.onItemClick(t));

            // 3-dot context menu
            b.btnMenu.setOnClickListener(v -> {
                android.widget.PopupMenu popup =
                        new android.widget.PopupMenu(itemView.getContext(), b.btnMenu);
                popup.inflate(R.menu.menu_transaction_item);
                popup.setOnMenuItemClickListener(item -> {
                    // Handle edit / delete from menu
                    return true;
                });
                popup.show();
            });
        }
    }
}