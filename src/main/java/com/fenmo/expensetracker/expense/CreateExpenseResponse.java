package com.fenmo.expensetracker.expense;

public record CreateExpenseResponse(Expense expense, boolean created) {
}
