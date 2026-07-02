/** Sleep for `ms` milliseconds. */
export function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Retries `fn` up to `retries` times with exponential backoff.
 * Only retries on network-level errors (status 0 or 5xx).
 */
export async function withRetry<T>(
  fn: () => Promise<T>,
  retries = 2,
  baseDelayMs = 300,
): Promise<T> {
  let attempt = 0
  while (true) {
    try {
      return await fn()
    } catch (err) {
      attempt++
      const isRetryable =
        err !== null &&
        typeof err === 'object' &&
        'status' in err &&
        typeof (err as { status: number }).status === 'number' &&
        ((err as { status: number }).status === 0 || (err as { status: number }).status >= 500)

      if (!isRetryable || attempt > retries) throw err
      await sleep(baseDelayMs * Math.pow(2, attempt - 1))
    }
  }
}
