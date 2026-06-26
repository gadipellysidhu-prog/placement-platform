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
**Effort estimate:** 4–6 days  
**Complexity:** Medium  
**Risk:** Low — all backend auth endpoints are verified and working.

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
- [ ] `npm run test` passes (≥ 80% coverage on auth module)
- [ ] Login flow works end-to-end with real backend
- [ ] Register flow works end-to-end (STUDENT role only exposed)
- [ ] Logout clears state and redirects to /login
- [ ] Token refresh works silently on 401 (interceptor test)
- [ ] Unauthenticated user redirected to /login from protected route
- [ ] Wrong-role user redirected to /403
- [ ] Session survives page refresh (rehydration from refresh token)
- [ ] AuthLayout and DashboardLayout render correctly at 375px, 768px, 1024px, 1440px
- [ ] No console errors or unhandled promise rejections in browser

### Risks

- **Refresh token race condition:** Multiple concurrent 401 responses can trigger multiple refresh calls. Mitigate with a refresh-in-progress flag and a queue of waiting requests in the Axios interceptor.
- **XSS:** Access token stays in Zustand memory only. Refresh token in `localStorage` is acceptable given short access-token lifetime (15 min). Never store access token in localStorage.

### Performance Targets

- Initial bundle: < 200KB gzipped (auth + layout only, no domain code loaded)
- LCP on `/login`: < 1.5s on 3G simulation

### Dependencies

- Backend: `/auth/*` endpoints (all working)
- No other backend modules required

---

## Phase 2: Core Domain — Students and Companies

**Status:** Pending Phase 1 approval  
**Effort estimate:** 6–8 days  
**Complexity:** Medium-High — many page variants, officer vs. student views differ significantly.

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

### Previously Blocked Features (now unblocked — Phase 0.75)

- Branch selector — `GET /api/branches` is now implemented; wire up branch dropdown in student create/update forms
- Skill selector/assignment — `GET /api/skills` is now implemented; wire up skill selector and `POST /api/students/{id}/skills/{skillId}`

### Exit Criteria

- [ ] All quality gates pass (build, lint, typecheck, test ≥ 80%)
- [ ] Officer can list, search (client-side), view, create, and update students
- [ ] Student can view own profile (`GET /api/students/me`)
- [ ] Companies list and detail work for both roles
- [ ] Officer can create, activate, deactivate companies
- [ ] DataTable pagination works correctly (page/size params passed to API)
- [ ] Blocked features (branch selector, skill selector) show informative "coming soon" banners — no broken UI
- [ ] No N+1 fetch patterns (use TanStack Query caching correctly)

### Risks

- **Branch/Skill blockers:** `POST /api/students` requires `branchId` but no Branch API exists. Workaround: make `branchId` optional in the creation form with a note; officers can assign branch later once API is available.
- **Unbounded list responses:** `GET /api/applications/my` and `GET /api/offers/my` return unbounded lists. Frontend must gracefully handle large arrays without crashing.

### Dependencies

- Phase 1 complete
- Backend: `GET /api/students`, `POST /api/students`, `GET /api/companies`, etc.

---

## Phase 3: Job Postings and Applications

**Status:** Pending Phase 2 approval  
**Effort estimate:** 5–7 days  
**Complexity:** High — status state machine, multi-step workflow, dual officer/student views.

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
- [ ] Student can browse OPEN postings, apply, and see their application in the list
- [ ] Student cannot apply twice (duplicate blocked by backend 409 — show toast)
- [ ] Student can withdraw from APPLIED/SHORTLISTED applications
- [ ] Officer can create DRAFT, open, close, and cancel postings
- [ ] Officer can view applications and move them through all status stages
- [ ] `ApplicationStatusStepper` shows correct current step
- [ ] 409 Conflict errors surfaced to user correctly (not silently swallowed)

### Risks

- **Missing officer job postings endpoint (HIGH-4):** Officers cannot fetch DRAFT/CLOSED postings — `GET /api/job-postings` returns OPEN only. Workaround: use the same student endpoint for the officer list, acknowledging it shows only OPEN. Design the officer UI to show a warning banner until the backend gap is resolved. Document as a known limitation.
- **Status machine enforcement:** The frontend should enforce only valid transitions (e.g., disable buttons for invalid next states) to prevent wasted 409 round-trips.

### Dependencies

- Phase 2 complete
- Backend: Job posting and application endpoints

---

## Phase 4: Offers, Certificates, and File Upload

**Status:** Pending Phase 3 approval  
**Effort estimate:** 5–6 days  
**Complexity:** High — file upload with async virus scan state, multi-step certificate flow.

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
- [ ] File upload works: progress indicator, scan status badge (CLEAN/PENDING/INFECTED), error on > 10MB
- [ ] Infected file upload is rejected with user-visible error (`422` → "File is infected")
- [ ] Student can accept or reject PENDING offers with confirmation dialog
- [ ] Student can submit certificate form with an uploaded file's UUID as `fileKey`
- [ ] `fileKey` UUID correctly maps to `/api/files/{fileKey}` download URL in the UI
- [ ] Officer can verify and reject certificates (queue empties as items are actioned)
- [ ] Skill selector in certificate form disabled with "coming soon" when no Skill API

### Risks

- **Virus scan is async:** After upload, scan status may be `PENDING`. The frontend must handle this state — show a spinner/badge, and re-poll (or re-fetch on page revisit). Do not allow submitting a certificate with a `PENDING` scan status.
- **Skill API still missing:** Certificate form field `skillId` cannot be populated. Make it optional in UI with a banner explaining the limitation.

### Dependencies

- Phase 3 complete
- Backend: Offers, certificates, file pipeline endpoints

---

## Phase 5: Officer Dashboard and Admin Views

**Status:** Pending Phase 4 approval  
**Effort estimate:** 3–4 days  
**Complexity:** Low-Medium — mostly data aggregation from existing APIs.

**Objective:** Complete the officer dashboard with real data and implement admin-specific capabilities.

### Deliverables

1. **Officer dashboard** — populated stat cards using `GET /api/dashboard/summary` (✅ real endpoint available since Phase 0.75)
   - Single request returns: `totalStudents`, `placedStudents`, `activeCompanies`, `openJobPostings`, `pendingApplications`, `pendingCertificates`, `placementRatePercent`
2. **Previously blocked pages (now implementable):**
   - Skills management page — wire to `GET/POST/PUT /api/skills`, `POST /api/skills/{id}/verify`
   - Branches management page — wire to `GET/POST/PUT /api/branches`, activate/deactivate
3. **Still-blocked pages (show banner):**
   - Audit log page — "Audit API coming soon"
3. **Admin capabilities:**
   - Company blacklist button (visible only to ROLE_ADMIN)
   - User creation (document that register endpoint accepts all roles — add UI warning)

### Exit Criteria

- [ ] All quality gates pass
- [ ] Officer dashboard shows real counts from `GET /api/dashboard/summary`
- [ ] Dashboard renders correctly when the summary API call fails (graceful error state)
- [ ] Admin-only buttons (blacklist, user creation) render only when `role === 'ROLE_ADMIN'`
- [ ] Skills and Branches management pages are fully wired to their REST APIs (no longer blocked)
- [ ] Audit log page shows "coming soon" banner
- [ ] Notification bell renders with count "0" (placeholder)

### Risks

- **Analytics API (CRITICAL-3 — resolved):** `GET /api/dashboard/summary` now exists. Use it directly; no fallback needed.

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
