# BACKEND_COMPATIBILITY.md
## Backend Gap Analysis for Frontend Integration

**Audit Date:** 2026-06-26 | **Refined:** 2026-06-27
**Source:** Verified from backend source code
**Purpose:** Identify gaps, inconsistencies, and missing features the frontend needs but the backend does not yet provide.

---

## Summary

| Severity | Count | Notes |
|---|---|---|
| Critical | ~~3~~ **0** | All 3 resolved in Phase 0.75 |
| High | 6 | |
| Medium | 7 | |
| Low | 5 | |

---

## ✅ Resolved Critical Gaps (Phase 0.75)

### ~~CRITICAL-1~~: Branch REST API — **RESOLVED**

`BranchController` added at `/api/branches` with full CRUD + activate/deactivate. Integration tests pass.
Frontend can now populate branch dropdowns via `GET /api/branches`.

---

### ~~CRITICAL-2~~: Skill REST API — **RESOLVED**

`SkillController` added at `/api/skills` with list/filter/create/update/verify. Integration tests pass.
Frontend can now populate skill selectors via `GET /api/skills` and `GET /api/skills?category=X`.

---

### ~~CRITICAL-3~~: Dashboard / Analytics Endpoint — **RESOLVED**

`DashboardController` added at `GET /api/dashboard/summary`. Returns `totalStudents`, `placedStudents`, `activeCompanies`, `openJobPostings`, `pendingApplications`, `pendingCertificates`, `placementRatePercent`. Integration tests pass.

---

## Former Critical Gaps (kept for history)

### CRITICAL-1 (RESOLVED): No Branch REST API

**Problem:** `BranchService` has full CRUD (`createBranch`, `updateBranch`, `getAll`, `getActiveBranches`, `deactivate`, `activate`). No `BranchController` existed.

**Resolved:** `BranchController` at `/api/branches`. See API_CONTRACT.md for endpoint details.

---

### CRITICAL-2 (RESOLVED): No Skill REST API

**Problem:** `SkillService` had full operations (`createSkill`, `verify`, `getAll`, `getByCategory`, `getVerified`). No `SkillController` existed.

**Resolved:** `SkillController` at `/api/skills`. Also added `updateSkill()` to `SkillService`. See API_CONTRACT.md for endpoint details.

---

### CRITICAL-3 (RESOLVED): No Dashboard / Analytics Endpoint

**Problem:** No summary statistics endpoint existed.

**Resolved:** `DashboardController` at `GET /api/dashboard/summary` in `modules/dashboard`. See API_CONTRACT.md for response shape.

---

## High Severity Gaps

*(Previously listed as "Critical Gaps" section. Renumbered after CRITICAL-1/2/3 resolution.)*

### FORMER CRITICAL-2 CONTEXT — DTO reference (for history):
```json
{
  "id": "UUID",
  "name": "string",
  "category": "string | null",
  "verified": "boolean",
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```

---

### CRITICAL-3: No Dashboard / Analytics Endpoint

**Problem:** The analytics module is a placeholder (`package-info.java` only). No summary statistics endpoint exists.

**Frontend Impact:**
- A placement platform dashboard MUST show summary metrics: total students, total companies, open postings, applications by status, placement rate, etc.
- Without any analytics endpoint, the dashboard page cannot be built with real data.

**Required Endpoints (to be created):**
- `GET /api/analytics/summary` (PLACEMENT_OFFICER/ADMIN) — aggregate counts
- Response should include: total students, placed students, active job postings, pending applications, pending certificates, placement rate percentage

---

## High Severity Gaps

### HIGH-1: No Recruiter REST API

**Problem:** `RecruiterService` and `Recruiter` entity exist with `company_id` FK. No `RecruiterController`.

**Frontend Impact:**
- Cannot associate a recruiter user to a company.
- The `recruiters` table exists but is completely unmanageable via API.

**Required Endpoints:**
- `POST /api/recruiters` (PLACEMENT_OFFICER) — link user to company as recruiter
- `GET /api/recruiters` (PLACEMENT_OFFICER) — list recruiters
- `GET /api/companies/{id}/recruiters` (PLACEMENT_OFFICER) — recruiters by company

---

### HIGH-2: No Pagination on Student Applications or Certificates (List endpoints return List, not Page)

**Problem:**
- `GET /api/applications/my` returns `List<JobApplicationResponse>` (unbounded)
- `GET /api/applications/student/{studentId}` returns `List<JobApplicationResponse>` (unbounded)
- `GET /api/certificates/my` returns `List<CertificateResponse>` (unbounded)
- `GET /api/certificates/student/{studentId}` returns `List<CertificateResponse>` (unbounded)
- `GET /api/offers/my` returns `List<OfferResponse>` (unbounded)

