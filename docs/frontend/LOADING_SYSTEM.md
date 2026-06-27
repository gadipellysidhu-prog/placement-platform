# Loading System

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27

---

## Loading Primitives

| Component / Hook | File | Use case |
|---|---|---|
| `Spinner` | `src/shared/ui/spinner.tsx` | Inline loading indicator — 4 sizes (`sm`, `md`, `lg`, `xl`) |
| `Skeleton` | `src/shared/ui/skeleton.tsx` | Placeholder shapes during data fetch |
| `LoadingOverlay` | `src/shared/ui/loading-overlay.tsx` | Semi-transparent overlay on top of existing content |
| `LoadingPage` | `src/pages/LoadingPage.tsx` | Full-page `<Spinner size="lg">` — Suspense fallback for lazy routes |
| `useLoading` | `src/shared/hooks/use-loading.ts` | Imperative loading state outside TanStack Query |

---

## Patterns

### Route-level (Suspense + lazy)

```tsx
const StudentsPage = lazy(() => import('@/features/students/pages/StudentsPage'))

// In App.tsx — already wired:
<Suspense fallback={<LoadingPage />}>
  <Routes>...</Routes>
</Suspense>
```

### Table / list skeleton

```tsx
import { Skeleton } from '@/shared/ui'

{isLoading && Array.from({ length: 5 }).map((_, i) => (
  <Skeleton key={i} className="h-10 w-full" />
))}
```

### Card loading

```tsx
{isLoading ? (
  <div className="space-y-3">
    <Skeleton className="h-6 w-48" />
    <Skeleton className="h-4 w-full" />
    <Skeleton className="h-4 w-3/4" />
  </div>
) : <RealContent />}
```

### Button loading state

```tsx
<Button disabled={mutation.isPending}>
  {mutation.isPending && <Spinner size="sm" />}
  Save
</Button>
```

### Overlay on mutation

```tsx
<div className="relative">
  <LoadingOverlay visible={mutation.isPending} label="Saving…" />
  <Form />
</div>
```

---

## TanStack Query Integration

Every query exposes three loading flags:

| Flag | Meaning |
|---|---|
| `isLoading` | First fetch — no cached data yet. Show skeleton. |
| `isFetching` | Any in-flight fetch, including background refresh. Show subtle indicator. |
| `isPending` | Mutation is in flight. Disable submit button. |

Prefer skeleton loading over spinner for content areas — it eliminates layout shift and gives users a structural preview of the incoming data.
