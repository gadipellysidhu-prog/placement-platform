# Permission System

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27

---

## Role Hierarchy

```
ADMIN (rank 3) > PLACEMENT_OFFICER (rank 2) > STUDENT (rank 1)
```

Defined in `src/constants/roles.ts`. The numeric ranks are an internal implementation detail of `usePermission` — callers always use the string role constants.

---

## Route-Level Guards

These components control which pages are accessible. They live in `src/routes/`.

| Component | File | Behaviour |
|---|---|---|
| `ProtectedRoute` | `ProtectedRoute.tsx` | Redirects unauthenticated users to `/login`; preserves `from` in location state for post-login redirect |
| `RoleRoute` | `RoleRoute.tsx` | Redirects users whose rank is below `minimumRole` to `/403` |
| `PublicRoute` | `PublicRoute.tsx` | Redirects already-authenticated users to `/dashboard` |

```tsx
// Require any logged-in user
<Route element={<ProtectedRoute />}>
  <Route element={<DashboardLayout />}>
    <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
  </Route>
</Route>

// Require officer or above
<Route element={<RoleRoute minimumRole="ROLE_PLACEMENT_OFFICER" />}>
  <Route path={ROUTES.OFFICER.STUDENTS} element={<StudentsPage />} />
</Route>
```

---

## UI-Level Guard

`RoleGuard` (`src/shared/guards/RoleGuard.tsx`) conditionally renders children in the UI — it does **not** protect routes.

```tsx
import { RoleGuard } from '@/shared/guards'

// Show a button only to officers and admins
<RoleGuard minimumRole="ROLE_PLACEMENT_OFFICER">
  <Button>Manage Students</Button>
</RoleGuard>

// Optional fallback
<RoleGuard minimumRole="ROLE_ADMIN" fallback={<ReadOnlyBadge />}>
  <EditControls />
</RoleGuard>
```

---

## Permission Hook

`usePermission` (`src/shared/hooks/use-permission.ts`) — the source of truth for all permission logic.

```ts
import { usePermission } from '@/shared/hooks'

const { isAdmin, isOfficer, isStudent, hasRole, role } = usePermission()

// isAdmin    — true only for ADMIN
// isOfficer  — true for PLACEMENT_OFFICER and ADMIN (hierarchy-aware)
// isStudent  — true for any authenticated user (STUDENT, OFFICER, ADMIN)
// hasRole(r) — true if current rank >= required rank
// role       — the raw Role string or null if unauthenticated
```

---

## Security Note

Frontend permission checks are **for UX only** — they hide/show elements and guard navigation. The Spring Boot backend enforces all authorization via `@PreAuthorize` and the role hierarchy in `MethodSecurityExpressionHandler`. Never rely solely on frontend guards for data security.
