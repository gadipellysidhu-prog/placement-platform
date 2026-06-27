# API Client

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27

---

## Directory Structure

```
src/lib/
├── axios.ts              — Axios instance, JWT interceptors, silent refresh
├── query-client.ts       — TanStack Query client (retry / stale / gc policy)
└── api/
    ├── keys.ts           — Centralized query key factory (all 10 modules)
    ├── error.ts          — API error normalization
    ├── auth.api.ts       — /auth/* endpoints
    ├── students.api.ts   — /api/students/*
    ├── companies.api.ts  — /api/companies/*
    ├── job-postings.api.ts — /api/job-postings/*
    ├── applications.api.ts — /api/applications/*
    ├── offers.api.ts     — /api/offers/*
    ├── certificates.api.ts — /api/certificates/*
    ├── branches.api.ts   — /api/branches/*
    ├── skills.api.ts     — /api/skills/*
    ├── dashboard.api.ts  — /api/dashboard/*
    ├── files.api.ts      — /api/files/*
    └── index.ts          — barrel export
```

---

## Axios Instance

`src/lib/axios.ts`

| Setting | Value |
|---|---|
| `baseURL` | `VITE_API_BASE_URL` (validated at startup by `src/config/env.ts`) |
| `timeout` | 30 000 ms |
| `Content-Type` | `application/json` |

### Request Interceptor

Reads the access token from `sessionStorage` (key `__access_token__`) and attaches it as:

```
Authorization: Bearer <token>
```

The interceptor reads sessionStorage directly instead of importing the Zustand auth store to prevent a circular dependency at module initialization time.

### Response Interceptor — JWT Silent Refresh

On HTTP 401 the interceptor silently refreshes the token using the following flow:

1. Read refresh token from `localStorage` (`__refresh_token__`).
2. If no refresh token → call `_handleAuthFailure()` immediately.
3. If `isRefreshing === true`, queue the original request config in `failedQueue` (resolves/rejects when refresh completes).
4. Call `POST /auth/refresh` with `{ refreshToken }`.
5. On success: update `__access_token__` in sessionStorage and `__refresh_token__` in localStorage (single-use rotation), lazy-import auth store to call `setAccessToken`, resolve all queued requests, replay the original request.
6. On failure: call `_handleAuthFailure()` → `clearAuth()` + `window.location.href = '/login'`.

The `isRefreshing` guard prevents concurrent refresh storms (the backend uses single-use token rotation — multiple concurrent refresh calls would invalidate each other).

**Feature code does NOT need to handle 401.** The interceptor handles it transparently.

---

## TanStack Query Client

`src/lib/query-client.ts`

| Option | Value | Rationale |
|---|---|---|
| `staleTime` | 5 min | Prevents refetching on navigation for stable data |
| `gcTime` | 10 min | Keeps inactive cache alive for quick back-navigation |
| `refetchOnWindowFocus` | `false` | Avoids surprise refetches during development |
| `retry` | never on 4xx | Auth/validation errors should not be retried |

---

## Query Key Factory

`src/lib/api/keys.ts` — typed tuple keys for all 10 API modules.

```ts
import { queryKeys } from '@/lib/api'

// Fetch a paginated list
const { data } = useQuery({
  queryKey: queryKeys.students.list({ page: 0, size: 20 }),
  queryFn: () => studentsApi.list({ page: 0, size: 20 }),
})

// Single record
queryKeys.students.detail('student-uuid')

// Invalidate all student queries after a mutation
queryClient.invalidateQueries({ queryKey: queryKeys.students.all() })
```

Keys follow the hierarchical pattern `[module, scope?, id?, params?]` so that `invalidateQueries` with a shorter prefix correctly invalidates all matching descendants.

---

## API Service Modules

`src/lib/api/` — 11 typed service modules, one per API domain. All return typed promises; callers receive the unwrapped response data.

| Module | File | Operations |
|---|---|---|
| Auth | `auth.api.ts` | Login, register, refresh, logout, verify email, forgot/reset password, `me` |
| Students | `students.api.ts` | CRUD, status, eligibility, skills association |
| Companies | `companies.api.ts` | CRUD, recruiter sub-resources |
| Job Postings | `job-postings.api.ts` | CRUD, student-facing list |
| Applications | `applications.api.ts` | Apply, status transitions, student/company views |
| Offers | `offers.api.ts` | Create, accept, reject, list |
| Certificates | `certificates.api.ts` | Generate, verify by number, list |
| Branches | `branches.api.ts` | CRUD |
| Skills | `skills.api.ts` | CRUD, search |
| Files | `files.api.ts` | Multipart upload with progress callback, blob download, delete |
| Dashboard | `dashboard.api.ts` | Admin/officer/student stats |

**Import via the barrel — never import individual API files directly:**

```ts
import { studentsApi, queryKeys, type StudentResponse } from '@/lib/api'
```

---

## Error Handling

`src/lib/api/error.ts`

```ts
import { normalizeApiError, getApiErrorMessage } from '@/lib/api'

// Full error object
try {
  await studentsApi.create(data)
} catch (err) {
  const { status, message, problem } = normalizeApiError(err)
}

// Direct toast usage
toast.error(getApiErrorMessage(err))
```

`normalizeApiError` converts any thrown value (AxiosError carrying an RFC 7807 `ProblemDetail`, network error with `status === 0`, or unknown) into a typed `ApiError`. `getApiErrorMessage` returns a user-friendly string keyed on status code.

---

## Dev Proxy

`vite.config.ts` proxies the following paths to `http://localhost:8081` in development:

- `/api/*` → backend REST endpoints
- `/auth/*` → backend auth endpoints

This eliminates CORS issues during local development. In production the backend URL is set via `VITE_API_BASE_URL`.

---

## Environment Validation

`src/config/env.ts` validates `import.meta.env` with Zod at startup. The app throws immediately if `VITE_API_BASE_URL` is missing or not a valid URL, surfacing misconfiguration before any network call is made.

```ts
export const env = {
  VITE_API_BASE_URL: string  // required, validated URL
  VITE_APP_NAME: string
  VITE_APP_VERSION: string
  VITE_ENABLE_DEVTOOLS: boolean
}
```

---

## Known Backend Gaps

Gaps identified against `docs/architecture/API_CONTRACT.md` that affect feature implementation:

| Gap | Impact |
|---|---|
| `GET /api/job-postings` returns OPEN postings only | Officers cannot list DRAFT/CLOSED postings via this endpoint |
| `GET /api/applications/my` returns unbounded list | No server-side pagination — front-end must handle large result sets |
| `GET /api/offers/my` returns unbounded list | Same |
| `GET /api/certificates/my` returns unbounded list | Same |
| No Recruiter API | Recruiter-association UI cannot be built until the backend exposes it |
| No Notification API | In-app notifications are not available |
