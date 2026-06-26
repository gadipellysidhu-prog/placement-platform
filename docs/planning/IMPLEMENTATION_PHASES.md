# IMPLEMENTATION_PHASES.md
## Frontend Implementation Phases — Placement Intelligence & Skill Verification Platform

**Date:** 2026-06-26
**Governance:** FRONTEND_CONSTITUTION.md + docs/CLAUDE.md

---

## Phase 0: Planning and Discovery

**Status:** COMPLETE

**Objective:** Establish verified knowledge of the backend, define all planning documents, and receive explicit approval before writing a single line of production code.

**Deliverables:**
- PROJECT_AUDIT.md
- API_CONTRACT.md
- BACKEND_COMPATIBILITY.md
- FRONTEND_MASTER_PLAN.md
- FRONTEND_ROADMAP.md
- IMPLEMENTATION_PHASES.md (this document)
- FEATURE_MATRIX.md
- MODULE_DEPENDENCY_MAP.md
- DESIGN_REVIEW.md

**Exit Criteria:**
- All planning documents reviewed and approved
- Backend gaps documented and classified
- No implementation code written
- Explicit approval to proceed to Phase 1 received

**Dependencies:** None

---

## Phase 1: Foundation and Authentication

**Status:** Pending approval to start

**Objective:** Create a runnable React application with complete authentication, routing infrastructure, shared layouts, and base design system.

### Deliverables

1. **Project scaffold**
   - Vite + React 19 + TypeScript (strict mode)
   - Tailwind CSS configured with design tokens
   - shadcn/ui initialized
   - ESLint + Prettier configured
   - Vitest + React Testing Library configured

2. **Infrastructure layer**
   - `src/lib/axios.ts` — Axios instance with base URL from `VITE_API_BASE_URL`
   - `src/lib/query-client.ts` — TanStack Query client
   - Request interceptor: attach `Authorization: Bearer` from Zustand store
   - Response interceptor: 401 → refresh → retry; 401 on refresh → logout + redirect

3. **Auth store (Zustand)**
   - `src/stores/auth.store.ts`
   - Fields: `accessToken`, `user: { email, role }`, `isAuthenticated`
   - Persisted: refresh token in `localStorage`

4. **Auth API layer**
   - `src/features/auth/api/auth.api.ts`
   - Functions: `login()`, `register()`, `refresh()`, `logout()`, `getMe()`
   - Zod schemas for all DTOs

5. **Routing**
   - `src/constants/routes.ts` — all route constants
   - `src/App.tsx` — React Router v6 with nested routes
   - `ProtectedRoute` — checks `isAuthenticated`, redirects to `/login`
   - `RoleRoute` — checks role, redirects to `/403`
   - Lazy loading for all route components

6. **Layouts**
   - `AuthLayout` — centered card, brand logo, responsive
   - `DashboardLayout` — sidebar + header + content + mobile drawer

7. **Auth pages**
   - `LoginPage` — email + password + submit + error display
   - `RegisterPage` — email + password + role dropdown (STUDENT only visible) + submit
   - `ForgotPasswordPage` — email field + submit + "If registered, you will receive an email" message
   - `403Page`, `404Page`

8. **Session management**
   - On app load: check `localStorage` for refresh token, call `/auth/refresh`, populate store
   - Logout: call `/auth/logout`, clear store + localStorage, redirect to `/login`

### API Endpoints Used

| Endpoint | Purpose |
|---|---|
| `POST /auth/login` | Login |
| `POST /auth/register` | Register |
| `POST /auth/refresh` | Token refresh |
| `POST /auth/logout` | Logout |
| `GET /api/users/me` | Session rehydration |

### Exit Criteria

- [ ] `npm run build` succeeds (zero TypeScript errors)
- [ ] `npm run lint` passes
- [ ] `npm run test` passes
- [ ] Login flow works end-to-end with real backend
- [ ] Register flow works end-to-end
- [ ] Logout clears state and redirects
- [ ] Token refresh works silently on 401
- [ ] Unauthenticated user redirected to /login from protected route
- [ ] Wrong-role user redirected to /403
- [ ] Session survives page refresh (rehydration from refresh token)
- [ ] AuthLayout and DashboardLayout render correctly at all breakpoints

### Dependencies

- Backend: `/auth/*` endpoints (all working)
- No other backend modules required

---

## Phase 2: Core Domain — Students and Companies

**Status:** Pending Phase 1 approval

**Objective:** Implement the student domain (self-service + officer management) and company browsing/management.

### Deliverables

