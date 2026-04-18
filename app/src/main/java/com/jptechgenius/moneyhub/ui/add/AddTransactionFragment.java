package com.jptechgenius.moneyhub.ui.add;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import com.jptechgenius.moneyhub.databinding.FragmentAddTransactionBinding;
import com.jptechgenius.moneyhub.ui.home.HomeViewModel;
import com.jptechgenius.moneyhub.util.DateUtils;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddTransactionFragment extends Fragment {

    private FragmentAddTransactionBinding binding;
    private HomeViewModel viewModel;
    private int transactionId = -1;
    private TransactionEntity existingTransaction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        if (getArguments() != null) {
            transactionId = getArguments().getInt("transaction_id", -1);
        }

        if (transactionId != -1) {
            loadTransactionData();
            binding.btnSave.setText("Update");
        }

        binding.btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void loadTransactionData() {
        viewModel.getTransactionById(transactionId).observe(getViewLifecycleOwner(), transaction -> {
            if (transaction != null) {
                existingTransaction = transaction;
                binding.etTitle.setText(transaction.getReason());
                binding.etAmount.setText(String.valueOf(transaction.getAmount()));
                if ("income".equalsIgnoreCase(transaction.getType())) {
                    binding.rbIncome.setChecked(true);
                } else {
                    binding.rbExpense.setChecked(true);
                }
            }
        });
    }

    private void saveTransaction() {
        String title = binding.etTitle.getText().toString();
        String amountStr = binding.etAmount.getText().toString();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        TransactionEntity transaction = (existingTransaction != null) ? existingTransaction : new TransactionEntity();
        transaction.setReason(title);
        transaction.setAmount(Double.parseDouble(amountStr));
        transaction.setType(binding.rbIncome.isChecked() ? "income" : "expense");
        if (transaction.getCategory() == null) {
            transaction.setCategory("General");
        }
        if (transaction.getDateMillis() == 0) {
            transaction.setDateMillis(DateUtils.getCurrentTimestamp());
        }
        
        viewModel.insert(transaction);
        Toast.makeText(requireContext(), transactionId == -1 ? "Transaction Saved" : "Transaction Updated", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
