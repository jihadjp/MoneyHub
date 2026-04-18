package com.jptechgenius.moneyhub.ui.today;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.jptechgenius.moneyhub.data.local.entity.TransactionEntity;
import com.jptechgenius.moneyhub.data.repository.TransactionRepository;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class TodayViewModel extends ViewModel {
    private final TransactionRepository repository;

    @Inject
    public TodayViewModel(TransactionRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<TransactionEntity>> getTodayTransactions() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long start = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        long end = calendar.getTimeInMillis();

        return repository.getTransactionsBetweenDates(start, end);
    }
}
