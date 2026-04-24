package com.fenmo.expensetracker.expense;

import java.util.List;

public record ExpenseListResponse(
    List<Expense> expenses,
    long total_paise,
    List<String> categories
) {
}
