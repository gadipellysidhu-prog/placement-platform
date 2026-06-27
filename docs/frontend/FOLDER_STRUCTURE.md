# Frontend Folder Structure

**Phase:** 2 — Core Infrastructure (updated)  
**Last updated:** 2026-06-27

---

## Top-Level Layout

```
frontend/
├── public/                    # Static assets served verbatim
├── src/
│   ├── assets/                # Fonts, icons, images bundled by Vite
│   ├── config/                # Runtime environment validation (env.ts)
│   ├── constants/             # App-wide constants (routes.ts, roles.ts)
│   ├── features/              # Feature modules (Phase 3+)
│   ├── layouts/               # Page shell layouts + nav sub-components
│   │   └── components/        # Sidebar.tsx, TopNav.tsx
│   ├── lib/
│   │   ├── axios.ts           # Axios instance + JWT silent refresh interceptor
│   │   ├── query-client.ts    # TanStack Query client configuration
│   │   └── api/               # Typed service modules (one per API domain)
│   │       ├── keys.ts        # Query key factory
│   │       ├── error.ts       # normalizeApiError, getApiErrorMessage
│   │       ├── *.api.ts       # 11 service modules
│   │       └── index.ts       # Barrel export
│   ├── pages/                 # Route-level page components
│   │   ├── DashboardPage.tsx
│   │   ├── LoadingPage.tsx    # Full-screen Suspense fallback
│   │   ├── NotFoundPage.tsx
│   │   └── ForbiddenPage.tsx
│   ├── providers/             # React context providers
│   │   ├── QueryProvider.tsx
│   │   ├── ThemeProvider.tsx
│   │   ├── NotificationProvider.tsx  # Sonner Toaster
│   │   └── index.ts
│   ├── routes/                # Route guards
│   │   ├── ProtectedRoute.tsx # Requires authenticated user
│   │   ├── PublicRoute.tsx    # Redirects authenticated users away
│   │   ├── RoleRoute.tsx      # Requires minimum role (hierarchy-aware)
│   │   └── index.ts
│   ├── shared/
│   │   ├── forms/             # FormField, FormSection, validation-messages
│   │   ├── guards/            # RoleGuard (UI-level conditional render)
│   │   ├── hooks/             # 18 shared hooks (pagination, permissions, etc.)
│   │   └── ui/                # 35 internal design-system components
│   ├── stores/                # Zustand global stores
│   │   ├── auth.store.ts
│   │   ├── theme.store.ts
│   │   └── ui.store.ts
│   ├── types/                 # TypeScript types and interfaces
│   │   ├── api.ts             # ProblemDetail, ApiError, Page<T>, PageParams
│   │   ├── auth.ts            # Role, User, AuthTokens
│   │   └── index.ts
│   ├── utils/                 # Pure utility functions
│   │   ├── cn.ts              # clsx + tailwind-merge
│   │   ├── format.ts          # formatCTC, formatDate, titleCase, etc.
│   │   ├── async.ts           # sleep, withRetry
│   │   ├── object.ts          # omitNullish, pick, omit
│   │   ├── array.ts           # groupBy, uniqueBy, sortBy, chunk
│   │   ├── url.ts             # buildUrl, fileExtension, fileDownloadUrl
│   │   └── index.ts
│   ├── vite-env.d.ts          # Vite / import.meta.env ambient types
│   ├── index.css              # Tailwind v4 entry + design tokens (@theme block)
│   ├── main.tsx               # React 19 entry point
│   └── App.tsx                # Root component: provider tree + router + routes
├── .env.development           # Dev environment variables
├── .env.example               # Template for contributors
├── .env.production            # Prod overrides (no secrets)
├── .prettierrc
├── .prettierignore
├── components.json            # shadcn/ui registry config
├── eslint.config.js           # ESLint 9 flat config (app + node segments)
├── index.html                 # Vite HTML entry
├── package.json
├── tsconfig.json              # Project references root
├── tsconfig.app.json          # App compilation (src/**)
├── tsconfig.node.json         # Tooling compilation (*.config.ts, scripts/**)
└── vite.config.ts
```

---

## Feature Module Structure (Phase 3+)

Each feature follows this layout:

```
features/<name>/
├── api/           # TanStack Query hooks + typed API calls for this feature
├── components/    # Feature-scoped UI components
├── hooks/         # Feature-scoped custom hooks
├── pages/         # Route-level pages for this feature
└── index.ts       # Public API barrel (explicit exports only)
```

**Dependency rule:** feature modules may import from `shared/`, `lib/`, `stores/`, `config/`, `constants/`, `utils/`, and `types/`. They must **not** import from other feature modules directly — cross-feature communication uses shared stores.

---

## Shared Module Rules

| Directory | Who may import it | Who may NOT import it |
|---|---|---|
| `shared/ui` | Anyone | — |
| `shared/hooks` | Anyone | — |
| `shared/forms` | Anyone | — |
| `shared/guards` | Feature modules, layouts | `shared/ui`, `lib/` |
| `lib/api` | Feature modules, shared hooks | — |
| `stores/` | Feature modules, shared hooks, providers | `lib/api` (avoid circular) |

`shared/` modules must **never** import from `features/` — dependencies flow inward only.

---

## Path Aliases

All `@/<name>/*` aliases resolve to `src/<name>/*`. Configured identically in `tsconfig.app.json` (TypeScript) and `vite.config.ts` (Vite bundler).

| Alias | Resolves to |
|---|---|
| `@/*` | `src/*` |
| `@features/*` | `src/features/*` |
| `@layouts/*` | `src/layouts/*` |
| `@shared/*` | `src/shared/*` |
| `@stores/*` | `src/stores/*` |
| `@lib/*` | `src/lib/*` |
| `@types/*` | `src/types/*` |
| `@utils/*` | `src/utils/*` |
| `@constants/*` | `src/constants/*` |
| `@config/*` | `src/config/*` |
| `@pages/*` | `src/pages/*` |
| `@routes/*` | `src/routes/*` |
| `@providers/*` | `src/providers/*` |
