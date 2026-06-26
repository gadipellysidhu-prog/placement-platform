# MODULE_DEPENDENCY_MAP.md
## Module Dependency Map — Placement Intelligence & Skill Verification Platform

**Date:** 2026-06-26
**Source:** Verified from Java source code and entity/import analysis.

---

## 1. Backend Module Dependency Diagram

### 1.1 Business Modules — Internal Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Business Modules                                    │
│                                                                              │
│  ┌──────────┐     ┌───────────────────────────────────────────────────────┐ │
│  │  auth    │◄────│  student  │  company  │  placement │ certificate      │ │
│  │          │     │           │           │            │                  │ │
│  │ AppUser  │     │ Student──►│ Company   │ JobApplica │ Certificate      │ │
│  │ Role     │     │ Branch    │ JobPosting│ tion       │                  │ │
│  │ Refresh  │     │ Skill     │ Recruiter │ Offer      │                  │ │
│  │ Token    │     │           │           │            │                  │ │
│  └──────────┘     └───────────────────────────────────────────────────────┘ │
│                                                                              │
│  Cross-module dependencies (FK relationships):                               │
│                                                                              │
│  auth.AppUser ◄─── student.Student (user_id FK)                             │
│  auth.AppUser ◄─── company.Recruiter (user_id FK)                           │
│  student.Student ◄─── placement.JobApplication (student_id FK)              │
│  student.Student ◄─── certificate.Certificate (student_id FK)               │
│  student.Skill ◄─── certificate.Certificate (skill_id FK optional)          │
│  student.Branch ◄─── company.JobPosting (job_posting_branches M:M)          │
│  student.Skill ◄─── company.JobPosting (job_posting_skills M:M)             │
│  company.JobPosting ◄─── placement.JobApplication (job_posting_id FK)       │
│  placement.JobApplication ◄─── placement.Offer (application_id FK)          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Shared Infrastructure Dependencies

```
┌────────────────────────────────────────────────────────────────────────────────┐
│                         Shared Infrastructure                                   │
│                                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────────────┐ │
│  │   security   │  │  eventbus    │  │   outbox     │  │   filepipeline     │ │
│  │              │  │              │  │              │  │                    │ │
│  │ SecurityConfig│ │ EventPublish │  │ OutboxService│  │ FilePipelineService│ │
│  │ JwtService   │  │ er           │  │ OutboxDispat │  │ ClamAvService      │ │
│  │ JwtAuthFilter│  │ DomainEvent  │  │ cher         │  │ FileStorageService │ │
│  │ CorsConfig   │  │ Events:      │  │ OutboxEvent  │  │ FileValidationSvc  │ │
│  │ RateLimitFilt│  │ - StudentCre │  │              │  │ FileHashService    │ │
│  └──────────────┘  │ - CompanyCre │  └──────────────┘  └────────────────────┘ │
│                    │ - JobPosting │                                             │
│  ┌──────────────┐  │   Created    │  ┌──────────────┐  ┌────────────────────┐ │
│  │    audit     │  │ - CertVerifi │  │ observability│  │    exception       │ │
│  │              │  │   ed         │  │              │  │                    │ │
│  │ Auditable    │  │ - UserRegist │  │ HealthIndiqs │  │ GlobalExceptionHd  │ │
│  │ AuditLog     │  │   ered       │  │ Metrics      │  │ AuthException      │ │
│  │ AuditLogRepo │  │ - AppSubmitt │  │ Tracing      │  │                    │ │
│  │ JpaAuditing  │  │   ed         │  │ RequestCorrl │  │                    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘  └────────────────────┘ │
└────────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Dependency Flow (Clean Architecture Direction)

```
Controllers
    │
    ▼ (call)
Services
    │
    ▼ (call)
Repositories
    │
    ▼ (read/write)
Domain Entities
    │
    ▼ (defined in)
Database (PostgreSQL via Flyway migrations)

Side effects flow:
Services ──publish──► EventBus ──handle──► OutboxEventBridge
                                               │
                                               ▼
                                         OutboxEvents table
                                               │
                                         (scheduled poll)
                                               │
                                               ▼
                                   NotificationOutboxHandler
                                               │
                                               ▼
                                     NotificationService
```

### 1.4 Database Entity Relationship Summary (Abbreviated)

```
app_users (1) ──────────────────────────────── (*) refresh_tokens
app_users (1) ──────────────────────────────── (0..1) students
app_users (1) ──────────────────────────────── (0..1) recruiters
students  (1) ──────────────────────────────── (*) student_skills ◄──── skills
students  (1) ──────────────────────────────── (*) job_applications
students  (1) ──────────────────────────────── (*) certificates ────── (0..1) skills
branches  (1) ──────────────────────────────── (*) students
companies (1) ──────────────────────────────── (*) recruiters
companies (1) ──────────────────────────────── (*) job_postings
job_postings (1) ───────────────────────────── (*) job_posting_skills ◄── skills
job_postings (1) ───────────────────────────── (*) job_posting_branches ◄── branches
job_postings (1) ───────────────────────────── (*) job_applications
job_applications (1) ───────────────────────── (0..1) offers
```

---

## 2. Frontend Module Dependency Map

### 2.1 Layer Architecture (Dependency Direction)

```
Pages (features/<feature>/pages/)
    │ depends on
    ▼
