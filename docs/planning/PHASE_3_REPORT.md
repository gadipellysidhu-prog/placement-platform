# Phase 3 Report — Authentication & Authorization

**Date:** 2026-06-27  
**Phase:** 3 (Frontend) — Authentication & Authorization  
**Status:** COMPLETE

---

## 1. Repository Audit Summary

### Backend (verified before writing any code)

| Area | Finding |
|---|---|
| Auth controller | `POST /auth/login`, `/register`, `/refresh`, `/logout`, `/verify-email`, `/forgot-password` — all verified real |
| User controller | `GET /api/users/me` returns `{email, role}` — verified real |
| Roles | `ROLE_STUDENT`, `ROLE_PLACEMENT_OFFICER`, `ROLE_ADMIN` — enum verified |
| Role hierarchy | `ADMIN > PLACEMENT_OFFICER > STUDENT` — wired in Spring Security |
| Token response | `accessToken`, `refreshToken`, `accessTokenExpiresIn` (ms), `tokenType: "Bearer"` |
| JWT | RS256, signed by `JwtService`; ephemeral key or env-var-supplied |
| Refresh token | Stored as SHA-256 hash in DB; single-use (rotated on each refresh) |
| Brute-force | Lockout after N failures; configurable via `SecurityProperties` |

### Frontend pre-existing infrastructure (found complete)

- `lib/axios.ts` — Axios client + 401 refresh interceptor with race-condition queue
- `lib/api/auth.api.ts` — All auth API functions
- `stores/auth.store.ts` — Zustand store with `persist` middleware
- `types/auth.ts` — `Role`, `User`, `AuthTokens` types
- `routes/ProtectedRoute.tsx`, `PublicRoute.tsx`, `RoleRoute.tsx` — All three guards
- `shared/hooks/use-permission.ts` — `hasRole()`, `isAdmin`, `isOfficer`, `isStudent`
- `constants/roles.ts`, `constants/routes.ts` — All constants
- `layouts/AuthLayout.tsx` — Centered card layout
- `shared/ui/*` — Full component library (Button, Input, PasswordInput, Alert, Spinner, FormField…)

---

## 2. Existing Documents Updated

| Document | Change |
|---|---|
| `docs/planning/IMPLEMENTATION_PHASES.md` | Phase 1 auth pages marked implemented |
| `src/App.tsx` | Replaced placeholder route content with real lazy-loaded pages + `SessionProvider` |

---

## 3. Newly Created Documents

| Document | Purpose |
|---|---|
| `docs/frontend/AUTH_FLOW.md` | Login, register, logout, refresh, forgot-password flows |
| `docs/frontend/SESSION_MANAGEMENT.md` | Session lifecycle, storage map, auto-logout, multi-tab |
| `docs/planning/PHASE_3_REPORT.md` | This report |

---

## 4. Files Created

| File | Description |
|---|---|
| `src/features/auth/schemas/auth.schemas.ts` | Zod schemas for login, register, forgot-password forms |
| `src/features/auth/hooks/use-login.ts` | Login mutation (login + /me → setAuth → navigate) |
| `src/features/auth/hooks/use-register.ts` | Register mutation (register + /me → setAuth → navigate) |
| `src/features/auth/hooks/use-logout.ts` | Logout callback (revoke → clearAuth → queryClient.clear → /login) |
| `src/features/auth/hooks/use-session-restore.ts` | Silent session restore hook |
| `src/features/auth/pages/LoginPage.tsx` | Enterprise login form |
| `src/features/auth/pages/RegisterPage.tsx` | Student registration form |
| `src/features/auth/pages/ForgotPasswordPage.tsx` | Forgot password form + success state |
| `src/features/auth/SessionProvider.tsx` | Spinner wrapper during session restore |
| `src/features/auth/index.ts` | Public exports |

---

## 5. Files Modified

| File | Change |
|---|---|
| `src/App.tsx` | Wired real auth pages (lazy), wrapped routes in `SessionProvider` |

---

## 6. Backend Endpoints Integrated

| Endpoint | Used by |
|---|---|
| `POST /auth/login` | `use-login.ts` |
| `POST /auth/register` | `use-register.ts` |
| `POST /auth/refresh` | `lib/axios.ts` interceptor + `use-session-restore.ts` |
| `POST /auth/logout` | `use-logout.ts` |
| `POST /auth/forgot-password` | `ForgotPasswordPage.tsx` direct call |
| `GET /api/users/me` | `use-login.ts`, `use-register.ts`, `use-session-restore.ts` |

---

## 7. Backend Endpoints Missing (BACKEND REQUIRED)

None for this phase. All required auth endpoints are fully implemented in the backend.

> **Note:** `/auth/verify-email` and `/auth/forgot-password` are accepted by the backend (202) but email dispatch is a placeholder until Phase 6 (Transactional Outbox). The frontend correctly calls these endpoints and handles the 202 response.

---

## 8. Authentication Architecture Summary

```
SessionProvider (app mount)
    └─ useSessionRestore
           ├─ ready: render routes
           └─ restoring: spinner

PublicRoute (login/register/forgot-password)
    └─ Redirect authenticated users to /dashboard

ProtectedRoute (dashboard + nested)
    └─ Redirect unauthenticated users to /login
    └─ RoleRoute: redirect insufficient-role to /403

Axios interceptor (all requests)
    └─ Attach Bearer token from sessionStorage
    └─ On 401: silent refresh → retry; fail → clearAuth + /login

useLogin / useRegister / useLogout
    └─ Mutations that update Zustand + storage
```

### Token storage:

| Token | Storage | Lifetime |
|---|---|---|
| Access token | `sessionStorage` + Zustand | Until tab close (~15 min) |
| Refresh token | `localStorage` | Backend-configured TTL |
| User profile | Zustand `persist` → `localStorage` | Until logout |

---

## 9. Test Results

No test runner is configured in the project (`vitest` not in `package.json`). Tests are planned for Phase 6 (Quality). The implementation is structured for testability:
- Hooks are pure async functions with no side-effects beyond store mutation
- `useSessionRestore` uses a `cancelled` flag for safe unmounting
- Zod schemas are independently testable

---

## 10. Remaining Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Multi-tab logout latency | Low | Tab B will auto-logout on its next 401; no active signalling |
| `/auth/forgot-password` email not sent | Low | Backend placeholder; backend will implement in Phase 6 |
| `accessTokenExpiresIn` not used client-side | Low | Token expiry is handled by server returning 401; interceptor refreshes |
| No CSRF protection on auth endpoints | Low | Endpoints are not cookie-authenticated; Bearer token not CSRF-vulnerable |

---

## 11. Suggested Phase 4 Prerequisites

Before starting the next feature phase (Job Postings & Applications per `IMPLEMENTATION_PHASES.md`):

1. Verify backend endpoints for students/companies are active
2. Implement `DashboardLayout` sidebar logout button using `useLogout()`
3. Add `useMe` TanStack Query hook for profile re-validation on session restore
4. Consider adding `RoleRoute` to protected nested routes in `App.tsx` once domain pages are built
