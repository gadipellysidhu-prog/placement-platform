# FRONTEND_ROADMAP.md
## Frontend Development Roadmap — Placement Intelligence & Skill Verification Platform

**Date:** 2026-06-26
**Total Estimated Duration:** 8–10 weeks (solo developer, AI-assisted)

---

## Milestone Overview

| Milestone | Focus | Duration | Dependencies |
|---|---|---|---|
| M0 | Phase 0 Planning & Discovery | Complete | — |
| M1 | Foundation + Auth | 1 week | Backend auth APIs |
| M2 | Student Domain | 1.5 weeks | Student, Company APIs |
| M3 | Job Postings + Applications | 1.5 weeks | Job posting, application APIs |
| M4 | Offers + Certificates | 1 week | Offers, certificate, file APIs |
| M5 | Officer Dashboard + Admin Views | 1.5 weeks | All domain APIs |
| M6 | Quality, Accessibility, Performance | 1 week | M1–M5 complete |
| M7 | Production Readiness | 0.5 weeks | M6 complete |

---

## Milestone 0: Phase 0 — Planning and Discovery (COMPLETE)

**Status:** Complete

**Deliverables:**
- [x] Full repository audit (PROJECT_AUDIT.md)
- [x] API contract documentation (API_CONTRACT.md)
- [x] Backend compatibility analysis (BACKEND_COMPATIBILITY.md)
- [x] Frontend master plan (FRONTEND_MASTER_PLAN.md)
- [x] Design review (DESIGN_REVIEW.md)
- [x] Feature matrix (FEATURE_MATRIX.md)
- [x] Module dependency map (MODULE_DEPENDENCY_MAP.md)
- [x] Implementation phases (IMPLEMENTATION_PHASES.md)
- [x] Roadmap (this document)

---

## Milestone 1: Foundation + Auth (Week 1)

**Goal:** A running React app with authentication, routing, layout, and shared infrastructure.

### Feature List

| Feature | Priority | Complexity | Backend Dependency |
|---|---|---|---|
| Vite + React 19 + TypeScript scaffold | P0 | Low | None |
| Tailwind + shadcn/ui setup | P0 | Low | None |
| Axios instance + interceptors | P0 | Medium | `/auth/*` |
| TanStack Query client setup | P0 | Low | None |
| Zustand auth store | P0 | Low | None |
| Route structure + React Router v6 | P0 | Medium | None |
| AuthLayout + DashboardLayout | P0 | Medium | None |
| ProtectedRoute + RoleRoute guards | P0 | Medium | None |
| Login page (email + password) | P0 | Medium | `POST /auth/login` |
| Register page (email + password + role) | P0 | Medium | `POST /auth/register` |
| Logout flow | P0 | Low | `POST /auth/logout` |
| Token refresh (silent, on 401) | P0 | High | `POST /auth/refresh` |
| Session rehydration on page load | P0 | Medium | `GET /api/users/me` |
| Forgot password page (stub) | P1 | Low | `POST /auth/forgot-password` (stub) |
| 403 / 404 error pages | P1 | Low | None |

**Exit Criteria:**
- User can register, login, and logout
- Token refresh happens transparently on 401
- Authenticated routes redirect unauthenticated users to /login
- Role routes redirect unauthorized users to /403
- All quality gates pass

---

## Milestone 2: Student Domain (Week 2–3)

**Goal:** Complete student self-service experience + officer student management.

### Feature List

| Feature | Priority | Complexity | Backend Dependency |
|---|---|---|---|
| Student dashboard (own stats) | P0 | Medium | `GET /api/applications/my`, `GET /api/offers/my` |
| Student profile page (view own) | P0 | Low | `GET /api/students/me` |
| Companies list (browse) | P0 | Low | `GET /api/companies` |
| Company detail page | P0 | Low | `GET /api/companies/{id}` |
| Officer: Students list (paginated + search) | P0 | Medium | `GET /api/students` |
| Officer: Student detail view | P0 | Medium | `GET /api/students/{id}` |
| Officer: Create student profile | P1 | Medium | `POST /api/students` |
| Officer: Update student profile | P1 | Medium | `PUT /api/students/{id}` |
| Officer: Update student status | P1 | Low | `PUT /api/students/{id}/status` |
| Officer: Evaluate eligibility | P1 | Low | `PUT /api/students/{id}/eligibility` |
| Officer: Companies list + management | P1 | Medium | `GET /api/companies`, `POST`, `PUT` |
| Officer: Company activate/deactivate | P1 | Low | `POST /api/companies/{id}/activate` |
| Admin: Company blacklist | P2 | Low | `POST /api/companies/{id}/blacklist` |
| Skill assignment | P1 | Low | `POST /api/students/{id}/skills/{skillId}` + `GET /api/skills` (✅ unblocked Phase 0.75) |
| Branch selector | P1 | Low | `GET /api/branches` + `branchId` in student form (✅ unblocked Phase 0.75) |

