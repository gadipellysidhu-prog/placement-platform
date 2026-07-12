# Placement Platform — Frontend

React 19 + TypeScript + Vite 6 single-page app for the Placement Intelligence &
Skill Verification Platform. Talks to the Spring Boot backend (`../backend`) over a
typed axios client with silent JWT refresh, TanStack Query for server state, and
Zustand for auth/UI/theme state.

## Tech stack

| Concern      | Choice                                                      |
| ------------ | ----------------------------------------------------------- |
| Framework    | React 19, TypeScript 5.5 (strict)                           |
| Build/dev    | Vite 6, `@vitejs/plugin-react`, Tailwind CSS 4              |
| Routing      | React Router 7                                              |
| Server state | TanStack Query 5                                            |
| Client state | Zustand 5 (`persist` middleware)                            |
| Forms        | React Hook Form + Zod                                       |
| HTTP         | axios (single client with request/response interceptors)    |
| UI           | Radix primitives + local `shared/ui` components             |
| Testing      | Vitest 4, React Testing Library, `user-event`, MSW 2, jsdom |

## Prerequisites

- Node 20+ and npm 10+ (CI pins Node 20).
- The backend running on `http://localhost:8081` for live use (see `../backend`).

## Installation

```bash
cd frontend
npm ci            # clean, lockfile-pinned install (preferred)
# or: npm install
cp .env.example .env.local   # then adjust VITE_API_BASE_URL if needed
```

Environment variables are validated at startup by [`src/config/env.ts`](src/config/env.ts)
(Zod). A missing/malformed value throws immediately instead of failing silently.

## Everyday commands

| Command                           | What it does                                                         |
| --------------------------------- | -------------------------------------------------------------------- |
| `npm run dev`                     | Start Vite dev server on `:5173` (proxies `/api`, `/auth` → `:8081`) |
| `npm run build`                   | Type-check (`tsc -b`) then produce a production build                |
| `npm run preview`                 | Serve the production build locally                                   |
| `npm run lint`                    | ESLint over `src` + config files                                     |
| `npm run format` / `format:check` | Prettier write / verify                                              |
| `npm run typecheck`               | `tsc -b` with no emit                                                |
| `npm run test`                    | Run the Vitest suite once (CI mode)                                  |
| `npm run test:watch`              | Vitest in watch mode                                                 |
| `npm run test:coverage`           | Run with V8 coverage report (`coverage/`)                            |

## Testing

### Philosophy

Tests are **behavioral**, not implementation-coupled. We assert what a user or a
caller observes — rendered text, navigation, storage side effects, resolved/rejected
promises — never internal component state or snapshots. There are **no snapshot tests**.

The real axios client and its interceptors are always exercised: **we never mock
axios**. Network boundaries are stubbed with [MSW](https://mswjs.io/) so the request
interceptor (Bearer attach), the response interceptor (401 → silent refresh → retry),
and the auth store all run exactly as in production.

### Stack & lifecycle

- **Vitest** (`vitest.config.ts`) runs in `jsdom` with `globals: true`. It reuses the
  app's path aliases directly from `vite.config.ts` — aliases live in exactly one place.
- **MSW** server is created once in [`src/test/msw/server.ts`](src/test/msw/server.ts)
  and wired for the whole run in [`src/test/setup.ts`](src/test/setup.ts):
  `listen({ onUnhandledRequest: 'error' })` → `resetHandlers()` + storage reset after
  each test → `close()` at the end. An unhandled request **fails** the test.

### `renderWithProviders`

Every component/integration test renders through
[`src/test/render.tsx`](src/test/render.tsx), which mirrors the real provider tree in
`App.tsx` (Query → Theme → Tooltip → Notifications → Router), swapping `BrowserRouter`
for `MemoryRouter` so tests drive navigation. Each render gets a **fresh** `QueryClient`
(retries off) so cache never leaks between tests.

```tsx
import { renderWithProviders, screen } from '@/test'

const { user } = renderWithProviders(<LoginPage />, { initialEntries: ['/login'] })
await user.type(screen.getByLabelText(/email address/i), 'me@uni.edu')
```

`@/test` is the single import surface — it re-exports Testing Library, `userEvent`,
`renderWithProviders`, the MSW `server`, and shared fixtures.

### Overriding requests per test

Default happy-path handlers live in
[`src/test/msw/handlers.ts`](src/test/msw/handlers.ts). Override them inline to
exercise failures — the override is reverted automatically after the test:

```ts
import { http, HttpResponse } from 'msw'
import { server, API_BASE_URL } from '@/test'

server.use(
  http.post(`${API_BASE_URL}/auth/login`, () =>
    HttpResponse.json({ title: 'Unauthorized', status: 401, detail: 'Bad creds' }, { status: 401 }),
  ),
)
```

### Writing a new test

1. Name it `*.test.ts(x)` next to the code under test.
2. Render via `renderWithProviders`; query by role/label/text, not test IDs.
3. Stub the network with MSW; never mock axios or fetch.
4. `await` user interactions (`user-event`) and use `findBy*` / `waitFor` for async UI.
5. Assert observable outcomes (DOM, navigation, storage), then let the shared
   `afterEach` clean up.

Current coverage: axios interceptors & refresh flow, auth store, route guards
(`ProtectedRoute` / `PublicRoute` / `RoleRoute`), and the login page.

## CI

The frontend has its own **independent** quality gate in
`../.github/workflows/ci.yml` (`Stage 9 · Frontend Quality`) — it never blocks, and is
never blocked by, the backend stages. On every push/PR it runs, from `frontend/`:
`npm ci` → `lint` → `format:check` → `typecheck` → `test` → `build`. Any failure fails
the PR. Reproduce the gate locally with:

```bash
npm ci && npm run lint && npm run format:check && npm run typecheck && npm run test && npm run build
```

## Project layout

```
src/
├── config/       env parsing & validation
├── constants/    routes, roles
├── features/     vertical slices (auth, students, companies, dashboard)
├── layouts/      shell layouts (auth, dashboard, minimal)
├── lib/          axios client, query client, typed API modules
├── providers/    Query/Theme/Notification providers
├── routes/       route guards
├── shared/       ui primitives, forms, hooks, guards
├── stores/       Zustand stores (auth, theme, ui)
├── test/         shared test infra (render, MSW, setup)  ← import via `@/test`
├── types/        shared TypeScript types
└── utils/        pure helpers
```
