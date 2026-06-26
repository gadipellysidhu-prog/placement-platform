# DESIGN_REVIEW.md
## Frontend Design Review — Placement Intelligence & Skill Verification Platform

**Date:** 2026-06-26
**Source:** FRONTEND_CONSTITUTION.md, MASTER_PROMPT.md, docs/CLAUDE.md, backend API analysis
**Status:** Phase 0 — Planning

---

## 1. Layout Strategy

### 1.1 Primary Layout

**Authenticated Layout (`DashboardLayout`)**
- Fixed left sidebar (240px collapsed to icon-only on mobile)
- Top header bar (breadcrumb + user menu + notifications bell)
- Main content area (fluid, scrollable)
- No footer in authenticated views

**Public Layout (`AuthLayout`)**
- Centered card on a neutral background
- Platform branding (logo + name)
- No navigation
- Used for: login, register, forgot-password pages

### 1.2 Breakpoint Behavior

Per FRONTEND_CONSTITUTION.md Section 14:

| Breakpoint | Sidebar | Content |
|---|---|---|
| < 768px (mobile) | Hidden, accessible via hamburger drawer | Full width |
| 768–1023px (tablet) | Icon-only collapsed (56px) | Fluid |
| >= 1024px (desktop) | Full expanded (240px) | Fluid |

### 1.3 Design Philosophy

Per constitution Section 12.1, the UI SHALL be inspired by enterprise SaaS: Linear, Vercel, GitHub, Stripe Dashboard.
- Clean whitespace
- Neutral backgrounds (gray-50 / gray-100)
- Card-based content containers
- Consistent header + action button placement (actions top-right of each section)

---

## 2. Navigation Structure

### 2.1 Role-Based Navigation

Navigation items differ by role:

**ROLE_STUDENT**
- Dashboard (overview of profile, applications, offers)
- My Profile (student details, CGPA, branch, year)
- Job Postings (browse open positions)
- My Applications (list with statuses)
- My Offers (pending/accepted/rejected offers)
- My Certificates (list + submit)

**ROLE_PLACEMENT_OFFICER** (inherits STUDENT access implicitly but sees officer views)
- Dashboard (platform overview metrics)
- Students (list, manage, search)
- Companies (list, create, manage)
- Job Postings (all statuses: draft, open, closed)
- Applications (all student applications)
- Offers (all offers)
- Certificates (pending verification queue)
- Skills (manage skill catalogue — pending backend)
- Branches (manage branch catalogue — pending backend)

**ROLE_ADMIN** (inherits PLACEMENT_OFFICER)
- All officer views
- User Management (register officers/admins)
- Audit Logs (pending backend endpoint)
- System Health (actuator metrics view)

### 2.2 Route Constants File

All routes shall be defined in `/src/constants/routes.ts`:

```
/login
/register
/forgot-password

/dashboard
/profile
/job-postings
/job-postings/:id
/applications
/applications/:id
/offers
/offers/:id
/certificates
/certificates/new

/admin/students
/admin/students/:id
/admin/companies
/admin/companies/new
/admin/companies/:id
/admin/job-postings
/admin/job-postings/new
/admin/job-postings/:id
/admin/applications
/admin/applications/:id
/admin/offers
/admin/certificates
/admin/skills
/admin/branches
/admin/users
/admin/audit

/403
/404
```

---

## 3. Dashboard Structure

### 3.1 Student Dashboard

**Top Row — Stat Cards (4 cards):**
1. Applications submitted (total)
2. Applications shortlisted
3. Pending offers
4. Accepted offers / placement status

**Middle Section:**
- Recent Applications table (last 5, with status badges)
- Pending Offers list (if any)

**Right Panel (or bottom on mobile):**
- Profile completeness indicator (CGPA set, branch set, skills added)
- Recent certificates (latest 3)

### 3.2 Placement Officer Dashboard

