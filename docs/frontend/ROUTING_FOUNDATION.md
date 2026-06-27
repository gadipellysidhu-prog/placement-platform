# Routing Foundation

**Phase:** 1 — Foundation  
**Last updated:** 2026-06-27

---

## Route Map

| Path | Guard | Layout | Component |
|---|---|---|---|
| `/` | — | — | Redirect → `/dashboard` |
| `/login` | PublicRoute | AuthLayout | LoginPage (Phase 1 Auth) |
| `/register` | PublicRoute | AuthLayout | RegisterPage (Phase 1 Auth) |
| `/forgot-password` | PublicRoute | AuthLayout | ForgotPasswordPage (Phase 1 Auth) |
| `/dashboard` | ProtectedRoute | DashboardLayout | DashboardPage |
| `/403` | — | — | ForbiddenPage |
| `/404` | — | — | NotFoundPage |
| `*` | — | — | NotFoundPage |

Phase 2+ will add role-scoped sub-routes under `/dashboard/` using nested `<Route>` elements inside the existing ProtectedRoute tree.

---

## Route Guards

### `ProtectedRoute`

`src/routes/ProtectedRoute.tsx`

```
isAuthenticated?
  ├─ No  → Navigate /login (preserves `from` in location.state for post-login redirect)
  └─ Yes → allowedRoles provided?
              ├─ role not in allowedRoles → Navigate /403
              └─ role OK → <Outlet />
```

Usage with role restriction:
```tsx
<Route element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]} />}>
  <Route path="/admin/users" element={<AdminUsersPage />} />
</Route>
```

### `PublicRoute`

`src/routes/PublicRoute.tsx`

Redirects authenticated users to `/dashboard`. Used to prevent logged-in users from accessing login/register pages.

---

## `ROUTES` Constant

`src/constants/routes.ts` — single source of truth for all URL strings.

```ts
ROUTES.LOGIN                         // '/login'
ROUTES.DASHBOARD                     // '/dashboard'
ROUTES.STUDENT.JOB_POSTING_DETAIL(id) // '/dashboard/jobs/:id'
ROUTES.OFFICER.STUDENT_DETAIL(id)     // '/dashboard/students/:id'
```

Dynamic segments are typed functions `(id: string) => string`. Never use string literals for navigation — always import from `ROUTES`.

---

## Adding New Routes (Phase 2+)

1. Add the path constant to `ROUTES` in `src/constants/routes.ts`.
2. Create the page at `src/features/<name>/pages/<PageName>.tsx`.
3. Lazy-import the page in `App.tsx`.
4. Nest the `<Route>` inside the appropriate guard element.
5. Add the nav item to `src/layouts/components/Sidebar.tsx` with the correct `allowedRoles`.
