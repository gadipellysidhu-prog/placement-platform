# Error Handling

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27

---

## Error Boundary

`src/shared/ui/error-boundary.tsx` — class component (required; no hook equivalent for `componentDidCatch`).

Three usage modes:

| Mode | Props | Renders |
|---|---|---|
| Global (default) | none | Full-page recovery UI with Reload + Try Again buttons |
| Feature / inline | `inline` | Compact bar with error message and Retry |
| Custom | `fallback` | Caller-provided React node |

```tsx
// Global — wraps entire app in App.tsx
<ErrorBoundary>
  <App />
</ErrorBoundary>

// Feature section — compact inline error
<ErrorBoundary inline>
  <DataTable />
</ErrorBoundary>

// Custom fallback
<ErrorBoundary fallback={<MyFallback />}>
  <Suspense>...</Suspense>
</ErrorBoundary>
```

The `onError` prop is the hook point for a Sentry or error-logging service integration (Phase 5 Observability).

---

## API Error Normalization

`src/lib/api/error.ts`

Every API call that can fail SHOULD use `normalizeApiError`:

```ts
import { normalizeApiError, getApiErrorMessage } from '@/lib/api'

try {
  await studentsApi.create(data)
} catch (err) {
  const { status, message } = normalizeApiError(err)
  // status: HTTP status or 0 for network errors
  // message: user-friendly string derived from RFC 7807 ProblemDetail
}

// Convenience shorthand for toast display:
toast.error(getApiErrorMessage(err))
```

`normalizeApiError` wraps any thrown value (AxiosError, network error with `status === 0`, plain Error, or unknown) into a typed `ApiError` that mirrors the backend RFC 7807 `ProblemDetail` format.

---

## HTTP 401 / Token Expiry

Handled transparently in `src/lib/axios.ts`. The response interceptor:

1. Detects 401
2. Silently refreshes the token via `POST /auth/refresh`
3. Replays the original request with the new token
4. On refresh failure: clears auth state and redirects to `/login`

**Feature code does NOT need to handle 401s.** The interceptor handles them transparently for all queued and in-flight requests.

---

## Network Errors

`normalizeApiError` maps network-level failures (AxiosError with no response, `status === 0`) to the message: *"Network error. Please check your connection."*

---

## Retry Strategy

| Context | Retry behaviour |
|---|---|
| TanStack Query (GET) | Retries up to 2 times; 4xx errors are never retried |
| TanStack Query (mutations) | No automatic retry |
| Manual async (`withRetry`) | Exponential backoff; retries on `status === 0` or `>= 500` only |

Use `withRetry` from `src/utils/async.ts` for non-Query async operations that should survive transient network blips.
