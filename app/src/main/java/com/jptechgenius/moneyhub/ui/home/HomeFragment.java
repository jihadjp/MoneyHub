package com.jptechgenius.moneyhub.ui.home;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.jptechgenius.moneyhub.R;
import com.jptechgenius.moneyhub.adapter.TransactionAdapter;
import com.jptechgenius.moneyhub.databinding.FragmentHomeBinding;
import dagger.hilt.android.AndroidEntryPoint;

/**
 * Dashboard screen — displays balance card and recent transactions.
 * Uses ViewBinding and observes LiveData from HomeViewModel.
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        setupRecyclerView();
        observeViewModel();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(transaction -> {
            // Navigate to detail/edit screen on item click
            Bundle args = new Bundle();
            args.putInt("transaction_id", transaction.getId());
            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.action_home_to_editTransaction, args);
        });

        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTransactions.setAdapter(adapter);
    }

    private void observeViewModel() {
        // Observe balance
        viewModel.getCurrentBalance().observe(getViewLifecycleOwner(), balance -> {
            if (balance != null) {
                binding.tvCurrentBalance.setText(
                        String.format("$ %.2f", balance)
                );
            }
        });

        // Observe total income
        viewModel.totalIncome.observe(getViewLifecycleOwner(), income -> {
            double val = (income != null) ? income : 0.0;
            binding.tvTotalIncome.setText(String.format("$ %.2f", val));
        });

        // Observe total expenses
        viewModel.totalExpenses.observe(getViewLifecycleOwner(), expense -> {
            double val = (expense != null) ? expense : 0.0;
            binding.tvTotalExpenses.setText(String.format("$ %.2f", val));
        });

        // Observe recent transactions list
        viewModel.recentTransactions.observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                adapter.submitList(transactions);
            }
        });
    }

    private void setupClickListeners() {
        // FAB -> navigate to Add Transaction
        binding.fabAdd.setOnClickListener(v -> {
            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.action_home_to_addTransaction);
        });

        // Chart icon -> navigate to Today's Chart
        binding.btnChart.setOnClickListener(v -> {
            NavController nav = Navigation.findNavController(requireView());
            nav.navigate(R.id.action_home_to_todayChart);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Prevent memory leaks
    }
}