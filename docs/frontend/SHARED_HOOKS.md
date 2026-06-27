# Shared Hooks

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27  
**Location:** `src/shared/hooks/`

---

## Import Convention

```ts
import { useDisclosure, usePagination, usePermission } from '@/shared/hooks'
```

All hooks are pure and composable — no side effects on import.

---

## Hook Reference

### State & UI

| Hook | Signature | Purpose |
|---|---|---|
| `useDisclosure` | `(initial?) → { isOpen, open, close, toggle }` | Toggle state for dialogs, drawers, dropdowns |
| `useLoading` | `(initial?) → { isLoading, start, stop, wrap }` | Imperative async loading state outside TanStack Query |
| `usePrevious` | `<T>(value) → T \| undefined` | Returns the value from the previous render |
| `useMounted` | `() → RefObject<boolean>` | Stable ref that reflects whether the component is mounted |

### API & Data

| Hook | Signature | Purpose |
|---|---|---|
| `usePagination` | `(options?) → { params, setPage, setSize, setSort, reset }` | Spring Data `Page<T>` page/size/sort state; resets page on size or sort change |
| `useSearch` | `(delay?) → { query, debouncedQuery, setQuery, clear }` | Search input state with debounce |
| `useDebounce` | `<T>(value, delay?) → T` | Debounced value; default delay 400 ms |
| `useQueryParams` | `<T>() → { getParam, setParam, setParams, clearParams }` | Typed URL search param read/write |
| `useInfiniteScroll` | `(options) → { sentinelRef }` | IntersectionObserver sentinel ref for infinite lists |
| `useFileUpload` | `() → { upload, uploading, progress, error, reset }` | Single-file upload lifecycle against `/api/files/upload` |

### Auth & Permissions

| Hook | Signature | Purpose |
|---|---|---|
| `usePermission` | `() → { hasRole, isAdmin, isOfficer, isStudent, role }` | Role-based permission checks; hierarchy-aware |

### UI & Theming

| Hook | Signature | Purpose |
|---|---|---|
| `useTheme` | `() → { theme, setTheme, isDark }` | Wraps theme store; `isDark` is reactive to OS preference |
| `useToast` | `() → { success, error, warning, info, loading, dismiss, promise }` | Sonner toast API |
| `useBreakpoint` | `() → { sm, md, lg, xl, isMobile, isTablet, isDesktop }` | Tailwind breakpoint detection |
| `useMediaQuery` | `(query) → boolean` | Generic CSS media query, SSR-safe |

### Storage

| Hook | Signature | Purpose |
|---|---|---|
| `useLocalStorage` | `<T>(key, initial) → [value, set, remove]` | Typed localStorage with JSON serialization |
| `useSessionStorage` | `<T>(key, initial) → [value, set, remove]` | Typed sessionStorage with JSON serialization |
| `useClipboard` | `(resetDelay?) → { copy, copied }` | Clipboard write with transient `copied` feedback |

---

## Notes

- `usePagination` resets `page` to 0 whenever `size` or `sort` changes — prevents stale page number on config change
- `usePermission` respects the role hierarchy: ADMIN ≥ PLACEMENT_OFFICER ≥ STUDENT
- `useFileUpload` wraps `filesApi.upload` from `@/lib/api` — no direct Axios usage in components
- `useInfiniteScroll` uses `IntersectionObserver` — no polling interval
- `useTheme().isDark` subscribes to `prefers-color-scheme` changes via `useMediaQuery` — reactive when OS theme switches while the app is open