**Top Row — Stat Cards (6 cards):**
1. Total students registered
2. Placed students
3. Active companies
4. Open job postings
5. Pending applications
6. Pending certificate verifications

**Charts (pending analytics endpoint):**
- Applications by status (donut chart)
- Placements over time (line chart — placeholder until analytics API exists)

**Tables:**
- Recent applications requiring action
- Certificates pending verification

---

## 4. Page List Per Role

### Public Pages

| Page | Route | Description |
|---|---|---|
| Login | /login | Email + password form |
| Register | /register | Email + password + role (student only) |
| Forgot Password | /forgot-password | Email submission (stub) |
| 403 | /403 | Access denied |
| 404 | /404 | Not found |

### Student Pages

| Page | Route | Auth | Description |
|---|---|---|---|
| Dashboard | /dashboard | STUDENT | Overview stats and recent items |
| My Profile | /profile | STUDENT | View/edit own student profile |
| Job Postings | /job-postings | STUDENT | Browse OPEN postings |
| Job Posting Detail | /job-postings/:id | STUDENT | Detail view + apply button |
| My Applications | /applications | STUDENT | Own application list with status |
| My Offers | /offers | STUDENT | Own offers with accept/reject |
| My Certificates | /certificates | STUDENT | Own certificate list |
| Submit Certificate | /certificates/new | STUDENT | Upload file + form |

### Officer Pages

| Page | Route | Auth | Description |
|---|---|---|---|
| Officer Dashboard | /dashboard | OFFICER | Platform metrics |
| Students List | /admin/students | OFFICER | Paginated student table with search |
| Student Detail | /admin/students/:id | OFFICER | View + manage student |
| Companies List | /admin/companies | OFFICER | Company management |
| Create Company | /admin/companies/new | OFFICER | Company creation form |
| Company Detail | /admin/companies/:id | OFFICER | View + activate/deactivate |
| Job Postings (All) | /admin/job-postings | OFFICER | All statuses, filter by status |
| Create Job Posting | /admin/job-postings/new | OFFICER | Create posting form |
| Job Posting Detail | /admin/job-postings/:id | OFFICER | Manage lifecycle |
| Applications | /admin/applications | OFFICER | All applications, status updates |
| Application Detail | /admin/applications/:id | OFFICER | Update status, create offer |
| Offers | /admin/offers | OFFICER | All offers management |
| Certificates Queue | /admin/certificates | OFFICER | Pending verification queue |
| Skills | /admin/skills | OFFICER | Skill catalogue (blocked: no API) |
| Branches | /admin/branches | OFFICER | Branch management (blocked: no API) |

### Admin Pages

| Page | Route | Auth | Description |
|---|---|---|---|
| User Management | /admin/users | ADMIN | Create officer/admin accounts |
| Audit Logs | /admin/audit | ADMIN | Audit trail (blocked: no API) |
| Blacklist Company | Via company detail | ADMIN | Special action button |

---

## 5. Component Hierarchy

### 5.1 Shared UI Components (`/src/shared/ui/`)

These are generic, reusable, presentation-only components (from shadcn/ui base):

- `Button` — variants: primary, secondary, destructive, ghost, outline
- `Input` — text input with label + error display
- `Select` — dropdown/combobox
- `Textarea` — multi-line text input
- `Form` — wrapper with React Hook Form integration
- `FormField` — label + control + error
- `Card`, `CardHeader`, `CardContent`, `CardFooter`
- `Table`, `TableHeader`, `TableRow`, `TableCell`
- `Badge` — status badges (color by value)
- `Dialog`, `DialogContent`, `DialogHeader`
- `AlertDialog` — destructive action confirmation
- `Sheet` — mobile sidebar drawer
- `Toast` / `Toaster` — notification toasts
- `Skeleton` — loading placeholders
- `Spinner` — inline loading indicator
- `Avatar` — user avatar
- `Pagination` — page navigation
- `Separator`
- `DropdownMenu`
- `Tooltip`
- `Alert` — inline message banners (info, success, warning, error)
- `EmptyState` — no-data placeholder with illustration + message
- `ErrorState` — API error display with retry button
- `LoadingState` — centered spinner for page-level loading
- `DatePicker` — date input
- `FileUpload` — drag + drop file upload with type/size validation

