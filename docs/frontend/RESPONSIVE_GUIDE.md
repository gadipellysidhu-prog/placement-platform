# Responsive Guide

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27

---

## Breakpoints

Tailwind CSS v4 uses mobile-first `min-width` breakpoints:

| Name | Min width |
|---|---|
| `sm` | 640px |
| `md` | 768px |
| `lg` | 1024px |
| `xl` | 1280px |
| `2xl` | 1536px |

---

## Hooks

### `useBreakpoint()`

Returns an object with boolean flags for each Tailwind breakpoint. Takes no arguments.

```ts
import { useBreakpoint } from '@/shared/hooks'

const { sm, md, lg, xl, isMobile, isTablet, isDesktop } = useBreakpoint()

// Named flags:     sm, md, lg, xl, '2xl'  (true when viewport ≥ that breakpoint)
// Convenience:     isMobile  → !md
//                  isTablet  → md && !lg
//                  isDesktop → lg
```

All flags are reactive — they re-render when the viewport crosses a breakpoint via `matchMedia` change events.

### `useMediaQuery(query)`

Low-level hook for arbitrary media queries. Internally used by `useBreakpoint`.

```ts
const isPortrait = useMediaQuery('(orientation: portrait)')
const reducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)')
```

SSR-safe: defaults to `false` when `window` is unavailable.

---

## Patterns

### CSS-first (preferred)

Prefer Tailwind responsive variants over JS breakpoint hooks wherever CSS alone is sufficient:

```tsx
// Two-column on desktop, one-column on mobile
<div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
  ...
</div>

// Hide on mobile, show on desktop
<aside className="hidden lg:flex flex-col w-64">
  ...
</aside>
```

### JS-driven responsive (when CSS isn't enough)

```tsx
const { isMobile } = useBreakpoint()

return isMobile ? <MobileList /> : <DesktopTable />
```

### Dialog vs Drawer on mobile

The `Dialog` component renders full-screen on mobile (below `sm`). For bottom-sheet patterns:

```tsx
const { isMobile } = useBreakpoint()

return isMobile
  ? <Drawer side="bottom"><DrawerContent>...</DrawerContent></Drawer>
  : <Dialog><DialogContent>...</DialogContent></Dialog>
```

---

## DashboardLayout Responsive Behaviour

| Viewport | Sidebar | Hamburger |
|---|---|---|
| `< lg` (mobile/tablet) | Hidden by default; opens as drawer with backdrop | Visible in TopNav |
| `≥ lg` (desktop) | Always visible; collapses to icon-only strip | Hidden |

Sidebar state (`sidebarOpen`, `sidebarCollapsed`) is stored in `useUIStore` (Zustand, not persisted across page reloads).

---

## Design Tokens

All spacing and sizing tokens are defined in `src/index.css` under the `@theme` block. Always use Tailwind utility classes for responsive sizing — inline styles bypass breakpoint variants and the token system.
