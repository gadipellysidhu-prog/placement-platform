# FRONTEND_MASTER_PLAN.md
## Frontend Implementation Strategy — Placement Intelligence & Skill Verification Platform

**Date:** 2026-06-26
**Status:** Approved for Phase 1 implementation
**Authority:** FRONTEND_CONSTITUTION.md v1.0.0

---

## 1. Project Overview

The frontend for the Placement Intelligence & Skill Verification Platform is a zero-baseline greenfield React application. The backend is feature-complete and production-deployed. The frontend must integrate with verified backend REST APIs only.

**No frontend code exists.** This plan establishes the complete strategy before implementation begins.

---

## 2. Tech Stack (Mandatory — From FRONTEND_CONSTITUTION.md)

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Framework | React | 19 | UI rendering, lifecycle |
| Language | TypeScript | 5.x (strict) | Type safety |
| Build | Vite | 5.x | Dev server, bundler |
| Styling | Tailwind CSS | 3.x | Utility-first styling |
| Component Library | shadcn/ui | Latest | Accessible, Radix-based components |
| Server State | TanStack Query | v5 | API data fetching, caching |
| Client State | Zustand | v4 | Auth session, UI state |
| Routing | React Router | v6 | Client-side navigation |
| Forms | React Hook Form | v7 | Form management |
| Validation | Zod | v3 | Schema validation |
| HTTP | Axios | v1 | API requests, interceptors |
| Icons | Lucide React | Latest | Icon set |
| Testing | Vitest + React Testing Library | Latest | Unit + component tests |

---

## 3. Project Structure

```
frontend/
├── public/
│   └── favicon.ico
├── src/
│   ├── main.tsx                          # Entry point
│   ├── App.tsx                           # Router setup
│   ├── constants/
│   │   ├── routes.ts                     # Route path constants (no hardcoded strings)
│   │   └── roles.ts                      # Role enum constants
│   ├── lib/
│   │   ├── axios.ts                      # Axios instance + interceptors
│   │   ├── query-client.ts               # TanStack Query client config
│   │   └── utils.ts                      # cn() helper, date formatters
│   ├── types/
│   │   ├── api.ts                        # Zod schemas mirroring backend DTOs
│   │   ├── auth.ts                       # Auth-related types
│   │   └── enums.ts                      # Enum types (ApplicationStatus, etc.)
│   ├── stores/
│   │   ├── auth.store.ts                 # Zustand: user session, token, role
│   │   └── ui.store.ts                   # Zustand: sidebar state, theme
│   ├── hooks/
│   │   ├── useAuth.ts                    # Auth state from store
│   │   ├── usePermissions.ts             # Role-based permission checks
│   │   └── useToast.ts                   # Toast notifications
│   ├── layouts/
│   │   ├── AuthLayout.tsx                # Public pages layout (centered card)
│   │   └── DashboardLayout.tsx           # Authenticated pages (sidebar + header)
│   ├── shared/
│   │   ├── ui/                           # Generic shadcn/ui + custom UI components
│   │   │   ├── Button.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── StatusBadge.tsx           # Enum-to-color badge
│   │   │   ├── DataTable.tsx             # Reusable paginated table
│   │   │   ├── EmptyState.tsx
│   │   │   ├── ErrorState.tsx
│   │   │   ├── LoadingState.tsx
│   │   │   ├── FileUpload.tsx
│   │   │   ├── DatePicker.tsx
│   │   │   ├── PageHeader.tsx
│   │   │   └── StatCard.tsx
│   │   └── business/
│   │       ├── SidebarNav.tsx            # Role-aware navigation
│   │       ├── UserMenu.tsx              # Header user dropdown
│   │       ├── ProtectedRoute.tsx        # Auth guard
│   │       └── RoleRoute.tsx             # Role guard
│   └── features/
│       ├── auth/
│       │   ├── api/auth.api.ts           # Auth API calls
│       │   ├── hooks/useLogin.ts
│       │   ├── hooks/useRegister.ts
│       │   ├── pages/LoginPage.tsx
│       │   ├── pages/RegisterPage.tsx
│       │   └── pages/ForgotPasswordPage.tsx
│       ├── dashboard/
│       │   ├── pages/StudentDashboardPage.tsx
│       │   └── pages/OfficerDashboardPage.tsx
│       ├── students/
│       │   ├── api/students.api.ts
│       │   ├── hooks/useStudents.ts
│       │   ├── components/StudentTable.tsx
│       │   ├── components/StudentProfileCard.tsx
│       │   ├── pages/StudentProfilePage.tsx     # Student /me view
│       │   ├── pages/StudentsListPage.tsx       # Officer view
│       │   └── pages/StudentDetailPage.tsx      # Officer view
│       ├── companies/
│       │   ├── api/companies.api.ts
│       │   ├── hooks/useCompanies.ts
│       │   ├── components/CompanyCard.tsx
│       │   ├── pages/CompaniesListPage.tsx
│       │   ├── pages/CompanyDetailPage.tsx
│       │   └── pages/CreateCompanyPage.tsx
│       ├── job-postings/
│       │   ├── api/job-postings.api.ts
│       │   ├── hooks/useJobPostings.ts
│       │   ├── components/JobPostingCard.tsx
│       │   ├── pages/JobPostingsPage.tsx         # Student browse
│       │   ├── pages/JobPostingDetailPage.tsx    # Student view
│       │   ├── pages/JobPostingsManagePage.tsx   # Officer view
│       │   ├── pages/CreateJobPostingPage.tsx
│       │   └── pages/JobPostingManageDetailPage.tsx
│       ├── applications/
│       │   ├── api/applications.api.ts
│       │   ├── hooks/useApplications.ts
│       │   ├── components/ApplicationStatusStepper.tsx
│       │   ├── pages/MyApplicationsPage.tsx
│       │   ├── pages/ApplicationsListPage.tsx    # Officer
│       │   └── pages/ApplicationDetailPage.tsx   # Officer
│       ├── offers/
│       │   ├── api/offers.api.ts
│       │   ├── hooks/useOffers.ts
│       │   ├── components/OfferCard.tsx
│       │   ├── pages/MyOffersPage.tsx
│       │   └── pages/OffersListPage.tsx          # Officer
│       ├── certificates/
│       │   ├── api/certificates.api.ts
│       │   ├── hooks/useCertificates.ts
│       │   ├── components/CertificateCard.tsx
│       │   ├── pages/MyCertificatesPage.tsx
│       │   ├── pages/SubmitCertificatePage.tsx
│       │   └── pages/CertificatesQueuePage.tsx   # Officer
│       └── files/
│           ├── api/files.api.ts
│           └── hooks/useFileUpload.ts
├── index.html
├── vite.config.ts
├── tailwind.config.ts
├── tsconfig.json
├── package.json
└── .env.example
```

