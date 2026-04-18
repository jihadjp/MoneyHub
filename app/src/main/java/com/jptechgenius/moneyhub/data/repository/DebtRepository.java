package com.jptechgenius.moneyhub.data.repository;

import androidx.lifecycle.LiveData;
import com.jptechgenius.moneyhub.data.local.dao.DebtDao;
import com.jptechgenius.moneyhub.data.local.entity.DebtEntity;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DebtRepository {
    private final DebtDao debtDao;

    @Inject
    public DebtRepository(DebtDao debtDao) {
        this.debtDao = debtDao;
    }

    public void insert(DebtEntity debt) {
        new Thread(() -> debtDao.insert(debt)).start();
    }

    public void update(DebtEntity debt) {
        new Thread(() -> debtDao.update(debt)).start();
    }

    public void delete(DebtEntity debt) {
        new Thread(() -> debtDao.delete(debt)).start();
    }

    public LiveData<List<DebtEntity>> getAllDebts() {
        return debtDao.getAllDebts();
    }

    public LiveData<List<DebtEntity>> getDebtsByType(String type) {
        return debtDao.getDebtsByType(type);
    }
}
