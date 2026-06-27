# Component Library

**Phase:** 2 — Core Infrastructure  
**Last updated:** 2026-06-27  
**Location:** `src/shared/ui/`

---

## Architecture

The design system wraps Radix UI primitives (via shadcn/ui conventions) into an internal component layer. **Feature modules MUST import from `@/shared/ui`, never directly from `@radix-ui/*` or shadcn.**

This layer ensures:
- Consistent visual language across the application
- Single point of change for branding / theming
- Accessible defaults via Radix primitives
- Tailwind v4 CSS variable tokens applied uniformly

---

## Form Controls

| Component | File | Notes |
|---|---|---|
| `Button` | `button.tsx` | 6 variants, 4 sizes, `asChild` support via Radix Slot |
| `Input` | `input.tsx` | `startAdornment` / `endAdornment` slots |
| `PasswordInput` | `password-input.tsx` | Extends Input with show/hide toggle |
| `SearchInput` | `search-input.tsx` | Search icon + optional clear button |
| `Textarea` | `textarea.tsx` | Resizable, full-width |
| `Checkbox` | `checkbox.tsx` | Radix primitive, accessible |
| `RadioGroup` / `RadioGroupItem` | `radio.tsx` | Radix primitive |
| `Switch` | `switch.tsx` | Radix primitive |
| `Slider` | `slider.tsx` | Radix primitive |
| `Select` / `SelectTrigger` / `SelectItem` etc. | `select.tsx` | Full Radix Select tree |
| `Label` | `label.tsx` | Radix Label, links to peer input |

---

## Display

| Component | File | Notes |
|---|---|---|
| `Badge` | `badge.tsx` | 6 variants: default / secondary / destructive / success / warning / outline |
| `Avatar` / `AvatarImage` / `AvatarFallback` | `avatar.tsx` | Radix Avatar |
| `Tooltip` / `TooltipTrigger` / `TooltipContent` | `tooltip.tsx` | Must be inside `TooltipProvider` (registered in `App.tsx`) |
| `Card` / `CardHeader` / `CardTitle` / etc. | `card.tsx` | Composable card layout |
| `Alert` / `AlertTitle` / `AlertDescription` | `alert.tsx` | 4 variants: default / destructive / success / warning |
| `Progress` | `progress.tsx` | Radix progress |
| `EmptyState` | `empty-state.tsx` | Dashed-border empty data state |
| `ErrorState` | `error-state.tsx` | Retry-capable error state |
| `Separator` | `separator.tsx` | Horizontal or vertical divider |

---

## Navigation

| Component | File | Notes |
|---|---|---|
| `Breadcrumb` family | `breadcrumb.tsx` | ARIA `nav[aria-label]` with ellipsis |
| `Tabs` family | `tabs.tsx` | Radix Tabs |
| `Accordion` family | `accordion.tsx` | Radix Accordion |
| `DropdownMenu` family | `dropdown.tsx` | Full Radix Dropdown tree |
| `Pagination` | `pagination.tsx` | Page-number buttons with ellipsis for Spring Data `Page<T>` responses |

---

## Overlay

| Component | File | Notes |
|---|---|---|
| `Dialog` family | `dialog.tsx` | Full-screen on mobile (`< sm`), `max-w-lg` on desktop |
| `Drawer` / `DrawerContent` etc. | `drawer.tsx` | Left / right side-panel via `side` prop |
| `Popover` / `PopoverContent` | `popover.tsx` | Radix Popover |

---

## Data

| Component | File | Notes |
|---|---|---|
| `Table` family | `table.tsx` | Horizontally scrollable wrapper |

---

## Feedback

| Component | File | Notes |
|---|---|---|
| `Skeleton` | `skeleton.tsx` | Pulse animation loading placeholder |
| `Spinner` | `spinner.tsx` | 4 sizes (`sm` / `md` / `lg` / `xl`), `role="status"` |
| `LoadingOverlay` | `loading-overlay.tsx` | Absolute overlay with backdrop blur |

---

## Upload

| Component | File | Notes |
|---|---|---|
| `FileUpload` | `file-upload.tsx` | Drag & drop + click, type/size validation, upload progress bar |

---

## Error Handling

| Component | File | Notes |
|---|---|---|
| `ErrorBoundary` | `error-boundary.tsx` | Global, inline (`inline` prop), or custom fallback (`fallback` prop) |

---

## Usage

```tsx
// Always import from @/shared/ui — never from @radix-ui or shadcn directly
import { Button, Card, CardContent, Badge, Spinner } from '@/shared/ui'
```

---

## Form Infrastructure (`src/shared/forms/`)

| Export | Purpose |
|---|---|
| `FormField` | Wraps any input with Label, hint text, and error message. Auto-generates `id`, `aria-describedby`, `aria-invalid`. |
| `FormSection` | `<fieldset>` wrapper with `<legend>` for grouping related fields |
| `VALIDATION_MESSAGES` | Standard validation string constants for consistent error wording |

```tsx
import { FormField, VALIDATION_MESSAGES } from '@/shared/forms'

<FormField label="Email" required error={errors.email?.message}>
  <Input type="email" {...register('email')} />
</FormField>
```

---

## Accessibility Guarantees

- All interactive components have visible focus rings
- Keyboard navigation follows WAI-ARIA patterns (via Radix)
- ARIA attributes are set automatically by Radix primitives
- `ErrorBoundary` inline mode uses `role="alert"`
- `Spinner` uses `role="status"` and a screen-reader-only label
- `EmptyState` and `ErrorState` are pure divs — callers should ensure surrounding context labels