Feature Components (features/<feature>/components/)
    │ depends on
    ▼
Feature Hooks (features/<feature>/hooks/)  ──uses──► TanStack Query
    │ depends on
    ▼
Feature API (features/<feature>/api/)  ──calls──► Axios instance
    │                                                    │
    │                                                    ▼
    │                                            Backend REST API
    │
    │ depends on
    ▼
Types / Zod Schemas (types/)
    │
    ▼ (shared by all layers)

Shared Business Components (shared/business/)
    │ depends on
    ▼
Shared UI Components (shared/ui/)  ──uses──► shadcn/ui + Tailwind

Stores (stores/)  ──used by──► Axios interceptors + useAuth hook
    │
    ├── auth.store.ts  ──── Zustand + persist middleware
    └── ui.store.ts    ──── Zustand
```

### 2.2 Feature Module Dependency Grid

"X uses Y" means Feature X imports from Feature Y (or shared).

| Feature | Depends On |
|---|---|
| auth | shared/ui, stores/auth, lib/axios, types/auth |
| dashboard | features/applications, features/offers, features/certificates (data hooks), shared |
| students | shared/ui, shared/business, types/api, lib/axios |
| companies | shared/ui, shared/business, types/api, lib/axios |
| job-postings | features/companies (company name display), shared, types/api |
| applications | features/students (student info), features/job-postings (posting info), shared |
| offers | features/applications (application info), features/students (student info), shared |
| certificates | features/students (student info), features/files (upload), shared |
| files | shared/ui (FileUpload component), lib/axios |

### 2.3 Shared Module Rules

Per FRONTEND_CONSTITUTION.md Section 6.5:
- `shared/ui` and `shared/business` MUST NOT import from any `features/` module
- `features/` modules MAY import from `shared/`
- `features/` modules SHOULD NOT import from other `features/` (only via hooks/types)
- `lib/` and `stores/` are dependency-free (only import from external packages)
- `types/` defines schemas only — no imports from other src/ modules

### 2.4 Routing Dependency

```
App.tsx (root router)
  │
  ├── AuthLayout
  │     ├── /login ──────────────► LoginPage (features/auth)
  │     ├── /register ───────────► RegisterPage (features/auth)
  │     └── /forgot-password ────► ForgotPasswordPage (features/auth)
  │
  └── ProtectedRoute
        └── DashboardLayout
              │
              ├── /dashboard ──────────────────────────────────► DashboardPage
              │                                                   (role-conditional)
              ├── /profile ────────────────────────────────────► StudentProfilePage
              ├── /job-postings ───────────────────────────────► JobPostingsPage
              ├── /job-postings/:id ───────────────────────────► JobPostingDetailPage
              ├── /applications ───────────────────────────────► MyApplicationsPage
              ├── /offers ─────────────────────────────────────► MyOffersPage
              ├── /certificates ───────────────────────────────► MyCertificatesPage
              ├── /certificates/new ───────────────────────────► SubmitCertificatePage
              │
              └── RoleRoute (PLACEMENT_OFFICER)
                    ├── /admin/students ─────────────────────► StudentsListPage
                    ├── /admin/students/:id ─────────────────► StudentDetailPage
                    ├── /admin/companies ────────────────────► CompaniesListPage
                    ├── /admin/companies/new ────────────────► CreateCompanyPage
                    ├── /admin/companies/:id ────────────────► CompanyDetailPage
                    ├── /admin/job-postings ─────────────────► JobPostingsManagePage
                    ├── /admin/job-postings/new ─────────────► CreateJobPostingPage
                    ├── /admin/job-postings/:id ─────────────► JobPostingManageDetailPage
                    ├── /admin/applications ─────────────────► ApplicationsListPage
                    ├── /admin/applications/:id ─────────────► ApplicationDetailPage
                    ├── /admin/offers ───────────────────────► OffersListPage
                    ├── /admin/certificates ─────────────────► CertificatesQueuePage
                    ├── /admin/skills ───────────────────────► SkillsPage (BLOCKED)
                    └── /admin/branches ─────────────────────► BranchesPage (BLOCKED)
```

### 2.5 State Dependency Map

```
Axios Interceptor (lib/axios.ts)
    ├── reads: stores/auth.store → accessToken (for Authorization header)
    └── writes: stores/auth.store → setAuth / clearAuth (on refresh)

TanStack Query (lib/query-client.ts)
    └── used by: all feature hooks

useAuth hook (hooks/useAuth.ts)
    └── reads: stores/auth.store (isAuthenticated, user, role)

usePermissions hook (hooks/usePermissions.ts)
    └── reads: stores/auth.store → user.role

ProtectedRoute (shared/business)
    └── reads: useAuth → isAuthenticated

RoleRoute (shared/business)
    └── reads: usePermissions → hasRole()

SidebarNav (shared/business)
    └── reads: useAuth → user.role (to show role-appropriate nav items)
```
