package com.fenmo.expensetracker.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateExpenseRequest(
    @NotBlank(message = "Amount is required")
    String amount,

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    String category,

    @NotBlank(message = "Description is required")
    @Size(max = 120, message = "Description must be at most 120 characters")
    String description,

    @NotBlank(message = "Date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in YYYY-MM-DD format")
    String date
) {
}