1. **Student API layer** (`src/features/students/api/students.api.ts`)
   - `getStudents(page)`, `getStudentById(id)`, `getMyStudentProfile()`
   - `createStudent(req)`, `updateStudent(id, req)`, `updateStudentStatus(id, status)`
   - `evaluateEligibility(id)`, `assignSkill(id, skillId)`, `removeSkill(id, skillId)`

2. **Company API layer** (`src/features/companies/api/companies.api.ts`)
   - `getCompanies(page)`, `getCompanyById(id)`
   - `createCompany(req)`, `updateCompany(id, req)`
   - `activateCompany(id)`, `deactivateCompany(id)`, `blacklistCompany(id)`

3. **TanStack Query hooks** for all above

4. **Student pages**
   - `StudentProfilePage` — student's own profile (CGPA, branch, year, skills, status)
   - `StudentsListPage` (officer) — paginated table with search
   - `StudentDetailPage` (officer) — full profile + action buttons

5. **Company pages**
   - `CompaniesListPage` — card grid or table, browse
   - `CompanyDetailPage` — details + status management
   - `CreateCompanyPage` (officer) — form

6. **Basic student dashboard**
   - Stat cards: Applications count, Offers count, Certificates count
   - Profile completeness indicator

7. **Officer dashboard skeleton**
   - Stat cards wired to available data (student count, company count)
   - Placeholder cards for analytics (marked as "coming soon")

8. **Shared components**
   - `DataTable` with pagination, sort
   - `StatCard`
   - `StatusBadge` for all enums
   - `EmptyState`, `ErrorState`, `LoadingState`, `Skeleton`

### API Endpoints Used

All student and company endpoints from API_CONTRACT.md.

### Blocked Features (document with banners)

- Branch selector (CRITICAL-1: no Branch API)
- Skill selector/assignment from UI (CRITICAL-2: no Skill API)

### Exit Criteria

- [ ] All quality gates pass
- [ ] Officer can list and manage students
- [ ] Student can view own profile
- [ ] Companies list and detail work for students
- [ ] Officer can create and manage companies
- [ ] Blocked features show informative banners

### Dependencies

- Phase 1 complete
- Backend: `GET /api/students`, `POST /api/students`, `GET /api/companies`, etc.

---

## Phase 3: Job Postings and Applications

**Status:** Pending Phase 2 approval

**Objective:** Implement the complete job posting lifecycle and student application workflow.

### Deliverables

1. **Job Postings API layer + hooks**
2. **Applications API layer + hooks**
3. **Student pages:**
   - `JobPostingsPage` — browse OPEN postings, filter by company name (client-side)
   - `JobPostingDetailPage` — full details + "Apply" button (eligibility-aware)
   - `MyApplicationsPage` — list with status stepper, withdraw button
4. **Officer pages:**
   - `JobPostingsManagePage` — ALL postings, filter by status tabs (DRAFT, OPEN, CLOSED, CANCELLED)
   - `CreateJobPostingPage` — form
   - `JobPostingManageDetailPage` — edit DRAFT, manage lifecycle buttons, application count
   - `ApplicationsListPage` — paginated, filterable by status
   - `ApplicationDetailPage` — status update dropdown, create offer button
5. **`ApplicationStatusStepper` component** — visual stepper showing APPLIED → SHORTLISTED → INTERVIEWED → OFFERED/REJECTED

### Exit Criteria

- [ ] All quality gates pass
- [ ] Student can browse open postings and apply
- [ ] Student can view and withdraw own applications
- [ ] Officer can manage full job posting lifecycle
- [ ] Officer can update application status through all stages

### Dependencies

- Phase 2 complete
- Backend: Job posting and application endpoints

---

## Phase 4: Offers, Certificates, and File Upload

**Status:** Pending Phase 3 approval

**Objective:** Complete offer management and the certificate submission/verification workflow including file uploads.

### Deliverables

1. **Offers API layer + hooks**
2. **Certificates API layer + hooks**
3. **Files API layer + hooks** (`POST /api/files/upload`, `GET /api/files/{id}`)
4. **`FileUploadWidget` component:**
   - Drag-and-drop + click to select
   - MIME type validation (PDF, PNG, JPEG) before upload
   - File size validation (< 10MB) before upload
   - Upload progress indicator
   - ClamAV scan status display (CLEAN / PENDING / INFECTED)
   - UUID `id` returned and stored as `fileKey`