### 5.2 Business Components (`/src/shared/business/`)

Reusable components with domain logic:

- `StatusBadge` — maps enum values (ApplicationStatus, OfferStatus, etc.) to colored badges
- `UserMenu` — header dropdown for logout and profile
- `SidebarNav` — role-aware navigation sidebar
- `RoleGuard` — renders children only for specified roles
- `PageHeader` — title + breadcrumb + optional action button

### 5.3 Feature Components (`/src/features/<feature>/components/`)

Feature-specific, not reusable across features:

- `StudentTable` (feature: students)
- `StudentProfileCard` (feature: students)
- `SkillSelector` (feature: students — blocked: needs Skills API)
- `BranchSelector` (feature: students — blocked: needs Branches API)
- `CompanyCard` (feature: companies)
- `JobPostingCard` (feature: job-postings)
- `ApplicationStatusStepper` (feature: applications)
- `CertificateCard` (feature: certificates)
- `FileUploadWidget` (feature: certificates)
- `OfferCard` (feature: offers)
- `StatCard` (feature: dashboard)

### 5.4 Page Components (`/src/features/<feature>/pages/`)

Top-level route components:

- `LoginPage`
- `RegisterPage`
- `ForgotPasswordPage`
- `DashboardPage` (student variant vs officer variant)
- `StudentProfilePage`
- `JobPostingsPage` (student browse)
- `JobPostingDetailPage`
- `MyApplicationsPage`
- `MyOffersPage`
- `MyCertificatesPage`
- `SubmitCertificatePage`
- `StudentsListPage` (officer)
- `StudentDetailPage` (officer)
- `CompaniesListPage`
- `CompanyDetailPage`
- `JobPostingsManagePage` (officer — all statuses)
- `CreateJobPostingPage`
- `JobPostingManageDetailPage`
- `ApplicationsListPage` (officer)
- `ApplicationDetailPage`
- `OffersListPage` (officer)
- `CertificatesQueuePage` (officer)
- `NotFoundPage`
- `ForbiddenPage`

### 5.5 Layouts (`/src/layouts/`)

- `AuthLayout` — centered card layout for public pages
- `DashboardLayout` — sidebar + header + content for authenticated pages

---

## 6. Design System Recommendations

### 6.1 Color Palette (Tailwind config)

```
Primary:   Indigo-600 (#4F46E5) — primary actions, links
Success:   Emerald-600 (#059669) — PLACED, ACCEPTED, VERIFIED, CLEAN
Warning:   Amber-500 (#F59E0B) — PENDING, DRAFT, SHORTLISTED
Danger:    Red-600 (#DC2626) — REJECTED, BLACKLISTED, INFECTED, BLOCKED
Neutral:   Gray-900 (text), Gray-600 (secondary text), Gray-100 (backgrounds)
Info:      Sky-600 (#0284C7) — OPEN, ACTIVE
```

### 6.2 Status → Color Mapping