**Frontend Impact:**
- These list responses could grow large. No pagination means the frontend must handle potentially large payloads.
- Consistent pagination across all collection endpoints improves UX (infinite scroll, page buttons).

**Recommendation:** Convert to `Page<T>` with Pageable support, or add maximum size safeguard.

---

### HIGH-3: No Student Self-Update Endpoint

**Problem:** Students cannot update their own profile. `PUT /api/students/{id}` is `ROLE_PLACEMENT_OFFICER` only.

**Frontend Impact:**
- Students cannot update their own CGPA or current year from the student dashboard.
- All profile management is officer-only.

**Recommendation:** Add `PUT /api/students/me` for authenticated students to update their own profile (CGPA, year).

---

### HIGH-4: Job Posting Status Filtering for Officers

**Problem:** `GET /api/job-postings` (ROLE_STUDENT) returns only OPEN postings. There is no endpoint for officers to list ALL postings across all statuses (DRAFT, OPEN, CLOSED, CANCELLED).

**Frontend Impact:**
- Placement officers cannot see their DRAFT postings from the frontend.
- The job posting management workflow is incomplete.

**Recommendation:** Add `GET /api/job-postings/all` (ROLE_PLACEMENT_OFFICER) that returns all postings with optional `?status=DRAFT` filter; OR add `status` query param to existing endpoint with role-based filtering.

---

### HIGH-5: Email Verification and Password Reset are Stubs

**Problem:** `POST /auth/verify-email` and `POST /auth/forgot-password` exist but `authService.initiateEmailVerification()` and `authService.initiateForgotPassword()` are stub implementations — no email is sent.

**Frontend Impact:**
- The forgot-password flow cannot be built end-to-end.
- Email verification cannot be completed.
- These flows must be marked as "not functional" in frontend planning.

**Classification:** High — functional gap that blocks user self-service auth flows.

---

### HIGH-6: No Token Validation / Session Info Endpoint

**Problem:** `GET /api/users/me` returns `{ email, role }` only. No endpoint confirms token validity or returns full user context (account lock status, email verified status, user ID).

**Frontend Impact:**
- On app load (page refresh), the frontend needs to validate the stored token by calling the backend. The current `/api/users/me` works for this but returns minimal data.
- The user's UUID is not returned, making it hard to correlate with `StudentResponse.userId`.

**Recommendation:** Extend `GET /api/users/me` to return `{ id, email, role, emailVerified, accountLocked }`.

---

## Medium Severity Gaps

### MEDIUM-1: No Job Posting Filtering or Search

**Problem:** `GET /api/job-postings` returns all OPEN postings with no filtering.

**Frontend Impact:**
- Cannot filter by company, CTC range, application deadline, or branch.
- Poor UX for students browsing postings.

**Recommendation:** Add query params: `?companyId=UUID`, `?ctcMin=X`, `?ctcMax=Y`, `?branchId=UUID`.

---

### MEDIUM-2: No Student Search / Filtering

**Problem:** `GET /api/students` returns all students with no filter.

**Frontend Impact:**
- Officers cannot search by roll number, branch, status, or CGPA range.

**Recommendation:** Add query params: `?status=ACTIVE`, `?branchId=UUID`, `?search=rollNumber`.

---

### MEDIUM-3: No Notification REST API

**Problem:** Notifications are internal-only (via outbox + handlers). No endpoint exists for users to view their notification history.

**Frontend Impact:**
- Cannot build an in-app notification inbox/panel.

---

### MEDIUM-4: CompanyUpdateRequest Requires Redundant Fields

**Problem:** `PUT /api/companies/{id}` uses `CompanyUpdateRequest` which requires `name` (not blank) but does not allow partial updates (PATCH). This means a frontend must always send the name even for minor updates.

**Recommendation:** Consider accepting null/optional fields and applying partial updates, or document that name is always required.

---

### MEDIUM-5: JobPosting Missing Required Skills and Eligible Branches in Response

**Problem:** `JobPosting` entity has `requiredSkills` (Set<Skill>) and `eligibleBranches` (Set<Branch>) but `JobPostingResponse` does NOT include them.

**Frontend Impact:**
- Students cannot see what skills or branches are required/eligible for a posting from the list/detail view.

**Recommendation:** Add `requiredSkillNames: string[]` and `eligibleBranchNames: string[]` to `JobPostingResponse`.

---

### MEDIUM-6: No Way to Add Skills/Branches to a Job Posting via API

**Problem:** `JobPosting` has `requiredSkills` and `eligibleBranches` collections but no controller endpoints exist to manage them.

