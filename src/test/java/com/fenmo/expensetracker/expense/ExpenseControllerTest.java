package com.fenmo.expensetracker.expense;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExpenseControllerTest {
    @TempDir
    static Path tempDirectory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository repository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("app.database.path", () -> tempDirectory.resolve("test-expenses.db").toString());
    }

    @BeforeEach
    void resetData() {
        repository.deleteAll();
    }

    @Test
    void createsExpenseOnlyOnceForRetriedIdempotencyKey() throws Exception {
        String payload = """
            {
              "amount": "125.50",
              "category": "Food",
              "description": "Dinner",
              "date": "2026-04-24"
            }
            """;

        mockMvc.perform(post("/api/expenses")
                .header("Idempotency-Key", "retry-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.created").value(true));

        mockMvc.perform(post("/api/expenses")
                .header("Idempotency-Key", "retry-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(false));

        mockMvc.perform(get("/api/expenses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenses", hasSize(1)))
            .andExpect(jsonPath("$.total_paise").value(12550));
    }

    @Test
    void filtersExpensesByCategory() throws Exception {
        mockMvc.perform(post("/api/expenses")
                .header("Idempotency-Key", "food-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": "80.00",
                      "category": "Food",
                      "description": "Lunch",
                      "date": "2026-04-24"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/expenses")
                .header("Idempotency-Key", "travel-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": "200.00",
                      "category": "Travel",
                      "description": "Cab",
                      "date": "2026-04-23"
                    }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/expenses").param("category", "Food"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.expenses", hasSize(1)))
            .andExpect(jsonPath("$.expenses[0].category").value("Food"))
            .andExpect(jsonPath("$.total_paise").value(8000));
    }
}
