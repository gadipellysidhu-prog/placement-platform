# Utilities

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27  
**Location:** `src/utils/`

---

## Import Convention

```ts
// All utilities are re-exported from the barrel
import { cn, formatDate, groupBy, omitNullish } from '@/utils'
```

All utility functions are pure — no side effects, no external dependencies beyond standard browser APIs.

---

## Class Names (`utils/cn.ts`)

```ts
cn('px-4 py-2', isActive && 'bg-primary', className)
```

Wraps `clsx` + `tailwind-merge` for safe Tailwind class composition. Always use `cn` when merging dynamic classes — raw string concatenation can produce conflicting Tailwind utilities.

---

## Formatting (`utils/format.ts`)

| Function | Signature | Example output |
|---|---|---|
| `formatCTC` | `(value?) → string` | `"₹12.5 LPA"` |
| `formatCTCRange` | `(min?, max?) → string` | `"₹8 LPA – ₹12.5 LPA"` |
| `formatDateTime` | `(iso?) → string` | `"27 Jun 2026, 14:30"` |
| `formatDate` | `(date?) → string` | `"27 Jun 2026"` |
| `formatFileSize` | `(bytes) → string` | `"10.2 KB"` |
| `emailInitials` | `(email) → string` | `"user@example.com"` → `"US"` |
| `truncate` | `(str, maxLen) → string` | Appends `…` if string exceeds `maxLen` |
| `capitalize` | `(str) → string` | `"hello"` → `"Hello"` |
| `titleCase` | `(str) → string` | `"OPTED_OUT"` → `"Opted Out"` |

Null/undefined inputs to date and CTC formatters return `"—"` (em dash).

---

## Async (`utils/async.ts`)

| Function | Signature | Purpose |
|---|---|---|
| `sleep` | `(ms) → Promise<void>` | Delay; used in retry backoff |
| `withRetry` | `(fn, retries?, baseDelayMs?) → Promise<T>` | Exponential backoff; only retries on `status === 0` or `>= 500` |

```ts
const data = await withRetry(() => externalApi.fetch(), 3, 500)
```

---

## Object (`utils/object.ts`)

| Function | Signature | Purpose |
|---|---|---|
| `omitNullish` | `(obj) → obj` | Removes `null` and `undefined` entries (shallow) |
| `pick` | `(obj, keys[]) → obj` | Returns a new object with only the specified keys |
| `omit` | `(obj, keys[]) → obj` | Returns a new object without the specified keys |

---

## Array (`utils/array.ts`)

| Function | Signature | Purpose |
|---|---|---|
| `groupBy` | `(arr, key) → Record<string, T[]>` | Groups array items by a property key |
| `uniqueBy` | `(arr, keyFn) → T[]` | Deduplicates by a computed key |
| `sortBy` | `(arr, key, dir?) → T[]` | Type-safe sort; non-mutating |
| `chunk` | `(arr, n) → T[][]` | Splits into fixed-size sub-arrays |

---

## URL (`utils/url.ts`)

| Function | Signature | Purpose |
|---|---|---|
| `buildUrl` | `(base, params) → string` | Appends query params, skipping null/undefined values |
| `fileExtension` | `(name) → string` | Extracts lowercase file extension from name or URL |
| `fileDownloadUrl` | `(apiBase, fileId) → string` | Builds the backend download URL for a file by ID |
