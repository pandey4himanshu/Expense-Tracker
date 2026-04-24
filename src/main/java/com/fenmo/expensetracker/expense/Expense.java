package com.fenmo.expensetracker.expense;

public record Expense(
    String id,
    long amount_paise,
    String category,
    String description,
    String date,
    String created_at
) {
}
