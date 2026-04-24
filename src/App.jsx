import { useEffect, useMemo, useState } from 'react';
import {
  clearPendingSubmission,
  createExpense,
  createPendingSubmission,
  fetchExpenses,
  formatCurrencyFromPaise,
  getPendingSubmission
} from './api';

const CATEGORY_OPTIONS = [
  'Food',
  'Transport',
  'Shopping',
  'Bills',
  'Health',
  'Entertainment',
  'Travel',
  'Other'
];

const INITIAL_FORM = {
  amount: '',
  category: CATEGORY_OPTIONS[0],
  description: '',
  date: new Date().toISOString().slice(0, 10)
};

export default function App() {
  const [form, setForm] = useState(INITIAL_FORM);
  const [expenses, setExpenses] = useState([]);
  const [total, setTotal] = useState(0);
  const [availableCategories, setAvailableCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [sort] = useState('date_desc');
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function loadExpenseData() {
    setIsLoading(true);
    setLoadError('');

    try {
      const data = await fetchExpenses({
        category: selectedCategory,
        sort
      });

      setExpenses(data.expenses);
      setTotal(data.total_paise);
      setAvailableCategories(data.categories);
    } catch (error) {
      setLoadError(error.message);
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadExpenseData();
  }, [selectedCategory, sort]);

  useEffect(() => {
    const pending = getPendingSubmission();
    if (!pending) {
      return;
    }

    setIsSubmitting(true);
    setSubmitError('');
    setSuccessMessage('Restoring your last submission after a refresh...');

    createExpense(pending)
      .then(() => {
        setForm(INITIAL_FORM);
        setSuccessMessage('Your last expense was safely recorded.');
        return loadExpenseData();
      })
      .catch((error) => {
        setSubmitError(error.message);
        clearPendingSubmission(pending.requestId);
      })
      .finally(() => {
        setIsSubmitting(false);
      });
  }, []);

  const visibleCategoryOptions = useMemo(() => {
    return Array.from(new Set([...CATEGORY_OPTIONS, ...availableCategories]));
  }, [availableCategories]);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitError('');
    setSuccessMessage('');

    const pending = createPendingSubmission(form);
    setIsSubmitting(true);

    try {
      await createExpense(pending);
      setForm(INITIAL_FORM);
      setSuccessMessage('Expense saved successfully.');
      await loadExpenseData();
    } catch (error) {
      setSubmitError(error.message);
      clearPendingSubmission(pending.requestId);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="page-shell">
      <section className="hero">
        <div>
          <p className="eyebrow">Fenmo Assessment</p>
          <h1>Expense Tracker</h1>
          <p className="hero-copy">
            Record expenses, review recent activity, and stay safe through retries,
            slow networks, and page refreshes.
          </p>
        </div>

        <div className="summary-card">
          <span className="summary-label">Visible total</span>
          <strong>{formatCurrencyFromPaise(total)}</strong>
          <span className="summary-meta">
            {selectedCategory === 'all'
              ? 'Across all categories'
              : `Filtered for ${selectedCategory}`}
          </span>
        </div>
      </section>

      <section className="content-grid">
        <div className="panel">
          <div className="panel-header">
            <div>
              <h2>Add expense</h2>
              <p>Creates a durable expense entry with retry protection.</p>
            </div>
          </div>

          <form className="expense-form" onSubmit={handleSubmit}>
            <label>
              Amount (INR)
              <input
                name="amount"
                type="number"
                min="0.01"
                step="0.01"
                required
                value={form.amount}
                onChange={(event) =>
                  setForm((current) => ({ ...current, amount: event.target.value }))
                }
              />
            </label>

            <label>
              Category
              <select
                name="category"
                value={form.category}
                onChange={(event) =>
                  setForm((current) => ({ ...current, category: event.target.value }))
                }
              >
                {visibleCategoryOptions.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Description
              <input
                name="description"
                type="text"
                maxLength="120"
                required
                value={form.description}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    description: event.target.value
                  }))
                }
              />
            </label>

            <label>
              Date
              <input
                name="date"
                type="date"
                required
                value={form.date}
                onChange={(event) =>
                  setForm((current) => ({ ...current, date: event.target.value }))
                }
              />
            </label>

            <button className="primary-button" type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : 'Save expense'}
            </button>

            {submitError ? <p className="feedback error">{submitError}</p> : null}
            {successMessage ? <p className="feedback success">{successMessage}</p> : null}
          </form>
        </div>

        <div className="panel">
          <div className="panel-header panel-header-stack">
            <div>
              <h2>Expense history</h2>
              <p>Newest entries first, with category filtering.</p>
            </div>

            <label className="filter-control">
              Filter by category
              <select
                value={selectedCategory}
                onChange={(event) => setSelectedCategory(event.target.value)}
              >
                <option value="all">All categories</option>
                {visibleCategoryOptions.map((category) => (
                  <option key={category} value={category}>
                    {category}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {isLoading ? <p className="state-message">Loading expenses...</p> : null}
          {loadError ? <p className="feedback error">{loadError}</p> : null}
          {!isLoading && !loadError && expenses.length === 0 ? (
            <p className="state-message">No expenses yet. Add your first one to get started.</p>
          ) : null}

          {!isLoading && !loadError && expenses.length > 0 ? (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Description</th>
                    <th>Category</th>
                    <th className="amount-cell">Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((expense) => (
                    <tr key={expense.id}>
                      <td>{expense.date}</td>
                      <td>{expense.description}</td>
                      <td>
                        <span className="chip">{expense.category}</span>
                      </td>
                      <td className="amount-cell">
                        {formatCurrencyFromPaise(expense.amount_paise)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      </section>
    </main>
  );
}
