# App Shell Architecture

**Phase:** 2 — Core Infrastructure (updated)  
**Last updated:** 2026-06-27

---

## Provider Tree

```
<ErrorBoundary>                     ← catches render errors from all descendants
  <QueryProvider>                   ← TanStack Query context + devtools
    <ThemeProvider>                 ← applies dark/light class to <html>
      <TooltipProvider>             ← Radix Tooltip context (required once, app-wide)
        <NotificationProvider />    ← renders the Sonner <Toaster> (self-closing)
        <BrowserRouter>             ← React Router v7 history context
          <Suspense fallback=…>     ← lazy-chunk loading boundary
            <Routes>                ← route matching
```

Provider order is intentional:

- **ErrorBoundary** wraps everything — even a broken QueryProvider is caught.
- **QueryProvider** must wrap ThemeProvider so theme queries (if added later) work.
- **ThemeProvider** must be above the router so layout components read the correct theme on first render.
- **TooltipProvider** must wrap the router — Radix Tooltip requires exactly one ancestor provider anywhere in the tree. Placing it here avoids duplicate providers per layout.
- **NotificationProvider** renders the Sonner `<Toaster>` as a sibling to `BrowserRouter` — it must be inside `ThemeProvider` so `useTheme()` works when picking the toaster's dark/light theme.
- **BrowserRouter** is innermost non-route provider so route context is available to all route-level code.

---

## Entry Point (`src/main.tsx`)

```tsx
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
```

React 19 StrictMode is always on — it double-invokes effects in development to surface side-effect bugs early.

---

## Lazy Route Loading

Every page component is loaded with `React.lazy()`. The Suspense boundary in `AppRoutes` shows `<LoadingPage>` (a full-screen `<Spinner size="lg">`) during chunk download.

```tsx
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
```

Each page becomes an independent Vite chunk, enabling fine-grained code splitting.

---

## Error Boundary

`src/shared/ui/error-boundary.tsx` — class component (required; no hook equivalent for `componentDidCatch`).

Three usage modes:

| Mode | Props | Renders |
|---|---|---|
| Global (default) | none | Full-page recovery UI with Reload + Try Again buttons |
| Inline / feature | `inline` | Compact error bar with Retry — no full-page takeover |
| Custom | `fallback` | Caller-provided React node |

The `onError` callback prop is the hook point for Sentry or other error-logging services (Phase 5 Observability).

---

## Build Output (Phase 2 baseline)

| Chunk | Size (gzip) |
|---|---|
| `index.js` (vendor + app shell + shared UI) | ~135 kB |
| `DashboardPage.js` | ~0.4 kB |
| `NotFoundPage.js` | ~0.5 kB |
| `ForbiddenPage.js` | ~0.5 kB |
| `index.css` | ~7.5 kB |
