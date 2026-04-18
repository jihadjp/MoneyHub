package com.jptechgenius.moneyhub.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import com.jptechgenius.moneyhub.data.repository.TransactionRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ReportViewModel extends ViewModel {
    private final TransactionRepository repository;

    @Inject
    public ReportViewModel(TransactionRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<TransactionEntity>> getTransactionsBetweenDates(long start, long end) {
        return repository.getTransactionsBetweenDates(start, end);
    }
}
