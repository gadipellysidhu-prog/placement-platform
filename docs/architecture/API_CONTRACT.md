# API_CONTRACT.md
## REST API Reference — Placement Intelligence & Skill Verification Platform

**Source:** Verified from backend controller, DTO, and service source code. | **Refined:** 2026-06-27
**Base URL (dev):** `http://localhost:8081`
**Base URL (prod):** `https://api.placement.example.com`
**Content-Type:** `application/json` for all requests and responses.
**Auth header:** `Authorization: Bearer <access_token>` (required for all authenticated endpoints).

---

## Endpoint Status Legend

| Symbol | Meaning |
|---|---|
| ✅ IMPLEMENTED | Endpoint exists and is functional |
| ⚠️ STUB | Endpoint exists but has no real implementation |
| ❌ MISSING | No controller — service exists but is unreachable |

All endpoints in Modules 1–9 below are **✅ IMPLEMENTED** unless marked otherwise.  
All endpoints in the **Missing Endpoints** section at the bottom are **❌ MISSING**.

---

## Error Response Format (RFC 7807 ProblemDetail)

All error responses use `Content-Type: application/problem+json` and return:

```json
{
  "type": "urn:placement:<error-code>",
  "title": "Human-readable title",
  "status": 400,
  "detail": "Optional detail message",
  "errors": ["field: message", "field2: message2"]
}
```

The `errors` array is present only on validation failures (400).

| HTTP Status | Condition |
|---|---|
| 400 | Validation failure (`errors` array present), malformed JSON, missing parameter |
| 401 | Unauthenticated (missing/invalid/expired token) |
| 403 | Insufficient role, ownership violation |
| 404 | Resource not found |
| 405 | HTTP method not allowed |
| 409 | Conflict (duplicate email, roll number, etc.) |
| 413 | File too large |
| 415 | Unsupported media type |
| 422 | Invalid state transition |
| 429 | Rate limit exceeded |
| 500 | Internal server error |

---

## Pagination Convention

Collection endpoints that accept `Pageable` respond with a Spring Data `Page` object:

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": false }
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false,
  "first": true,
  "numberOfElements": 20,
  "size": 20,
  "number": 0,
  "empty": false
}
```

**Query parameters:**
- `page` — 0-indexed page number (default: 0)
- `size` — Page size (default: 20)
- `sort` — Field and direction, e.g. `sort=createdAt,desc`

---

## Module 1: Authentication (`/auth`)

These endpoints are **PUBLIC** (no auth token required).

---

### POST /auth/register

Register a new user account.

**Auth:** None  
**Rate limit:** 3 requests/IP/window

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass1!",
  "role": "ROLE_STUDENT"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| email | string | Valid email format | Yes |
| password | string | 8–128 characters | Yes |
| role | enum | **ROLE_STUDENT only** — submitting ROLE_ADMIN or ROLE_PLACEMENT_OFFICER returns 400 | Yes |

> **Security note (fixed in Phase 0.75):** Public registration is restricted to `ROLE_STUDENT`. Privileged users must be created via direct DB provisioning. Submitting a privileged role returns `400 Bad Request`.

**Response: 201 Created**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "a1b2c3...",
  "accessTokenExpiresIn": 900000,
  "tokenType": "Bearer"
}
```

**Errors:**
- `400 Bad Request` — Privileged role submitted (`ROLE_ADMIN`, `ROLE_PLACEMENT_OFFICER`)
- `409 Conflict` — Email already registered

---

### POST /auth/login

Authenticate with email and password.

**Auth:** None  
**Rate limit:** 5 requests/IP/window

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass1!"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| email | string | Valid email format, not blank | Yes |
| password | string | Not blank | Yes |