**Exit Criteria:**
- Student can view own profile and dashboard
- Officer can list, view, create, and manage students
- Officer can manage companies
- Blocked features show appropriate banners

---

## Milestone 3: Job Postings + Applications (Week 3–4)

**Goal:** Complete job posting lifecycle and student application flow.

### Feature List

| Feature | Priority | Complexity | Backend Dependency |
|---|---|---|---|
| Job postings browse (student) | P0 | Low | `GET /api/job-postings` |
| Job posting detail + apply button | P0 | Medium | `GET /api/job-postings/{id}`, `POST /api/applications` |
| My applications list | P0 | Low | `GET /api/applications/my` |
| Withdraw application | P0 | Low | `POST /api/applications/{id}/withdraw` |
| Officer: All job postings (all statuses) | P0 | Medium | `GET /api/job-postings` + filter |
| Officer: Create job posting | P0 | Medium | `POST /api/job-postings` |
| Officer: Manage job posting lifecycle | P0 | Medium | `/open`, `/close`, `/cancel` |
| Officer: Applications list | P0 | Medium | `GET /api/applications` |
| Officer: Application detail + status update | P0 | Medium | `PUT /api/applications/{id}/status` |
| Officer: Applications by student | P1 | Low | `GET /api/applications/student/{studentId}` |

**Exit Criteria:**
- Student can browse open jobs, apply, and withdraw
- Officer can manage full job posting lifecycle
- Officer can view and update application statuses

---

## Milestone 4: Offers + Certificates + File Upload (Week 5)

**Goal:** Complete offer flow and certificate submission/verification.

### Feature List

| Feature | Priority | Complexity | Backend Dependency |
|---|---|---|---|
| My offers list (student) | P0 | Low | `GET /api/offers/my` |
| Accept/reject offer | P0 | Low | `POST /api/offers/{id}/accept`, `/reject` |
| Officer: Create offer from application | P0 | Medium | `POST /api/offers` |
| Officer: Offers list + expire | P0 | Medium | `GET /api/offers`, `POST /api/offers/{id}/expire` |
| File upload widget (PDF/image) | P0 | High | `POST /api/files/upload` |
| Certificate submission form | P0 | High | `POST /api/certificates` + file upload |
| My certificates list | P0 | Low | `GET /api/certificates/my` |
| Certificate file download | P1 | Medium | `GET /api/files/{id}` |
| Officer: Certificates queue | P0 | Medium | `GET /api/certificates` |
| Officer: Verify/reject certificate | P0 | Low | `POST /api/certificates/{id}/verify`, `/reject` |

**Exit Criteria:**
- Student can view, accept, and reject offers
- File upload works with progress indicator and virus scan status
- Student can submit certificates with file attachment
- Officer can verify and reject certificates

---

## Milestone 5: Officer Dashboard + Admin Views (Week 6)

**Goal:** Complete officer dashboard with real metrics and admin capabilities.

### Feature List

| Feature | Priority | Complexity | Backend Dependency |
|---|---|---|---|
| Officer dashboard stats | P0 | Low | `GET /api/dashboard/summary` (✅ real endpoint, unblocked Phase 0.75) |
| Skills management page | P1 | Medium | `GET/POST/PUT /api/skills`, `POST /api/skills/{id}/verify` (✅ unblocked Phase 0.75) |
| Branches management page | P1 | Low | `GET/POST/PUT /api/branches` (✅ unblocked Phase 0.75) |
| Admin: User management page | P2 | Low | `POST /auth/register` (workaround) |
| Admin: Blacklist companies | P2 | Low | `POST /api/companies/{id}/blacklist` |
| Audit log page (blocked) | BLOCKED | — | Needs Audit API |
| Notification bell (placeholder 0 count) | P2 | Low | Needs Notification API |

**Exit Criteria:**
- Officer dashboard shows real data from available APIs
- Blocked features show clear banners with expected timeline
- Admin-only features are role-gated

---

## Milestone 6: Quality, Accessibility, Performance (Week 7)

**Goal:** Bring all pages to production quality standard.

### Tasks

| Task | Priority |
|---|---|
| Axe accessibility audit on all pages | P0 |
| Keyboard navigation verification | P0 |
| ARIA improvements | P0 |
| Responsive verification (375, 768, 1024, 1440px) | P0 |
| Bundle size analysis (< 500KB gzipped) | P0 |
| Route-based code splitting verification | P0 |
| React.memo, useMemo optimizations | P1 |
| Error boundary coverage audit | P0 |
| Empty state coverage (all list pages) | P0 |
| Loading skeleton coverage (all data pages) | P0 |
| Unit test coverage >= 80% | P0 |
| Component test coverage for critical flows | P0 |

