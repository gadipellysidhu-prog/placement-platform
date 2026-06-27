# Session Management

**Phase:** 3 — Authentication & Authorization  
**Last updated:** 2026-06-27  
**Related:** [AUTH_FLOW.md](AUTH_FLOW.md) · [PERMISSION_SYSTEM.md](PERMISSION_SYSTEM.md)

---

## Session Lifecycle

```
Browser open / page reload
        │
        ▼
SessionProvider mounts
        │
        ▼
useSessionRestore()
        ├─ isAuthenticated=false → status='ready' (no restore needed)
        ├─ isAuthenticated=true + accessToken in sessionStorage
        │       → status='ready' (already live)
        ├─ isAuthenticated=true + no accessToken + refreshToken in localStorage
        │       → POST /auth/refresh
        │           ├─ Success → setAuth() → status='ready'
        │           └─ Failure → clearAuth() → status='ready'
        └─ isAuthenticated=true + no accessToken + no refreshToken
                → clearAuth() → status='ready'

While status='restoring' → full-screen spinner (no route rendering)
When status='ready'      → routes render normally
```

---

## State Persistence Map

| Data | Written by | Read by | Storage |
|---|---|---|---|
| `user` (email + role) | `setAuth()` | Zustand `persist` → `localStorage` | Survives tab close |
| `isAuthenticated` | `setAuth()` / `clearAuth()` | Zustand `persist` → `localStorage` | Survives tab close |
| `accessToken` | `setAuth()` / `setAccessToken()` | Axios request interceptor | `sessionStorage` only — lost on close |
| `refreshToken` | `setAuth()` / raw `localStorage.setItem` in interceptor | `useSessionRestore`, Axios interceptor | `localStorage` |

---

## Auto-Logout Triggers

The session is automatically cleared and the user redirected to `/login` in these cases:

1. **401 on a non-refresh request + no refresh token** — Axios interceptor calls `_handleAuthFailure()`
2. **401 on the refresh request** — Axios interceptor calls `_handleAuthFailure()`
3. **`useSessionRestore` fails to refresh** — `clearAuth()` called silently
4. **Manual logout** — `useLogout()` hook

---

## Multi-Tab Behaviour

Each tab has its own `sessionStorage`, so each tab independently holds an access token. The `localStorage` refresh token is shared across tabs.

- If tab A logs out (revokes refresh token), tab B's next 401 will attempt refresh, which will fail (revoked), triggering auto-logout in tab B.
- There is no active cross-tab signalling; cleanup happens on the next authenticated request in each tab.

---

## Session Restore Implementation

`src/features/auth/hooks/use-session-restore.ts`

The hook runs once on mount (empty dependency array) and:
1. Reads `sessionStorage` for an existing access token
2. If `isAuthenticated=true` and no token → triggers silent refresh
3. Returns `'restoring'` until the outcome is known, then `'ready'`
4. Uses a `cancelled` flag to prevent state updates after unmount

`src/features/auth/SessionProvider.tsx` renders the spinner while `status='restoring'` and children otherwise.