**Response: 200 OK**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "a1b2c3...",
  "accessTokenExpiresIn": 900000,
  "tokenType": "Bearer"
}
```

**Errors:**
- `401 Unauthorized` — Invalid credentials
- `423 Locked` — Account locked (5 failed attempts, 15-minute lockout)

---

### POST /auth/refresh

Exchange a refresh token for a new access token (single-use rotation).

**Auth:** None  
**Rate limit:** 20 requests/IP/window

**Request Body:**
```json
{
  "refreshToken": "a1b2c3..."
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| refreshToken | string | Not blank | Yes |

**Response: 200 OK**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "d4e5f6...",
  "accessTokenExpiresIn": 900000,
  "tokenType": "Bearer"
}
```

**Errors:**
- `401 Unauthorized` — Invalid, expired, or already-used refresh token

---

### POST /auth/logout

Invalidate the refresh token.

**Auth:** None (refresh token in body)

**Request Body:**
```json
{
  "refreshToken": "a1b2c3..."
}
```

**Response: 204 No Content**

---

### ⚠️ POST /auth/verify-email

Initiate email verification flow.

**Auth:** None  
**Query param:** `?email=user@example.com`

**Response: 202 Accepted**

> **STUB:** The endpoint responds 202 but no email is sent. Frontend should display "If registered, you will receive a verification email" without relying on any backend confirmation.

---

### ⚠️ POST /auth/forgot-password

Initiate password reset flow.

**Auth:** None  
**Query param:** `?email=user@example.com`

**Response: 202 Accepted**

> **STUB:** The endpoint responds 202 but no email is sent. Frontend must show a generic "If this email is registered, a reset link will be sent" message. The full reset flow cannot be built until the backend implements email dispatch.

---

## Module 2: Users (`/api/users`)

---

### GET /api/users/me

Get the authenticated user's profile. Used for session rehydration on page load.

**Auth:** Required (any role)

**Response: 200 OK**
```json
{
  "email": "user@example.com",
  "role": "ROLE_STUDENT"
}
```

| Field | Type | Description |
|---|---|---|
| email | string | User email (principal name) |
| role | string | First granted authority (`ROLE_STUDENT`, `ROLE_PLACEMENT_OFFICER`, `ROLE_ADMIN`) |

> **Frontend Note:** This endpoint does NOT return the user UUID. When correlating with `StudentResponse.userId`, the frontend must first call `GET /api/students/me` to retrieve the student record. See HIGH-6 in BACKEND_COMPATIBILITY.md.

---

### GET /api/users/admin-only

Admin-only resource test endpoint.

**Auth:** Required — `ROLE_ADMIN`

**Response: 200 OK** — `"admin-resource"` (plain string)

**Errors:**
- `403 Forbidden` — Insufficient role

---

## Module 3: Students (`/api/students`)

---

### POST /api/students

Create a student profile linked to an existing AppUser.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "rollNumber": "CS2021001",
  "branchId": "550e8400-e29b-41d4-a716-446655440001",
  "currentYear": 2
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| userId | UUID | Must reference existing AppUser | Yes |
| rollNumber | string | max 50 chars, not blank, unique | Yes |
| branchId | UUID | Must reference existing Branch | No |
| currentYear | int | 1–6 | Yes |

**Response: 201 Created** — `StudentResponse` (see schema below)

**Errors:**
- `404 Not Found` — User not found
- `409 Conflict` — Student profile already exists for user, or roll number taken

---

### GET /api/students

List all students (paginated).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`  
**Query params:** `page`, `size`, `sort`

**Response: 200 OK** — `Page<StudentResponse>`

---

### GET /api/students/me

Get the authenticated student's own profile.

**Auth:** Required — `ROLE_STUDENT`

**Response: 200 OK** — `StudentResponse`

**Errors:**
- `404 Not Found` — Student profile not found for this user

---

### GET /api/students/{id}

Get a specific student by UUID.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Path param:** `id` (UUID)

**Response: 200 OK** — `StudentResponse`

**Errors:**
- `404 Not Found` — Student not found

---

### PUT /api/students/{id}

Update a student's profile (branch, CGPA, current year).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Path param:** `id` (UUID)

