package com.fenmo.expensetracker.expense;

import java.util.regex.Pattern;

public final class MoneyParser {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    private MoneyParser() {
    }

    public static long toPaise(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        String normalized = value.trim();
        if (!AMOUNT_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Amount must be a valid positive number with up to two decimals");
        }

        String[] parts = normalized.split("\\.");
        long whole = Long.parseLong(parts[0]);
        String fraction = parts.length > 1 ? parts[1] : "";
        String paddedFraction = (fraction + "00").substring(0, 2);
        long paise = (whole * 100) + Long.parseLong(paddedFraction);

        if (paise <= 0) {
          throw new IllegalArgumentException("Amount must be greater than zero");
        }

        return paise;
    }
}