---

## 4. State Management Strategy

Per FRONTEND_CONSTITUTION.md Section 9:

### 4.1 Server State — TanStack Query

Used for ALL API data:
- Query keys structured as arrays: `['students', { page: 0, size: 20 }]`
- `staleTime: 30_000` (30 seconds) as default
- `gcTime: 300_000` (5 minutes) as default
- Invalidate on mutations: `queryClient.invalidateQueries(['students'])`
- Error handling via `onError` callbacks and global error handler

### 4.2 Client State — Zustand

**Auth Store:**
```typescript
interface AuthStore {
  user: { id?: string; email: string; role: string } | null;
  accessToken: string | null;
  setAuth: (user, token) => void;
  clearAuth: () => void;
}
```
Persisted via `zustand/middleware/persist` in `localStorage`.

**UI Store:**
```typescript
interface UIStore {
  sidebarOpen: boolean;
  setSidebarOpen: (open: boolean) => void;
}
```
Not persisted.

### 4.3 Form State — React Hook Form + Zod

Every form uses:
```typescript
const schema = z.object({ email: z.string().email(), ... });
const form = useForm({ resolver: zodResolver(schema) });
```

### 4.4 Local State — useState

Only for ephemeral UI state: modal open/close, hover state, active tab.

---

## 5. Authentication Integration Approach

### 5.1 Token Storage

- **Access token:** In-memory via Zustand store (cleared on page unload)
- **Refresh token:** `localStorage` (with key `rft`) — survives page reload
- **Rationale:** Access tokens in memory prevent XSS token theft; refresh tokens need persistence

### 5.2 Axios Instance

`src/lib/axios.ts`:
- Base URL from `VITE_API_BASE_URL` env var
- Request interceptor: attach `Authorization: Bearer <access_token>` from Zustand store
- Response interceptor:
  - On 401: attempt token refresh via `POST /auth/refresh` using stored refresh token
  - On refresh success: retry original request with new token
  - On refresh failure: call `clearAuth()` + redirect to `/login`
  - On 403: redirect to `/403`

### 5.3 Route Protection

```
<ProtectedRoute> — checks auth store, redirects to /login if unauthenticated
  <RoleRoute roles={['ROLE_PLACEMENT_OFFICER']}> — redirects to /403 if role insufficient
```

### 5.4 Session Rehydration