**Request Body:**
```json
{
  "branchId": "550e8400-e29b-41d4-a716-446655440001",
  "cgpa": 8.5,
  "currentYear": 3
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| branchId | UUID | Optional | No |
| cgpa | decimal | 0.0–10.0 | No |
| currentYear | int | 1–6 | Yes |

**Response: 200 OK** — `StudentResponse`

---

### PUT /api/students/{id}/status

Update a student's placement status.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Path param:** `id` (UUID)

**Request Body:**
```json
{
  "status": "PLACED"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| status | enum | ACTIVE, PLACED, OPTED_OUT, GRADUATED, BLOCKED | Yes |

**Valid transitions:**
- ACTIVE → PLACED, OPTED_OUT, BLOCKED, GRADUATED
- PLACED → GRADUATED
- OPTED_OUT → ACTIVE
- BLOCKED → ACTIVE
- GRADUATED → (none)

**Response: 200 OK** — `StudentResponse`

**Errors:**
- `422 Unprocessable Entity` — Invalid status transition

---

### PUT /api/students/{id}/eligibility

Re-evaluate and update a student's placement eligibility.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

*Logic: eligible = status is ACTIVE AND cgpa is set AND cgpa >= 5.0*

**Response: 200 OK** — `StudentResponse`

---

### POST /api/students/{id}/skills/{skillId}

Assign a skill to a student.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Path params:** `id` (student UUID), `skillId` (UUID)

**Response: 200 OK** — `StudentResponse`

**Errors:**
- `404 Not Found` — Student or skill not found

---

### DELETE /api/students/{id}/skills/{skillId}

Remove a skill from a student.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Path params:** `id` (student UUID), `skillId` (UUID)

**Response: 200 OK** — `StudentResponse`

---

### StudentResponse Schema

```json
{
  "id": "UUID",
  "userId": "UUID",
  "userEmail": "string",
  "rollNumber": "string",
  "branchId": "UUID | null",
  "branchName": "string | null",
  "cgpa": "decimal | null",
  "currentYear": "int",
  "placementEligible": "boolean",
  "status": "ACTIVE | PLACED | OPTED_OUT | GRADUATED | BLOCKED",
  "skillNames": ["string"],
  "createdAt": "ISO-8601 Instant",
  "updatedAt": "ISO-8601 Instant"
}
```

---

## Module 4: Companies (`/api/companies`)

---

### POST /api/companies

Register a new company.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:**
```json
{
  "name": "Acme Corp",
  "website": "https://acme.example.com",
  "industry": "Technology",
  "description": "Software company"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| name | string | max 255 chars, not blank, unique | Yes |
| website | string | max 255 chars | No |
| industry | string | max 100 chars | No |
| description | string | text | No |

**Response: 201 Created** — `CompanyResponse`

**Errors:**
- `409 Conflict` — Company name already exists

---

### GET /api/companies

List all companies (paginated).

**Auth:** Required — `ROLE_STUDENT` (and above via role hierarchy)  
**Query params:** `page`, `size`, `sort`

**Response: 200 OK** — `Page<CompanyResponse>`

---

### GET /api/companies/{id}

Get company by UUID.

**Auth:** Required — `ROLE_STUDENT`

**Response: 200 OK** — `CompanyResponse`

**Errors:**
- `404 Not Found`

---

### PUT /api/companies/{id}

Update company details.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:**
```json
{
  "name": "Acme Corp",
  "website": "https://acme.example.com",
  "industry": "Technology",
  "description": "Updated description",
  "logoUrl": "https://cdn.example.com/logo.png"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| name | string | max 255, not blank | Yes |
| website | string | max 255 | No |
| industry | string | max 100 | No |
| description | string | text | No |
| logoUrl | string | max 500 | No |

**Response: 200 OK** — `CompanyResponse`

---

### POST /api/companies/{id}/activate

Activate a company (sets status to ACTIVE).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `CompanyResponse`

---

### POST /api/companies/{id}/deactivate

Deactivate a company (sets status to INACTIVE).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `CompanyResponse`

---

### POST /api/companies/{id}/blacklist

Blacklist a company (sets status to BLACKLISTED).

**Auth:** Required — `ROLE_ADMIN` only

**Response: 200 OK** — `CompanyResponse`

---

### CompanyResponse Schema

```json
{
  "id": "UUID",
  "name": "string",
  "website": "string | null",
  "industry": "string | null",
  "description": "string | null",
  "logoUrl": "string | null",
  "status": "ACTIVE | INACTIVE | BLACKLISTED",
  "createdAt": "ISO-8601 Instant",
  "updatedAt": "ISO-8601 Instant"
}
```

---

## Module 5: Job Postings (`/api/job-postings`)

---

### POST /api/job-postings

Create a job posting (starts in DRAFT status).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:**
```json
{
  "companyId": "UUID",
  "title": "Backend Engineer",
  "description": "Full description...",
  "ctcMin": 8.0,
  "ctcMax": 15.0,
  "applicationDeadline": "2026-12-31",
  "offerLimit": 5
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| companyId | UUID | Must reference existing Company | Yes |
| title | string | max 255, not blank | Yes |
| description | string | text | No |
| ctcMin | decimal | >= 0.0 | No |
| ctcMax | decimal | >= 0.0 | No |
| applicationDeadline | date | ISO-8601 date (YYYY-MM-DD) | No |
| offerLimit | int | >= 1 | Yes |

**Response: 201 Created** — `JobPostingResponse`

---

### GET /api/job-postings

List job postings (paginated).

**Auth:** Required — `ROLE_STUDENT`  
**Scope:** Returns **only OPEN** postings. Officers cannot use this endpoint to list DRAFT or CLOSED postings — that endpoint is missing. See ❌ MISSING section and HIGH-4 in BACKEND_COMPATIBILITY.md.

**Response: 200 OK** — `Page<JobPostingResponse>`

---

### GET /api/job-postings/{id}

Get job posting by UUID.

**Auth:** Required — `ROLE_STUDENT`

**Response: 200 OK** — `JobPostingResponse`

**Errors:**
- `404 Not Found`

---

### PUT /api/job-postings/{id}

Update a DRAFT job posting.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:** Same fields as create (all required as per update DTO).

**Response: 200 OK** — `JobPostingResponse`

**Errors:**
- `409 Conflict` — Posting is not in DRAFT status

---

### POST /api/job-postings/{id}/open

Transition a DRAFT posting to OPEN status.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `JobPostingResponse`

**Errors:**
- `409 Conflict` — Not in DRAFT status

---

### POST /api/job-postings/{id}/close

Transition an OPEN posting to CLOSED status.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `JobPostingResponse`

**Errors:**
- `409 Conflict` — Not in OPEN status

---

### POST /api/job-postings/{id}/cancel

Cancel a job posting.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `JobPostingResponse`

---

### JobPostingResponse Schema

```json
{
  "id": "UUID",
  "companyId": "UUID",
  "companyName": "string",
  "title": "string",
  "description": "string | null",
  "ctcMin": "decimal | null",
  "ctcMax": "decimal | null",
  "status": "DRAFT | OPEN | CLOSED | CANCELLED",
  "applicationDeadline": "YYYY-MM-DD | null",
  "offerLimit": "int",
  "createdAt": "ISO-8601 Instant",
  "updatedAt": "ISO-8601 Instant"
}
```

---

## Module 6: Job Applications (`/api/applications`)

---

### POST /api/applications

Apply to a job posting.

**Auth:** Required — `ROLE_STUDENT`  
**Ownership:** `req.studentId` must match the authenticated student's own ID.

**Request Body:**
```json
{
  "studentId": "UUID",
  "jobPostingId": "UUID"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| studentId | UUID | Must be own student profile | Yes |
| jobPostingId | UUID | Must reference OPEN posting | Yes |

**Response: 201 Created** — `JobApplicationResponse`

**Errors:**
- `403 Forbidden` — Applying for another student
- `409 Conflict` — Already applied to this posting

---

### GET /api/applications

List all applications (paginated).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`  
**Query params:** `page`, `size`, `sort`

**Response: 200 OK** — `Page<JobApplicationResponse>`

---

### GET /api/applications/my

Get the authenticated student's own applications.

**Auth:** Required — `ROLE_STUDENT`

**Response: 200 OK** — `List<JobApplicationResponse>`

> **Pagination gap:** Returns an unbounded list (no `page`/`size` params). See HIGH-2 in BACKEND_COMPATIBILITY.md.

---

### GET /api/applications/{id}

Get application by UUID.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `JobApplicationResponse`

**Errors:**
- `404 Not Found`

---

### GET /api/applications/student/{studentId}

Get all applications for a specific student.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `List<JobApplicationResponse>`

---

### PUT /api/applications/{id}/status

Update application status.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:**
```json
{
  "status": "SHORTLISTED"
}
```

| Field | Type | Values | Required |
|---|---|---|---|
| status | enum | APPLIED, SHORTLISTED, INTERVIEWED, OFFERED, REJECTED | Yes |

**Valid status transitions (officer-driven):**
- APPLIED → SHORTLISTED, REJECTED
- SHORTLISTED → INTERVIEWED, REJECTED
- INTERVIEWED → OFFERED, REJECTED
- OFFERED → (offer entity created via `POST /api/offers`)
- REJECTED → (terminal)

**Student-driven transitions:** APPLIED or SHORTLISTED → WITHDRAWN (via `POST /api/applications/{id}/withdraw`)

**Response: 200 OK** — `JobApplicationResponse`

---

### POST /api/applications/{id}/withdraw

Withdraw an application.

**Auth:** Required — `ROLE_STUDENT`  
**Ownership:** Only the student who applied can withdraw.

**Response: 200 OK** — `JobApplicationResponse`

**Errors:**
- `403 Forbidden` — Not own application

---

### JobApplicationResponse Schema

```json
{
  "id": "UUID",
  "studentId": "UUID",
  "studentRollNumber": "string",
  "jobPostingId": "UUID",
  "jobPostingTitle": "string",
  "companyId": "UUID",
  "companyName": "string",
  "status": "APPLIED | SHORTLISTED | INTERVIEWED | OFFERED | REJECTED | WITHDRAWN",
  "appliedAt": "ISO-8601 Instant",
  "createdAt": "ISO-8601 Instant",
  "updatedAt": "ISO-8601 Instant"
}
```

---

## Module 7: Offers (`/api/offers`)

---

### POST /api/offers

Create a placement offer for an application in OFFERED status.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Request Body:**
```json
{
  "applicationId": "UUID",
  "ctc": 12.5,
  "joiningDate": "2027-07-01"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| applicationId | UUID | Must be in OFFERED status | Yes |
| ctc | decimal | >= 0.0 | No |
| joiningDate | date | ISO-8601 date (YYYY-MM-DD) | No |

**Response: 201 Created** — `OfferResponse`

**Errors:**
- `409 Conflict` — Application not in OFFERED status, or offer already exists

---

### GET /api/offers

List all offers (paginated).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`  
**Query params:** `page`, `size`, `sort`

**Response: 200 OK** — `Page<OfferResponse>`

---

### GET /api/offers/my

Get the authenticated student's own offers.

**Auth:** Required — `ROLE_STUDENT`

**Response: 200 OK** — `List<OfferResponse>`

> **Pagination gap:** Returns an unbounded list. See HIGH-2 in BACKEND_COMPATIBILITY.md.

---

### GET /api/offers/{id}

Get offer by UUID.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `OfferResponse`

**Errors:**
- `404 Not Found`

---

### POST /api/offers/{id}/accept

Accept a PENDING offer.

**Auth:** Required — `ROLE_STUDENT`  
**Ownership:** Only the student who received the offer can accept.

**Response: 200 OK** — `OfferResponse`

**Errors:**
- `403 Forbidden` — Not own offer
- `409 Conflict` — Offer not in PENDING status

---

### POST /api/offers/{id}/reject

Reject a PENDING offer.

**Auth:** Required — `ROLE_STUDENT`  
**Ownership:** Only the student who received the offer can reject.

**Response: 200 OK** — `OfferResponse`

**Errors:**
- `403 Forbidden` — Not own offer
- `409 Conflict` — Offer not in PENDING status

---

### POST /api/offers/{id}/expire

Mark a PENDING offer as expired.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `OfferResponse`

---

### OfferResponse Schema

```json
{
  "id": "UUID",
  "applicationId": "UUID",
  "studentId": "UUID",
  "studentRollNumber": "string",
  "companyId": "UUID",
  "companyName": "string",
  "status": "PENDING | ACCEPTED | REJECTED | EXPIRED",
  "ctc": "decimal | null",
  "joiningDate": "YYYY-MM-DD | null",
  "createdAt": "ISO-8601 Instant",
  "updatedAt": "ISO-8601 Instant"
}
```

---

## Module 8: Certificates (`/api/certificates`)

---

### POST /api/certificates

Submit a certificate for verification.

**Auth:** Required — `ROLE_STUDENT`  
**Ownership:** `req.studentId` must match the authenticated student's own ID.

**Request Body:**
```json
{
  "studentId": "UUID",
  "name": "AWS Certified Developer",
  "issuingOrganization": "Amazon Web Services",
  "skillId": "UUID",
  "fileKey": "UUID"
}
```

| Field | Type | Constraints | Required |
|---|---|---|---|
| studentId | UUID | Must be own student profile | Yes |
| name | string | max 255, not blank | Yes |
| issuingOrganization | string | max 255 | No |
| skillId | UUID | Must reference existing Skill | No |
| fileKey | string | max 500, UUID from file upload | No |

**Response: 201 Created** — `CertificateResponse`

**Errors:**
- `403 Forbidden` — Submitting for another student

---

### GET /api/certificates

List all certificates (paginated).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`  
**Query params:** `page`, `size`, `sort`

**Response: 200 OK** — `Page<CertificateResponse>`

---

### GET /api/certificates/my

Get the authenticated student's own certificates.

**Auth:** Required — `ROLE_STUDENT`

**Response: 200 OK** — `List<CertificateResponse>`

> **Pagination gap:** Returns an unbounded list. See HIGH-2 in BACKEND_COMPATIBILITY.md.

---

### GET /api/certificates/{id}

Get certificate by UUID.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `CertificateResponse`

**Errors:**
- `404 Not Found`

---

### GET /api/certificates/student/{studentId}

Get all certificates for a specific student.

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `List<CertificateResponse>`

---

### POST /api/certificates/{id}/verify

Verify a certificate (sets verificationStatus to VERIFIED).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `CertificateResponse`

---

### POST /api/certificates/{id}/reject

Reject a certificate (sets verificationStatus to REJECTED).

**Auth:** Required — `ROLE_PLACEMENT_OFFICER`

**Response: 200 OK** — `CertificateResponse`

---

### CertificateResponse Schema

```json
{
  "id": "UUID",
  "studentId": "UUID",
  "studentRollNumber": "string",
  "skillId": "UUID | null",
  "skillName": "string | null",
  "name": "string",
  "issuingOrganization": "string | null",
  "fileKey": "string | null",
  "verificationStatus": "PENDING | VERIFIED | REJECTED",
  "createdAt": "ISO-8601 Instant",
  "updatedAt": "ISO-8601 Instant"
}
```

---

## Module 9: File Pipeline (`/api/files`)

---

### POST /api/files/upload

Upload a file (multipart form data). Returns a `fileKey` UUID for use in certificate submissions.

**Auth:** Required — any authenticated role (STUDENT, PLACEMENT_OFFICER, ADMIN)  
**Content-Type:** `multipart/form-data`  
**Max size:** 10MB  
**Allowed MIME types:** `application/pdf`, `image/png`, `image/jpeg`

**Form field:** `file` (multipart file)

**Response: 201 Created**
```json
{
  "id": "UUID",
  "filename": "certificate.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 102400,
  "sha256Hash": "abc123...",
  "scanStatus": "CLEAN | INFECTED | SCAN_ERROR | PENDING",
  "quarantined": false,
  "uploadedBy": "user@example.com",
  "uploadedAt": "ISO-8601 Instant"
}
```

**Errors:**
- `400 Bad Request` — No file provided or invalid file type
- `413 Payload Too Large` — File exceeds 10MB
- `422 Unprocessable Entity` — Virus detected

---

### GET /api/files/{id}

Download a file by its UUID (streams the file).

**Auth:** Required — any authenticated role  
**Path param:** `id` (UUID)

**Response: 200 OK** — File bytes with appropriate `Content-Type` and `Content-Disposition: attachment` headers.

**Errors:**
- `403 Forbidden` — File is quarantined
- `404 Not Found` — File not found

---

### DELETE /api/files/{id}

Delete a file and its metadata record.

**Auth:** Required — `ROLE_ADMIN` or `ROLE_PLACEMENT_OFFICER`  
**Path param:** `id` (UUID)

**Response: 204 No Content**

---

## Module 10: Actuator (Admin/Monitoring)

---

### GET /actuator/health

Basic health check (public).

**Auth:** None

**Response: 200 OK**
```json
{ "status": "UP" }
```

---

### GET /actuator/health/liveness

JVM liveness probe.

**Auth:** None (public)

**Response: 200 OK** — `{ "status": "UP" }`

---

### GET /actuator/health/readiness

Readiness probe (DB connectivity).

**Auth:** None (public)

**Response: 200 OK** — `{ "status": "UP" }`

---

### GET /actuator/metrics

Expose Micrometer metrics.

**Auth:** Required — `ROLE_ADMIN`

---

### GET /actuator/prometheus

Prometheus scrape endpoint.

**Auth:** Required — `ROLE_ADMIN` (additionally restricted to internal IPs via nginx)

---

### GET /actuator/info

Application info.

**Auth:** Required — `ROLE_ADMIN`

---

## OpenAPI / Swagger

The backend exposes OpenAPI 3 documentation:
- UI: `GET /swagger-ui/index.html` (public)
- JSON: `GET /v3/api-docs` (public)

---

---

## ✅ Endpoints Added in Phase 0.75

These were previously missing and are now **fully implemented** with integration tests passing.

### ✅ Branch API (`/api/branches`) — IMPLEMENTED

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/branches | STUDENT | List all active branches |
| GET | /api/branches/{id} | STUDENT | Get branch by ID |
| POST | /api/branches | PLACEMENT_OFFICER | Create branch |
| PUT | /api/branches/{id} | PLACEMENT_OFFICER | Update branch |
| POST | /api/branches/{id}/activate | PLACEMENT_OFFICER | Activate branch |
| POST | /api/branches/{id}/deactivate | PLACEMENT_OFFICER | Deactivate branch |

**Response shape:**
```json
{ "id": "UUID", "name": "string", "code": "string|null", "description": "string|null", "active": true, "createdAt": "ISO-8601", "updatedAt": "ISO-8601" }
```

**Errors:** `409 Conflict` — duplicate name or code.

---

### ✅ Skill API (`/api/skills`) — IMPLEMENTED

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/skills | STUDENT | List all skills |
| GET | /api/skills?category=X | STUDENT | Filter by category |
| GET | /api/skills?verified=true | STUDENT | Filter verified only |
| GET | /api/skills/{id} | STUDENT | Get skill by ID |
| POST | /api/skills | PLACEMENT_OFFICER | Create skill |
| PUT | /api/skills/{id} | PLACEMENT_OFFICER | Update skill name/category |
| POST | /api/skills/{id}/verify | PLACEMENT_OFFICER | Verify skill |

**Response shape:**
```json
{ "id": "UUID", "name": "string", "category": "string|null", "verified": true, "createdAt": "ISO-8601", "updatedAt": "ISO-8601" }
```

**Errors:** `409 Conflict` — duplicate name.

---

### ✅ Dashboard Summary (`/api/dashboard/summary`) — IMPLEMENTED

| Method | Path | Auth | Purpose |
|---|---|---|---|
| GET | /api/dashboard/summary | PLACEMENT_OFFICER | Aggregate placement statistics |

**Response shape:**
```json
{
  "totalStudents": 0,
  "placedStudents": 0,
  "activeCompanies": 0,
  "openJobPostings": 0,
  "pendingApplications": 0,
  "pendingCertificates": 0,
  "placementRatePercent": 0.0
}
```

---

## ❌ Missing Endpoints (No REST Controller)

These services have full backend implementations but no HTTP controller. The frontend **cannot call these endpoints** — they do not exist yet.

### Missing: Officer Job Postings Endpoint

**Blocking:** Officers cannot see DRAFT/CLOSED/CANCELLED postings. `GET /api/job-postings` returns OPEN only.

**Recommended:**
```
GET /api/job-postings/all?status=DRAFT    (PLACEMENT_OFFICER)
```
or add role-aware `?status` filter to the existing endpoint.

---

### Missing: Recruiter API (`/api/recruiters`)

No controller. `Recruiter` entity and `RecruiterService` exist. Needed to associate users to companies as recruiters.

---

### Missing: Notification API (`/api/notifications`)

`NotificationService` exists; internal only via outbox. No in-app notification REST endpoint.

---

### Missing: Audit Log API (`/api/audit`)

`AuditLog` entity and repository exist (populated via domain event handler). No REST endpoint.
