# Authentication Flow

**Phase:** 3 — Authentication & Authorization  
**Last updated:** 2026-06-27  
**Related:** [SESSION_MANAGEMENT.md](SESSION_MANAGEMENT.md) · [PERMISSION_SYSTEM.md](PERMISSION_SYSTEM.md) · [ROUTING_FOUNDATION.md](ROUTING_FOUNDATION.md)

---

## Overview

All authentication interacts with the verified backend endpoints at `POST /auth/*` and `GET /api/users/me`. No mock or placeholder logic exists.

---

## Backend Endpoints (Verified)

| Endpoint | Method | Auth Required | Purpose |
|---|---|---|---|
| `/auth/login` | POST | No | Exchange credentials for access + refresh tokens |
| `/auth/register` | POST | No | Register student account, returns tokens |
| `/auth/refresh` | POST | No | Exchange refresh token for new token pair |
| `/auth/logout` | POST | No | Revoke refresh token server-side |
| `/auth/forgot-password` | POST | No | Trigger password reset email |
| `/api/users/me` | GET | Yes (Bearer) | Return `{email, role}` for the current token |

### Request / Response shapes

**Login request** (`POST /auth/login`):
```json
{ "email": "user@example.com", "password": "secret" }
```

**Token response** (login, register, refresh):
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "base64url-opaque-string",
  "accessTokenExpiresIn": 900000,
  "tokenType": "Bearer"
}
```

**User profile** (`GET /api/users/me`):
```json
{ "email": "user@example.com", "role": "ROLE_STUDENT" }
```

---

## Login Flow

```
User submits LoginPage form
        │
        ▼
useLogin() → POST /auth/login
        │
        ├─ Success → GET /api/users/me
        │                │
        │                ▼
        │           setAuth(accessToken, refreshToken, user)
        │           sessionStorage: __access_token__
        │           localStorage:   __refresh_token__
        │           Zustand:        { user, isAuthenticated: true }
        │                │
        │                ▼
        │           Navigate to `from` (return URL) or /dashboard
        │
        └─ Error → mutation.error set → Alert rendered in LoginPage
```

---

## Register Flow

```
User submits RegisterPage form
        │
        ▼
useRegister() → POST /auth/register { email, password, role: "ROLE_STUDENT" }
        │
        ├─ Success → GET /api/users/me → setAuth() → navigate /dashboard
        └─ Error → mutation.error → Alert in RegisterPage

Note: Public registration is restricted to ROLE_STUDENT by the backend.
```

---

## Logout Flow

```
User triggers logout (e.g., sidebar button)
        │
        ▼
useLogout()
        ├─ POST /auth/logout { refreshToken }   ← server revokes token
        │   (error silently swallowed — cleanup always proceeds)
        ├─ clearAuth()                           ← Zustand + storage cleared
        ├─ queryClient.clear()                   ← TanStack Query cache flushed
        └─ navigate /login
```

---

## Token Refresh Flow (Axios Interceptor)

Implemented in `src/lib/axios.ts`. Transparent to all callers.

```
API request returns 401
        │
        ├─ No refresh token in localStorage?
        │       └─ clearAuth() → redirect /login
        │
        ├─ Refresh already in progress?
        │       └─ Queue request, wait for refresh result, retry
        │
        └─ Start refresh: POST /auth/refresh { refreshToken }
                ├─ Success → update sessionStorage + localStorage + Zustand
                │           → process queue → retry original request
                └─ Failure → clearAuth() → redirect /login
```

Race condition protection: `isRefreshing` flag + `failedQueue` ensure only one refresh request runs concurrently. All queued requests replay once resolved.

---

## Forgot Password Flow

```
User submits ForgotPasswordPage form
        │
        ▼
POST /auth/forgot-password?email=<email>
        │
        ├─ 202 Accepted → show "check your inbox" success state
        └─ Error → show error alert

Backend note: The email dispatch is a placeholder (Phase 6 outbox). The endpoint
accepts the request and returns 202 regardless of whether the email exists.
```

---

## Token Storage Strategy

| Token | Storage | Rationale |
|---|---|---|
| Access token | `sessionStorage['__access_token__']` + Zustand | Short-lived (15 min); cleared on tab close; never persisted |
| Refresh token | `localStorage['__refresh_token__']` | Persists across tab/browser close for session restore |
| User profile | Zustand `persist` → `localStorage['placement-auth']` | Allows UI to render before `/me` resolves; never contains a token |

---

## Feature Files

| File | Responsibility |
|---|---|
| `features/auth/pages/LoginPage.tsx` | Login form, error display, forgot-password link |
| `features/auth/pages/RegisterPage.tsx` | Registration form (ROLE_STUDENT only) |
| `features/auth/pages/ForgotPasswordPage.tsx` | Password reset request, success state |
| `features/auth/hooks/use-login.ts` | TanStack Query mutation wrapping login + `/me` |
| `features/auth/hooks/use-register.ts` | TanStack Query mutation wrapping register + `/me` |
| `features/auth/hooks/use-logout.ts` | Logout callback: server revoke + client cleanup |
| `features/auth/hooks/use-session-restore.ts` | Silent session restore on app mount |
| `features/auth/schemas/auth.schemas.ts` | Zod schemas for all auth forms |
| `features/auth/SessionProvider.tsx` | Wraps app; shows spinner while restoring session |
| `lib/api/auth.api.ts` | Raw API calls |
| `lib/axios.ts` | Axios client + request/response interceptors |
| `stores/auth.store.ts` | Zustand auth state with `persist` middleware |