**Frontend Impact:**
- Officers cannot specify required skills or eligible branches for a posting.

**Recommendation:** Add `POST /api/job-postings/{id}/skills/{skillId}`, `DELETE /api/job-postings/{id}/skills/{skillId}`, and similar for branches.

---

### MEDIUM-7: No Audit Log REST API

**Problem:** `AuditLog` entity and `AuditLogRepository` exist (populated via `DomainEventAuditHandler`) but no REST endpoint exposes audit logs.

**Frontend Impact:**
- Admin audit trail page cannot be built.

---

## Low Severity Gaps

### LOW-1: Role Enum Values Use ROLE_ Prefix in Registration

**Problem:** The `RegisterRequest.role` field uses the enum values `ROLE_ADMIN`, `ROLE_PLACEMENT_OFFICER`, `ROLE_STUDENT` (with `ROLE_` prefix). This is Spring Security internal convention and may be confusing for API consumers.

**Recommendation:** Document clearly and validate in Zod schema using these exact values.

---

### LOW-2: No Company Logo Upload Integration

**Problem:** `CompanyUpdateRequest.logoUrl` accepts a URL string but there is no endpoint to upload a company logo file via the file pipeline and get back a URL. The `FileUploadResponse` returns a file `id` (UUID) not a URL.

**Recommendation:** Clarify whether `logoUrl` should be a full URL or a file-pipeline key, and document the expected format.

---

### LOW-3: `StudentResponse.skillNames` Is a Set of Strings (Not Skill Objects)

**Problem:** `skillNames` in `StudentResponse` only returns skill name strings, not skill IDs. This prevents the frontend from knowing which skill UUID to use for the remove-skill endpoint.

**Recommendation:** Change to `skills: [{ id: UUID, name: string, category: string }]` or add a parallel `skillIds` array.

---

### LOW-4: Certificate `fileKey` is an Opaque UUID (Not a URL)

**Problem:** `Certificate.fileKey` stores the file-pipeline UUID. `CertificateResponse.fileKey` returns this UUID as a string. The frontend must construct the download URL as `/api/files/{fileKey}`.

**Recommendation:** Document this convention explicitly. Optionally add a computed `fileUrl` field to the response.

---

### LOW-5: No Application/Offer Count on Job Posting Response

**Problem:** `JobPostingResponse` does not include current application count or remaining offer slots.

**Frontend Impact:**
- Students and officers cannot see how many applications exist or how many spots remain.

**Recommendation:** Add `applicationCount` and `remainingOfferSlots` to `JobPostingResponse` (or provide via a separate summary endpoint).

---

## Authorization Gap Analysis

| Gap | Description | Risk |
|---|---|---|
| Student can register as any role | `POST /auth/register` accepts `role: ROLE_ADMIN` — anyone can self-register as admin | **HIGH** — production security issue |
| No ownership check on GET student | `GET /api/students/{id}` is officer-only; students cannot view another student's profile (correct, but there is also no self-lookup by ID without `/me`) | Low |
| Certificate student ID not server-derived | The client sends `studentId` in the certificate request, then it is re-validated against the authenticated user. This is correct but verbose | Low |

### Authorization Note on Role Registration

The `POST /auth/register` endpoint accepts any `Role` enum value including `ROLE_ADMIN`. In production, this is a significant security gap — any user could self-register as an admin. The backend currently does not restrict registration to `ROLE_STUDENT` only.

**Recommendation:** Restrict `/auth/register` to `ROLE_STUDENT` only; require an admin-only endpoint (e.g., `POST /api/admin/users`) to create officer/admin accounts.

---

## Frontend Integration Reference

### Error Response Format (RFC 7807 ProblemDetail)

All error responses use `Content-Type: application/problem+json`:

```json
{
  "type": "urn:placement:<error-code>",
  "title": "Human-readable title",
  "status": 400,
  "detail": "Optional detail message",
  "errors": ["fieldName: message", "otherField: message"]
}
```

- `errors` array is **only present on 400 validation failures**
- For 401/403/404/409/422/500: `type`, `title`, `status`, `detail` only
- Frontend must check `response.data.errors` (array) for field-level validation display
- Frontend must check `response.data.detail` for non-field errors (conflict, not found, etc.)

**Mapping for Zod-based form error display:**
```typescript
// Parse errors array: "fieldName: message" → { fieldName: "message" }
errors.forEach(e => {
  const [field, ...rest] = e.split(': ');
  setError(field, { message: rest.join(': ') });
});
```

---

### Date and Time Conventions

