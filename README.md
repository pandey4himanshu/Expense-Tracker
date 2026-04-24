# Expense Tracker

A minimal full-stack expense tracker built for the Fenmo SDE assessment.

It focuses on correctness under realistic client behavior:
- repeated submits
- page refreshes after submit
- slow or flaky network responses

## Stack

- Frontend: React + Vite
- Backend: Java 21 + Spring Boot
- Persistence: SQLite

## Why SQLite

I chose SQLite because it gives durable local persistence with very little operational overhead, while still behaving more like a real datastore than an in-memory list or JSON file. It is a good fit for a small single-service application that may be extended later.

## Key Design Decisions

- Money is stored as `amount_paise` (`long`) instead of floating-point values to avoid precision issues.
- `POST /api/expenses` requires an `Idempotency-Key` header so retries do not create duplicate expenses.
- The frontend creates and stores a pending submission in `localStorage` before sending it. If the user refreshes after clicking submit, the app can safely retry the same request on reload.
- Expenses are persisted in SQLite with a unique constraint on `idempotency_key`.
- The backend supports filtering by category and sorting by date descending.

## API

### `POST /api/expenses`

Headers:
- `Idempotency-Key: <unique-request-id>`

Request body:

```json
{
  "amount": "125.50",
  "category": "Food",
  "description": "Dinner",
  "date": "2026-04-24"
}
```

### `GET /api/expenses`

Optional query params:
- `category=Food`
- `sort=date_desc`

## Local Development

Install frontend dependencies:

```bash
npm install
```

Run the frontend and backend together:

```bash
npm run dev
```

- Frontend dev server: `http://localhost:5173`
- Backend API: `http://localhost:3001`

## Production Build

Build the frontend and package the backend:

```bash
npm run build:full
```

Run the backend:

```bash
mvn spring-boot:run
```

The Spring Boot app serves the built frontend from `dist/`.

## Testing

Backend tests:

```bash
mvn test
```

Covered scenarios:
- same idempotency key does not create duplicate expenses
- filtering by category returns the correct rows and total

## Trade-offs Due To Timebox

- I prioritized correctness, data handling, and retry safety over adding many extra features.
- I kept authentication, editing/deleting expenses, and advanced analytics out of scope.
- I added focused backend tests rather than a larger end-to-end browser suite.
- Styling is intentionally polished but simple.

## Intentionally Not Done

- Authentication or multi-user support
- Expense editing and deletion
- Rich analytics dashboards
- Background sync or offline-first queueing beyond refresh-safe submit recovery

## Submission Notes

When deploying, build the frontend first so the generated `dist/` folder is available for the Java backend to serve.
