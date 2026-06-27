/**
 * Formatting utilities.
 * All functions are pure — no side effects, no external dependencies.
 */

/** Format a number as Indian LPA currency (e.g. 12.5 → "₹12.5 LPA") */
export function formatCTC(value: number | null | undefined): string {
  if (value === null || value === undefined) return '—'
  return `₹${value.toLocaleString('en-IN')} LPA`
}

/** Format a CTC range */
export function formatCTCRange(
  min: number | null | undefined,
  max: number | null | undefined,
): string {
  if (!min && !max) return '—'
  if (!max) return formatCTC(min)
  if (!min) return formatCTC(max)
  return `${formatCTC(min)} – ${formatCTC(max)}`
}

/** Format an ISO-8601 Instant string to a readable local date + time. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}

/** Format an ISO-8601 date string (YYYY-MM-DD) to a readable date. */
export function formatDate(date: string | null | undefined): string {
  if (!date) return '—'
  return new Intl.DateTimeFormat('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(date))
}

/** Format bytes to KB/MB. */
export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** Generate initials from an email address (first two letters). */
export function emailInitials(email: string): string {
  const [local] = email.split('@')
  return (local.slice(0, 2) || 'U').toUpperCase()
}

/** Truncate a string to maxLen characters. */
export function truncate(str: string, maxLen: number): string {
  return str.length > maxLen ? `${str.slice(0, maxLen - 1)}…` : str
}

/** Capitalize the first letter. */
export function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase()
}

/** Convert snake_case or SCREAMING_SNAKE to Title Case. */
export function titleCase(str: string): string {
  return str
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}