5. **Student pages:**
   - `MyOffersPage` — offer cards with accept/reject buttons + confirmation dialogs
   - `MyCertificatesPage` — list with verification status badges + file download link
   - `SubmitCertificatePage` — name + organization + skill (disabled if no skill API) + file upload
6. **Officer pages:**
   - `OffersListPage` — all offers, expire action
   - Offer creation from `ApplicationDetailPage` (inline form/modal)
   - `CertificatesQueuePage` — pending certificates, verify/reject actions + file preview link

### Exit Criteria

- [ ] All quality gates pass
- [ ] File upload works with scan status feedback
- [ ] Student can accept/reject offers with confirmation dialog
- [ ] Student can submit certificates with file
- [ ] Officer can verify/reject certificates
- [ ] File download works from certificate detail

### Dependencies

- Phase 3 complete
- Backend: Offers, certificates, file pipeline endpoints

---

## Phase 5: Officer Dashboard and Admin Views

**Status:** Pending Phase 4 approval

**Objective:** Complete the officer dashboard with real data and implement admin-specific capabilities.

### Deliverables

1. **Officer dashboard** — populated stat cards using data from existing APIs
   - Total students: count from `GET /api/students` total elements
   - Active companies: count from `GET /api/companies` (filter ACTIVE client-side or via total)
   - Open postings: from `GET /api/job-postings`
   - Pending applications: from `GET /api/applications` (filter APPLIED)
   - Pending certificates: from `GET /api/certificates` (filter PENDING)
2. **Blocked pages with banners:**
   - Skills management page — "Skills API coming soon"
   - Branches management page — "Branches API coming soon"
   - Audit log page — "Audit API coming soon"
3. **Admin capabilities:**
   - Company blacklist button (visible only to ROLE_ADMIN)
   - User creation (document that register endpoint accepts all roles — add UI warning)

### Exit Criteria

- [ ] All quality gates pass
- [ ] Officer dashboard shows real aggregated metrics
- [ ] Admin-only actions are role-gated and visible only to ROLE_ADMIN
- [ ] Blocked features are clearly communicated with expected status

### Dependencies

- Phase 4 complete

---

## Phase 6: Quality, Accessibility, Performance

**Status:** Pending Phase 5 approval

**Objective:** Raise all pages to enterprise production quality.

### Checklist

- [ ] Axe accessibility audit — zero violations on all pages
- [ ] Keyboard navigation — all interactive elements reachable
- [ ] Focus management — correct on modal open/close, route changes
- [ ] Color contrast — all text meets 4.5:1
- [ ] Screen reader test — key flows narrated correctly
- [ ] Responsive test — 375px, 768px, 1024px, 1440px
- [ ] Table horizontal scroll on mobile
- [ ] Sidebar collapse to hamburger on mobile
- [ ] Bundle analysis — < 500KB gzipped total
- [ ] Route splitting — each route in own chunk
- [ ] Core Web Vitals — LCP < 2.5s, FCP < 1.5s
- [ ] Lighthouse scores — Performance >= 90, Accessibility >= 95
- [ ] Test coverage >= 80% for all new code
- [ ] Error boundary at app root and route level
- [ ] Empty states on all list pages
- [ ] Loading skeletons on all data pages

### Dependencies

- Phase 5 complete

---

## Phase 7: Production Readiness

**Status:** Pending Phase 6 approval

**Objective:** Frontend is deployable to production.

### Checklist

- [ ] `.env.production` file with correct backend URL
- [ ] `npm run build` clean production bundle
- [ ] nginx SPA config (try_files $uri $uri/ /index.html)
- [ ] CORS origin matches `CORS_ALLOWED_ORIGINS` in backend `.env.prod`
- [ ] CSP header verified (backend sends `default-src 'self'` — frontend must be compatible)
- [ ] No `console.log` in production build
- [ ] `README.md` updated with frontend setup instructions
- [ ] Frontend section added to `docker-compose.prod.yml` or separate deployment documented

### Dependencies

- Phase 6 complete
- Production backend deployed and accessible

---

## Cross-Phase Dependencies

```
Phase 0 (Planning) ─────────────────────────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 1 (Foundation + Auth) ────────────────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 2 (Students + Companies) ─────────────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 3 (Job Postings + Applications) ──────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 4 (Offers + Certificates + Files) ────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 5 (Dashboard + Admin) ────────────────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 6 (Quality) ──────────────────────────────────────────────────────────────────►
             │
             ▼ [Approval Required]
Phase 7 (Production) ───────────────────────────────────────────────────────────────►
```

**Rule:** Each phase requires explicit approval before the next begins. No phase skipping.
