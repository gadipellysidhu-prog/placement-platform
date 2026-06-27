# Theme System

**Phase:** 2 — Core Infrastructure (updated)  
**Last updated:** 2026-06-27

---

## Architecture

The theme system has three layers:

```
CSS custom properties (HSL channels)   ← source of truth
        ↓
Tailwind v4 @theme block               ← maps tokens to utility classes
        ↓
ThemeProvider + Zustand theme store    ← manages .dark class on <html>
```

---

## Design Tokens

`src/index.css` declares all tokens as raw HSL channels on `:root` and `.dark`.

Using raw channels (not `hsl(…)`) enables Tailwind's opacity modifier syntax:

```css
/* token definition */
--primary: 221.2 83.2% 53.3%;

/* usage */
.bg-primary\/50 { background-color: hsl(221.2 83.2% 53.3% / 0.5); }
```

### Token Groups

| Group | Tokens |
|---|---|
| Surfaces | `background`, `foreground`, `card`, `popover` (each with `-foreground`) |
| Brand | `primary`, `secondary` (each with `-foreground`) |
| Neutral | `muted`, `accent` (each with `-foreground`) |
| Semantic | `destructive`, `success`, `warning` (each with `-foreground`) |
| Structure | `border`, `input`, `ring` |
| Shape | `radius` (0.5rem base) |
| Typography | `font-sans`, `font-mono` |

---

## Tailwind v4 Integration

The `@theme {}` block in `src/index.css` maps CSS variable tokens to Tailwind utility classes:

```css
@theme {
  --color-primary: hsl(var(--primary));
  --radius-lg: var(--radius);
  /* … */
}
```

No `tailwind.config.js` — Tailwind v4 is configured entirely via CSS.

---

## Theme Store

`src/stores/theme.store.ts` — Zustand persisted to `localStorage` key `placement-theme`.

```ts
type Theme = 'light' | 'dark' | 'system'

state: { theme: Theme }
actions: { setTheme(theme: Theme): void }
```

---

## ThemeProvider

`src/providers/ThemeProvider.tsx`

- Reads the resolved theme (honouring `system` via `matchMedia`).
- Adds/removes the `.dark` class on `document.documentElement`.
- Listens to `prefers-color-scheme` changes when theme is `'system'`.

The provider **does not** use a React context — components read the store directly:

```ts
const { theme, setTheme } = useThemeStore()
```

---

## `useTheme` Hook (Phase 2)

`src/shared/hooks/use-theme.ts` — convenience hook wrapping the store:

```ts
import { useTheme } from '@/shared/hooks'

const { theme, setTheme, isDark } = useTheme()
// isDark resolves the 'system' preference via matchMedia — ready to use in components
```

The `isDark` boolean is computed at call time (not stored), so it always reflects the current OS preference when `theme === 'system'`.

---

## Dark Mode Strategy

Tailwind's `darkMode: 'class'` (the default in v4) is used. The `.dark` class on `<html>` switches the entire token set, requiring zero component-level changes to support dark mode.