**Exit Criteria:**
- Lighthouse scores: Performance >= 90, Accessibility >= 95, Best Practices >= 95
- Zero axe violations
- All quality gates pass

---

## Milestone 7: Production Readiness (Week 8)

**Goal:** Frontend ready for deployment alongside production backend.

### Tasks

| Task | Priority |
|---|---|
| `.env.production` config | P0 |
| Vite production build verification | P0 |
| nginx config for SPA (try_files $uri /index.html) | P0 |
| CORS origin verification against backend config | P0 |
| CSP header compatibility check | P0 |
| Error logging setup (console → production monitoring) | P1 |
| Documentation updates (README.md with frontend setup) | P0 |

**Exit Criteria:**
- `npm run build` produces optimized production bundle
- Frontend deploys and connects to production backend
- All end-to-end flows work on production URL

---

## Feature Prioritization Matrix

| Priority | Meaning |
|---|---|
| P0 | Must have — core platform functionality |
| P1 | Should have — important officer/admin operations |
| P2 | Nice to have — enhancements |
| BLOCKED | Waiting on backend endpoint (documented in BACKEND_COMPATIBILITY.md) |

---

---

## Review Checkpoints

Each milestone ends with a mandatory review before the next begins:

| Checkpoint | Criteria |
|---|---|
| After M1 | Auth flows tested E2E against real backend; token refresh verified; layouts reviewed at all breakpoints |
| After M2 | Student and company CRUD flows working; blocked features showing correct banners |
| After M3 | Full job posting lifecycle and application status workflow verified; 409 handling correct |
| After M4 | File upload + scan status; offer accept/reject; certificate submission — all E2E tested |
| After M5 | Dashboard metrics accurate; role-gated actions verified |
| After M6 | Lighthouse ≥ 90 Performance, ≥ 95 Accessibility; zero axe violations; bundle < 500KB gzip |
| After M7 | Production build connects to live backend; CORS verified; no console errors |

---

## Blocked Feature Strategy

Features blocked by missing backend endpoints follow a consistent pattern:

1. **Build the page shell** — route, navigation entry, page component — but render a `<BlockedFeatureBanner>` in the content area.
2. **Banner content:** "This feature requires [Skill/Branch/Analytics] API (see BACKEND_COMPATIBILITY.md). Expected: when backend adds the controller."
3. **Do not disable routes.** The page must be reachable so officers see the expected navigation structure.
4. **Forms that require blocked data** (e.g., branch selector in student create form) — make the field optional and note it with inline hint text.
5. **When the backend gap is resolved:** Remove the banner, wire up the real API. No page restructuring should be required because the shell already exists.

| Feature | Status | Notes |
|---|---|---|
| Branch selector | ✅ **Unblocked** (Phase 0.75) | `GET /api/branches` — wire up branch dropdown in M2 |
| Skill selector | ✅ **Unblocked** (Phase 0.75) | `GET /api/skills` — wire up skill selector in M2/M4 |
| Dashboard metrics | ✅ **Unblocked** (Phase 0.75) | `GET /api/dashboard/summary` — use in M5 |
| Forgot password | ❌ Blocked | Backend email stub (HIGH-5); M1 stub page only |
| Officer all-status job postings | ❌ Blocked | HIGH-4 still missing; M3 shows OPEN only with banner |
| Notifications | ❌ Blocked | No Notification API; bell with "0" placeholder |
| Audit log | ❌ Blocked | No Audit log REST API |

---

## Risk Register

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| ~~Skills/Branches API not added before M2~~ | — | — | **Resolved in Phase 0.75** — Branch and Skill REST APIs implemented |
| ~~Analytics API not added before M5~~ | — | — | **Resolved in Phase 0.75** — `GET /api/dashboard/summary` implemented |
| Password reset/email verification never implemented | Medium | Low | Stub page with generic "check your inbox" message always |
| ~~Backend role restriction on register not fixed~~ | — | — | **Resolved in Phase 0.75** — register now rejects non-STUDENT roles with 400 |
| Token storage XSS risk | Low | High | Access token in Zustand memory only; refresh token in localStorage (acceptable tradeoff) |
| Refresh token race condition (concurrent 401s) | Medium | High | Implement queue-based refresh: one in-flight refresh, other requests wait and retry |
| File scan async state | Medium | Medium | Poll or block certificate submission until scan status is CLEAN |
