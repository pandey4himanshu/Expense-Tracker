const PENDING_SUBMISSION_KEY = 'expense-tracker.pending-submission';

export function formatCurrencyFromPaise(paise) {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2,
    minimumFractionDigits: 2
  }).format((paise ?? 0) / 100);
}

export async function fetchExpenses({ category, sort = 'date_desc' } = {}) {
  const params = new URLSearchParams();
  if (category && category !== 'all') {
    params.set('category', category);
  }
  if (sort) {
    params.set('sort', sort);
  }

  const suffix = params.toString() ? `?${params.toString()}` : '';
  const response = await fetch(`/api/expenses${suffix}`);

  if (!response.ok) {
    throw new Error('Unable to load expenses right now.');
  }

  return response.json();
}

export function createPendingSubmission(payload) {
  const requestId = crypto.randomUUID();
  const pending = { requestId, payload };
  localStorage.setItem(PENDING_SUBMISSION_KEY, JSON.stringify(pending));
  return pending;
}

export function getPendingSubmission() {
  const raw = localStorage.getItem(PENDING_SUBMISSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    localStorage.removeItem(PENDING_SUBMISSION_KEY);
    return null;
  }
}

export function clearPendingSubmission(requestId) {
  const current = getPendingSubmission();
  if (current && current.requestId === requestId) {
    localStorage.removeItem(PENDING_SUBMISSION_KEY);
  }
}

export async function createExpense(pendingSubmission) {
  const response = await fetch('/api/expenses', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': pendingSubmission.requestId
    },
    body: JSON.stringify(pendingSubmission.payload)
  });

  const body = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(body.error ?? 'Unable to save expense right now.');
  }

  clearPendingSubmission(pendingSubmission.requestId);
  return body;
}
