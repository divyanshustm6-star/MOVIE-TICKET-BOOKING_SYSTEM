export function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatDate(value) {
  if (!value) {
    return 'Not scheduled';
  }
  return new Intl.DateTimeFormat('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value));
}

export function formatDateTime(value) {
  if (!value) {
    return 'Not scheduled';
  }
  return new Intl.DateTimeFormat('en-IN', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

export function compactStatus(value) {
  return String(value || '').replaceAll('_', ' ');
}

export function initials(value) {
  return String(value || 'MB').split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase();
}

export function uniqueValues(items, key) {
  return [...new Set(items.map((item) => item?.[key]).filter(Boolean))].sort();
}
