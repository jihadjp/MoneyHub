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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Using HomeViewModel for simplicity, or could have a dedicated AddTransactionViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        binding.btnSave.setOnClickListener(v -> saveTransaction());
    }

    private void saveTransaction() {
        String title = binding.etTitle.getText().toString();
        String amountStr = binding.etAmount.getText().toString();

        if (title.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        TransactionEntity transaction = new TransactionEntity();
        transaction.setReason(title);
        transaction.setAmount(Double.parseDouble(amountStr));
        transaction.setType(binding.rbIncome.isChecked() ? "INCOME" : "EXPENSE");
        transaction.setDateMillis(DateUtils.getCurrentTimestamp());
        
        viewModel.insert(transaction);
        Toast.makeText(requireContext(), "Transaction Saved", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