| Status | Color | Badge |
|---|---|---|
| ApplicationStatus.APPLIED | Gray | Neutral |
| ApplicationStatus.SHORTLISTED | Amber | Warning |
| ApplicationStatus.INTERVIEWED | Blue | Info |
| ApplicationStatus.OFFERED | Indigo | Primary |
| ApplicationStatus.REJECTED | Red | Danger |
| ApplicationStatus.WITHDRAWN | Gray | Neutral |
| OfferStatus.PENDING | Amber | Warning |
| OfferStatus.ACCEPTED | Emerald | Success |
| OfferStatus.REJECTED | Red | Danger |
| OfferStatus.EXPIRED | Gray | Neutral |
| StudentStatus.ACTIVE | Emerald | Success |
| StudentStatus.PLACED | Indigo | Primary |
| StudentStatus.OPTED_OUT | Gray | Neutral |
| StudentStatus.GRADUATED | Purple | Special |
| StudentStatus.BLOCKED | Red | Danger |
| CompanyStatus.ACTIVE | Emerald | Success |
| CompanyStatus.INACTIVE | Gray | Neutral |
| CompanyStatus.BLACKLISTED | Red | Danger |
| JobPostingStatus.DRAFT | Gray | Neutral |
| JobPostingStatus.OPEN | Emerald | Success |
| JobPostingStatus.CLOSED | Amber | Warning |
| JobPostingStatus.CANCELLED | Red | Danger |
| CertificateVerificationStatus.PENDING | Amber | Warning |
| CertificateVerificationStatus.VERIFIED | Emerald | Success |
| CertificateVerificationStatus.REJECTED | Red | Danger |
| FileScanStatus.CLEAN | Emerald | Success |
| FileScanStatus.PENDING | Amber | Warning |
| FileScanStatus.INFECTED | Red | Danger |
| FileScanStatus.SCAN_ERROR | Orange | Warning |

### 6.3 Typography

- Font: Inter (Google Fonts)
- Headings: Inter 600 (semibold)
- Body: Inter 400 (regular)
- Monospace (UUIDs, file keys): JetBrains Mono or system-ui mono

### 6.4 Spacing Scale (Tailwind defaults)

Use Tailwind spacing tokens: `p-2` (8px), `p-4` (16px), `p-6` (24px), `p-8` (32px).

### 6.5 Icon Set

Lucide Icons (via `lucide-react`). Key icons:
- `User`, `Users` — student/user
- `Building2` — company
- `Briefcase` — job posting
- `FileText` — application
- `Award` — certificate / offer
- `CheckCircle` — verified
- `XCircle` — rejected
- `Clock` — pending
- `Upload` — file upload
- `Download` — file download
- `Settings`, `Shield`, `BarChart3`, `Bell`

---

## 7. Accessibility Notes

Per FRONTEND_CONSTITUTION.md Section 13 (WCAG 2.1 Level AA):

1. **All interactive elements** — keyboard navigable, focus visible.
2. **Status badges** — use both color AND text (never color alone for meaning).
3. **Tables** — `<th scope="col">` headers, sortable headers with `aria-sort`.
4. **Modals/Dialogs** — focus trap, `aria-modal="true"`, `aria-labelledby`.
5. **File upload** — keyboard accessible, announces scan status via `aria-live`.
6. **Form errors** — `aria-describedby` linking input to error message, `role="alert"`.
7. **Loading states** — `aria-busy="true"` on loading containers.
8. **Toast notifications** — `role="status"` or `role="alert"` depending on urgency.
9. **Data tables** — consider `caption` for screen reader context.
10. **Color contrast** — all text meets 4.5:1 minimum. Status badges with light backgrounds need dark text.

---

## 8. Feature Flags / Blocked Features

The following planned frontend features are **blocked** by missing backend endpoints:

| Feature | Blocked By | Workaround |
|---|---|---|
| Branch selector in student forms | No Branch API (CRITICAL-1) | Hide field / show "coming soon" |
| Skill selector in student/certificate forms | No Skill API (CRITICAL-2) | Hide field / show "coming soon" |
| Dashboard metrics charts | No Analytics API (CRITICAL-3) | Show placeholder cards with "--" |
| Forgot password flow | Stub implementation (HIGH-5) | Show "Email sent" always |
| Skills management page | No Skill API | Show empty state + banner |
| Branches management page | No Branch API | Show empty state + banner |
| User management (create officers) | No admin user creation endpoint | Show banner |
| Audit logs page | No audit log API | Show banner |
| In-app notifications | No notification API | Show bell with count "0" |
| Company logo upload | Unclear file pipeline → URL mapping | Accept URL string only |
