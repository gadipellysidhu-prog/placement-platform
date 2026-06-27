# Navigation Architecture

**Phase:** 1 — Foundation  
**Last updated:** 2026-06-27

---

## Components

### Sidebar (`src/layouts/components/Sidebar.tsx`)

Role-filtered navigation menu with three display states:

| State | Trigger | Visual |
|---|---|---|
| Expanded | Default on desktop | Icon + label, w-64 |
| Collapsed | Desktop collapse button | Icon only, w-16, tooltip on hover |
| Mobile drawer | Hamburger in TopNav | Slides in from left, backdrop overlay |

**Role filtering** — nav items declare `allowedRoles?: Role[]`. Items with no restriction show to all authenticated users. Items with a restriction only render if `user.role` is in the list.

**Active state** — detected via `useLocation()`. The Dashboard link requires exact match (`pathname === href`). All other links use prefix match (`pathname.startsWith(href)`).

**Accessibility** — landmark `<nav aria-label="Main navigation">`, `aria-current="page"` on the active link, `aria-label` on icon-only buttons, `aria-hidden` on the backdrop.

### TopNav (`src/layouts/components/TopNav.tsx`)

Fixed-height header bar containing:

- **Hamburger button** (mobile only, `lg:hidden`) — calls `useUIStore().toggleSidebar()`
- **User info chip** (right side) — displays email, role label, and avatar initial

---

## Navigation Items (Phase 1)

```
Dashboard          → /dashboard              (all roles)
Students           → /dashboard/students     (OFFICER, ADMIN)
Companies          → /dashboard/companies    (all roles)
Job Postings       → /dashboard/jobs         (OFFICER, ADMIN)
Applications       → /dashboard/applications (OFFICER, ADMIN)
Certificates       → /dashboard/certificates (OFFICER, ADMIN)
Skills             → /dashboard/skills       (OFFICER, ADMIN)
Branches           → /dashboard/branches     (OFFICER, ADMIN)
```

Phase 2 will add student-facing nav items (My Profile, Job Board, My Applications, My Offers, My Certificates).

---

## UI Store

`src/stores/ui.store.ts` — Zustand store (not persisted).

```ts
state: {
  sidebarOpen: boolean       // mobile drawer visibility
  sidebarCollapsed: boolean  // desktop collapse state
}
actions: {
  toggleSidebar(): void
  setSidebarOpen(open: boolean): void
  toggleSidebarCollapsed(): void
}
```

The sidebar persists its collapsed state in memory only (cleared on page reload). If persistence is desired later, add the Zustand `persist` middleware with a separate localStorage key.

---

## Adding New Nav Items

1. Add the route constant to `src/constants/routes.ts`.
2. Add a `NavItem` entry to the `NAV_ITEMS` array in `Sidebar.tsx`.
3. Import the appropriate Lucide icon.
4. Set `allowedRoles` if the item should be role-restricted.
