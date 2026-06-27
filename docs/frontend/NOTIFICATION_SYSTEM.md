# Notification System

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27

---

## Architecture

Uses [Sonner](https://sonner.emilkowal.ski/) as the underlying toast library.

- `NotificationProvider` (`src/providers/NotificationProvider.tsx`) — renders the `<Toaster>` once at the app root, inside `ThemeProvider` so it picks up the correct dark/light theme.
- `useToast` (`src/shared/hooks/use-toast.ts`) — thin wrapper hook providing a consistent typed API; the only way feature code should trigger notifications.

---

## Usage

```ts
import { useToast } from '@/shared/hooks'

const { success, error, warning, info, loading, dismiss, promise } = useToast()

// Simple
success('Student created successfully')
error('Failed to save', 'Please check your connection and try again')

// Promise lifecycle — shows loading → success/error automatically
promise(mutateAsync(data), {
  loading: 'Creating student…',
  success: 'Student created',
  error: 'Failed to create student',
})

// Dismissible loading toast
const id = loading('Uploading file…')
// later:
dismiss(id)
```

---

## Toast Types

| Method | Use for |
|---|---|
| `success` | Completed mutations (create, update, delete) |
| `error` | API failures, validation errors shown globally |
| `warning` | Degraded state, confirmation prompts |
| `info` | Non-critical information, tips |
| `loading` | Long-running operations with manual dismiss |
| `promise` | Async operations needing automatic 3-state feedback |

---

## Configuration

Defaults (set in `NotificationProvider`):

| Option | Value |
|---|---|
| Position | `top-right` |
| Duration | 4 000 ms |
| Rich colors | enabled |
| Close button | enabled |
| Theme | follows app theme (`isDark` from `useTheme()`) |