| Field type | Format | Example | Notes |
|---|---|---|---|
| Timestamp (audit) | ISO-8601 Instant | `"2026-06-27T10:30:00Z"` | `createdAt`, `updatedAt` — always UTC |
| Date-only | ISO-8601 date | `"2027-07-01"` | `applicationDeadline`, `joiningDate` — no time component |
| Token expiry | milliseconds (long) | `900000` | `accessTokenExpiresIn` — 15 minutes in ms |

**Frontend rule:** Always use `new Date(isoString)` for display formatting. Never assume timezone. Format with `Intl.DateTimeFormat` in the user's local timezone for display; send dates to the API in `YYYY-MM-DD` format only (no timezone for date-only fields).

---

### Enum Reference

All enum values sent to and received from the API:

**Role** (auth — `POST /auth/register`)
`ROLE_STUDENT` | `ROLE_PLACEMENT_OFFICER` | `ROLE_ADMIN`
> Frontend should only expose `ROLE_STUDENT` in the registration dropdown (security safeguard).

**StudentStatus** (`PUT /api/students/{id}/status`)
`ACTIVE` | `PLACED` | `OPTED_OUT` | `GRADUATED` | `BLOCKED`

**CompanyStatus** (read-only in response; changed via `/activate`, `/deactivate`, `/blacklist`)
`ACTIVE` | `INACTIVE` | `BLACKLISTED`

**JobPostingStatus** (read-only in response; changed via `/open`, `/close`, `/cancel`)
`DRAFT` | `OPEN` | `CLOSED` | `CANCELLED`

**ApplicationStatus** (`PUT /api/applications/{id}/status`)
`APPLIED` | `SHORTLISTED` | `INTERVIEWED` | `OFFERED` | `REJECTED` | `WITHDRAWN`
> `WITHDRAWN` is set only via `POST /api/applications/{id}/withdraw`

**OfferStatus** (changed via `/accept`, `/reject`, `/expire`)
`PENDING` | `ACCEPTED` | `REJECTED` | `EXPIRED`

**CertificateVerificationStatus** (changed via `/verify`, `/reject`)
`PENDING` | `VERIFIED` | `REJECTED`

**FileScanStatus** (returned in `FileUploadResponse`, not settable by frontend)
`CLEAN` | `PENDING` | `INFECTED` | `SCAN_ERROR`

---

### Required Frontend Environment Variables

| Variable | Purpose | Example |
|---|---|---|
| `VITE_API_BASE_URL` | Backend API base URL | `http://localhost:8081` |

No other environment variables are required for the frontend itself. The backend reads its own env vars (database URL, JWT keys, S3 config, etc.) separately.

> For production, `VITE_API_BASE_URL` must match the backend's `CORS_ALLOWED_ORIGINS` setting in `.env.prod`.

---

### Pagination and Filtering

**Paginated endpoints** (return Spring `Page<T>`):
- `GET /api/students` — page, size, sort
- `GET /api/companies` — page, size, sort
- `GET /api/job-postings` — page, size, sort
- `GET /api/applications` — page, size, sort
- `GET /api/offers` — page, size, sort
- `GET /api/certificates` — page, size, sort

**Unbounded list endpoints** (return `List<T>` — no pagination params accepted):
- `GET /api/applications/my`
- `GET /api/applications/student/{studentId}`
- `GET /api/certificates/my`
- `GET /api/certificates/student/{studentId}`
- `GET /api/offers/my`

**Filtering:** No server-side filtering is implemented on any current endpoint. All filtering must be done client-side or via separate fetch with known IDs.

**Sort field examples:**
- `sort=createdAt,desc` — most recent first (most common)
- `sort=rollNumber,asc` — alphabetical by roll number

---

### HTTP Status Code Handling Guide

| Status | When it occurs | Frontend action |
|---|---|---|
| 200 | Success (GET, PUT, POST action) | Use response data |
| 201 | Resource created (POST) | Use response data, navigate or show success |
| 202 | Accepted (stub endpoints) | Show generic success message |
| 204 | Success, no body (DELETE, logout) | Clear relevant UI state |
| 400 | Validation failure | Parse `errors[]`, display per-field |
| 401 | Token expired or invalid | Attempt silent refresh; if refresh fails, redirect to /login |
| 403 | Wrong role or ownership violation | Show 403 page or toast: "You don't have permission" |
| 404 | Resource not found | Show 404 or inline "Not found" |
| 409 | Conflict (duplicate, wrong state) | Show `detail` field as toast/banner |
| 413 | File too large | Show "File must be under 10MB" |
| 422 | Invalid state transition | Show `detail` field |
| 423 | Account locked | Show "Account locked. Try again in 15 minutes" |
| 429 | Rate limited | Show "Too many requests. Please wait." |
| 500 | Server error | Show generic error with retry |
