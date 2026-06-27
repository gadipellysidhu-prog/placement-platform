# Layout System

**Phase:** 1 — Foundation  
**Last updated:** 2026-06-27

---

## Layouts

Three layout components in `src/layouts/`:

### `AuthLayout`

Centered card layout for public authentication pages.

```
┌─────────────────────────────┐
│          Brand header        │
│   ┌─────────────────────┐   │
│   │   <Outlet />        │   │   max-w-md card, rounded, bordered
│   │  (login / register) │   │
│   └─────────────────────┘   │
└─────────────────────────────┘
```

Used by: `/login`, `/register`, `/forgot-password`

### `DashboardLayout`

Full-screen shell for authenticated pages.

```
┌──────────┬────────────────────────────┐
│          │  TopNav (h-14)             │
│ Sidebar  ├────────────────────────────┤
│ (fixed)  │                            │
│          │  <main> <Outlet />         │
│          │  (scrollable, p-6)         │
│          │                            │
└──────────┴────────────────────────────┘
```

- Sidebar is `position: fixed`, shifts the content area via `margin-left` transition.
- Content area transitions smoothly when the sidebar collapses (`w-64` → `w-16`).
- Mobile: sidebar slides in as a drawer with a backdrop.

### `MinimalLayout`

Bare wrapper used for error pages (404, 403) — full-height background only.

---

## Layout Usage in Routing

Layouts are applied as wrapper `<Route>` elements:

```tsx
<Route element={<AuthLayout />}>
  <Route path="/login" element={<LoginPage />} />
</Route>

<Route element={<ProtectedRoute />}>
  <Route element={<DashboardLayout />}>
    <Route path="/dashboard" element={<DashboardPage />} />
  </Route>
</Route>
```

---

## Responsive Breakpoints

| Context | Behaviour |
|---|---|
| `< lg` (mobile/tablet) | Sidebar hidden; hamburger button in TopNav opens drawer |
| `≥ lg` (desktop) | Sidebar always visible; collapse button shrinks it to icon-only |

The sidebar state (`sidebarOpen`, `sidebarCollapsed`) is managed in `src/stores/ui.store.ts` and persisted across page reloads.
