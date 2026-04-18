package com.jptechgenius.moneyhub.util;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class CurrencyHelper {
    public static String formatCurrency(double amount, String currencyCode) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        try {
            format.setCurrency(Currency.getInstance(currencyCode));
        } catch (Exception e) {
            format.setCurrency(Currency.getInstance(Locale.getDefault()));
        }
        return format.format(amount);
    }
}