On app load (`App.tsx`):
1. Check `localStorage` for refresh token
2. If present, call `POST /auth/refresh`
3. On success: update auth store with new access token, fetch `/api/users/me` for user info
4. On failure: clear store, show login page

---

## 6. API Integration Approach

### 6.1 API Layer (`features/<feature>/api/<feature>.api.ts`)

Each domain has an API module that:
- Exports typed async functions
- Uses the shared Axios instance
- Validates responses with Zod schemas

Example:
```typescript
export async function getStudents(params: PageParams): Promise<Page<StudentResponse>> {
  const { data } = await api.get('/api/students', { params });
  return StudentPageSchema.parse(data);
}
```

### 6.2 TanStack Query Hooks (`features/<feature>/hooks/use<Feature>.ts`)

Wraps API functions in `useQuery` / `useMutation`:
```typescript
export function useStudents(params: PageParams) {
  return useQuery({ queryKey: ['students', params], queryFn: () => getStudents(params) });
}
```

### 6.3 Zod Schemas (`src/types/api.ts`)

Mirrors backend DTOs exactly:
```typescript
const StudentResponseSchema = z.object({
  id: z.string().uuid(),
  userId: z.string().uuid(),
  userEmail: z.string().email(),
  rollNumber: z.string(),
  branchId: z.string().uuid().nullable(),
  branchName: z.string().nullable(),
  cgpa: z.number().nullable(),
  currentYear: z.number().int(),
  placementEligible: z.boolean(),
  status: z.enum(['ACTIVE','PLACED','OPTED_OUT','GRADUATED','BLOCKED']),
  skillNames: z.array(z.string()),
  createdAt: z.string(),
  updatedAt: z.string(),
});
```

---

## 7. Error Handling Strategy

### 7.1 Error Boundary

Top-level `<ErrorBoundary>` wraps entire app. Fallback shows "Something went wrong" with reload button.

### 7.2 API Error Handling

Axios response interceptor classifies errors:
- `401` → token refresh or redirect to login
- `403` → redirect to `/403`
- `404` → pass to TanStack Query `isError` state
- `409` → extract `title` from ProblemDetail, show as form-level error via toast
- `422` → extract `title`, show as form-level error
- `5xx` → show generic "Server error" toast

### 7.3 Form Validation

- Zod schema validates before submission
- Backend validation errors (400 with `errors[]` array) are mapped to React Hook Form field errors
- Form-level errors show as `Alert` component above submit button

### 7.4 Data Loading States

Every data-fetching component handles:
- `isLoading` → `<Skeleton>` or `<LoadingState>`
- `isError` → `<ErrorState>` with retry button
- `isEmpty` → `<EmptyState>` with contextual message
- `data` → actual content

---

## 8. Environment Variables

All env vars prefixed with `VITE_` (Vite convention):

```env
# .env.example (frontend)

# API base URL (no trailing slash)
VITE_API_BASE_URL=http://localhost:8081

# App name (shown in page titles)
VITE_APP_NAME="Placement Intelligence Platform"

# Feature flags (set to false to hide blocked features)
VITE_FEATURE_BRANCHES=false
VITE_FEATURE_SKILLS=false
VITE_FEATURE_ANALYTICS=false
VITE_FEATURE_AUDIT_LOGS=false
VITE_FEATURE_NOTIFICATIONS=false
```

---

## 9. Quality Gates

Before each phase is considered complete:
1. `npm run build` — must succeed (zero TypeScript errors)
2. `npm run lint` — must pass (zero ESLint errors)
3. `npm run typecheck` — must pass
4. `npm run test` — all tests pass, coverage >= 80% for new code
5. Accessibility — automated axe scan passes on all new pages
6. Responsive — verified at 375px, 768px, 1024px, 1440px
7. Backend integration — all API calls verified against actual backend
8. Documentation — affected docs updated

---

## 10. Constraints and Decisions

| Decision | Rationale |
|---|---|
| Feature-based folder structure | Mandated by FRONTEND_CONSTITUTION.md Section 6.1 |
| shadcn/ui over Material-UI | Mandated; Radix-based, accessible, Tailwind-compatible |
| Axios over fetch | Mandated; interceptors, request/response transformation |
| React 19 functional components only | Mandated; no class components |
| No Redux | Mandated; Zustand + TanStack Query replace Redux entirely |
| Desktop-first responsive | Mandated; enterprise audience primarily on desktop |
| Strict TypeScript | Mandated; `strict: true` in tsconfig |
| No mock/placeholder APIs | Mandated; only integrate with verified backend endpoints |
