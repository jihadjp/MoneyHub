package com.jptechgenius.moneyhub.ui.debitcredit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.jptechgenius.moneyhub.data.local.entity.DebtEntity;
import com.jptechgenius.moneyhub.data.repository.DebtRepository;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DebitCreditViewModel extends ViewModel {
    private final DebtRepository repository;
    private final LiveData<List<DebtEntity>> allDebts;

    @Inject
    public DebitCreditViewModel(DebtRepository repository) {
        this.repository = repository;
        this.allDebts = repository.getAllDebts();
    }

    public LiveData<List<DebtEntity>> getAllDebts() {
        return allDebts;
    }

    public void insert(DebtEntity debt) {
        repository.insert(debt);
    }
}
