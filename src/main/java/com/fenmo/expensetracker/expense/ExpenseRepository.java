package com.fenmo.expensetracker.expense;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class ExpenseRepository {
    private final String databaseUrl;

    public ExpenseRepository(@Value("${app.database.path:data/expenses.db}") String databasePath) {
        Path path = Path.of(databasePath);
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create data directory", exception);
        }
        this.databaseUrl = "jdbc:sqlite:" + path;
    }

    @PostConstruct
    void initialize() {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id TEXT PRIMARY KEY,
                    idempotency_key TEXT NOT NULL UNIQUE,
                    amount_paise INTEGER NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    date TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """);
            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_expenses_date_created
                ON expenses(date DESC, created_at DESC)
            """);
            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_expenses_category
                ON expenses(category)
            """);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to initialize database", exception);
        }
    }

    public CreateExpenseResponse createExpense(String idempotencyKey, CreateExpenseRequest request) {
        try (Connection connection = DriverManager.getConnection(databaseUrl)) {
            connection.setAutoCommit(false);
            Expense existing = findByIdempotencyKey(connection, idempotencyKey);
            if (existing != null) {
                connection.commit();
                return new CreateExpenseResponse(existing, false);
            }

            Expense expense = new Expense(
                UUID.randomUUID().toString(),
                MoneyParser.toPaise(request.amount()),
                request.category().trim(),
                request.description().trim(),
                request.date(),
                Instant.now().toString()
            );

            try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO expenses (id, idempotency_key, amount_paise, category, description, date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """)) {
                statement.setString(1, expense.id());
                statement.setString(2, idempotencyKey);
                statement.setLong(3, expense.amount_paise());
                statement.setString(4, expense.category());
                statement.setString(5, expense.description());
                statement.setString(6, expense.date());
                statement.setString(7, expense.created_at());
                statement.executeUpdate();
            } catch (SQLException exception) {
                if (isUniqueConstraintViolation(exception)) {
                    connection.rollback();
                    try (Connection retryConnection = DriverManager.getConnection(databaseUrl)) {
                        Expense retriedExpense = findByIdempotencyKey(retryConnection, idempotencyKey);
                        if (retriedExpense != null) {
                            return new CreateExpenseResponse(retriedExpense, false);
                        }
                    }
                }
                throw exception;
            }

            connection.commit();
            return new CreateExpenseResponse(expense, true);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to create expense", exception);
        }
    }

    public ExpenseListResponse listExpenses(String category, String sort) {
        StringBuilder sql = new StringBuilder("""
            SELECT id, amount_paise, category, description, date, created_at
            FROM expenses
        """);
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" WHERE category = ?");
            params.add(category);
        }

        if ("date_desc".equals(sort)) {
            sql.append(" ORDER BY date DESC, created_at DESC");
        } else {
            sql.append(" ORDER BY created_at DESC");
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int index = 0; index < params.size(); index++) {
                statement.setObject(index + 1, params.get(index));
            }

            List<Expense> expenses = new ArrayList<>();
            long totalPaise = 0;

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Expense expense = mapExpense(resultSet);
                    expenses.add(expense);
                    totalPaise += expense.amount_paise();
                }
            }

            List<String> categories = new ArrayList<>();
            try (PreparedStatement categoriesStatement = connection.prepareStatement("""
                SELECT DISTINCT category FROM expenses ORDER BY category ASC
            """);
                 ResultSet resultSet = categoriesStatement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(resultSet.getString("category"));
                }
            }

            return new ExpenseListResponse(expenses, totalPaise, categories);
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to list expenses", exception);
        }
    }

    public void deleteAll() {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM expenses");
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to clear expenses", exception);
        }
    }

    private Expense findByIdempotencyKey(Connection connection, String idempotencyKey) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT id, amount_paise, category, description, date, created_at
            FROM expenses
            WHERE idempotency_key = ?
        """)) {
            statement.setString(1, idempotencyKey);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return mapExpense(resultSet);
            }
        }
    }

    private Expense mapExpense(ResultSet resultSet) throws SQLException {
        return new Expense(
            resultSet.getString("id"),
            resultSet.getLong("amount_paise"),
            resultSet.getString("category"),
            resultSet.getString("description"),
            resultSet.getString("date"),
            resultSet.getString("created_at")
        );
    }

    private boolean isUniqueConstraintViolation(SQLException exception) {
        return exception instanceof SQLIntegrityConstraintViolationException
            || (exception.getMessage() != null && exception.getMessage().toLowerCase().contains("unique"));
    }
}
