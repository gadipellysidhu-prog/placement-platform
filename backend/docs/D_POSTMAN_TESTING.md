# D. Postman Testing Guide

Complete guide to testing all API endpoints using Postman or curl.

---

## Setup

### Base URL
```
http://localhost:8081
```

### Postman environment variables

Create a Postman environment with:

| Variable | Value |
|----------|-------|
| `baseUrl` | `http://localhost:8081` |
| `accessToken` | _(set after login)_ |
| `refreshToken` | _(set after login)_ |
| `studentId` | _(set after student creation)_ |
| `companyId` | _(set after company creation)_ |
| `jobPostingId` | _(set after job posting creation)_ |
| `applicationId` | _(set after application)_ |
| `offerId` | _(set after offer creation)_ |
| `certificateId` | _(set after certificate submission)_ |

### Auto-set token (Postman Test script)

Add to the Login / Register request's **Tests** tab:
```javascript
const json = pm.response.json();
pm.environment.set("accessToken", json.accessToken);
pm.environment.set("refreshToken", json.refreshToken);
```

### Authorization header

For all protected requests, set:
```
Authorization: Bearer {{accessToken}}
```

---

## 1. Registration

### Register STUDENT
```
POST {{baseUrl}}/auth/register
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "password123",
  "role": "ROLE_STUDENT"
}
```

Expected: `201 Created`
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "abc123...",
  "tokenType": "Bearer",
  "accessTokenExpiresIn": 900000
}
```

### Register PLACEMENT_OFFICER
```
POST {{baseUrl}}/auth/register
Content-Type: application/json

{
  "email": "officer@example.com",
  "password": "password123",
  "role": "ROLE_PLACEMENT_OFFICER"
}
```

### Register ADMIN
```
POST {{baseUrl}}/auth/register
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "password123",
  "role": "ROLE_ADMIN"
}
```

---

## 2. Login

```
POST {{baseUrl}}/auth/login
Content-Type: application/json

{
  "email": "student@example.com",
  "password": "password123"
}
```

Expected: `200 OK` with tokens.

---

## 3. Token Refresh

```
POST {{baseUrl}}/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{{refreshToken}}"
}
```

Expected: `200 OK` with **new** access and refresh tokens.
Old refresh token is now **revoked**.

---

## 4. Logout

```
POST {{baseUrl}}/auth/logout
Content-Type: application/json
Authorization: Bearer {{accessToken}}

{
  "refreshToken": "{{refreshToken}}"
}
```

Expected: `204 No Content`. The refresh token is revoked.

---

## 5. Protected endpoints

### Get current user
```
GET {{baseUrl}}/api/users/me
Authorization: Bearer {{accessToken}}
```

Expected: `200 OK`
```json
{
  "email": "student@example.com",
  "role": "ROLE_STUDENT"
}
```

### Admin-only endpoint
```
GET {{baseUrl}}/api/users/admin-only
Authorization: Bearer {{accessToken}}   # must be ADMIN token
```

Expected (ADMIN): `200 OK`  
Expected (STUDENT/OFFICER): `403 Forbidden`

---

## 6. Student management (PLACEMENT_OFFICER token required)

### Create student profile
```
POST {{baseUrl}}/api/students
Authorization: Bearer {{officerToken}}
Content-Type: application/json

{
  "userId": "{{studentUserId}}",
  "rollNumber": "CS2024001",
  "branchId": null,
  "currentYear": 3
}
```

Expected: `201 Created`. Save the `id` as `{{studentId}}`.

### List all students
```
GET {{baseUrl}}/api/students?page=0&size=20
Authorization: Bearer {{officerToken}}
```

### Get student by ID
```
GET {{baseUrl}}/api/students/{{studentId}}
Authorization: Bearer {{officerToken}}
```

### Student views own profile
```
GET {{baseUrl}}/api/students/me
Authorization: Bearer {{studentToken}}
```

---

## 7. Company management (PLACEMENT_OFFICER token required)

### Create company
```
POST {{baseUrl}}/api/companies
Authorization: Bearer {{officerToken}}
Content-Type: application/json

{
  "name": "Acme Corp",
  "description": "Tech company",
  "industry": "Technology",
  "website": "https://acme.com"
}
```

Expected: `201 Created`. Save `id` as `{{companyId}}`.

### Create job posting
```
POST {{baseUrl}}/api/job-postings
Authorization: Bearer {{officerToken}}
Content-Type: application/json

{
  "companyId": "{{companyId}}",
  "title": "Software Engineer",
  "description": "Java backend development",
  "minCgpa": 7.0,
  "maxCgpa": 10.0,
  "deadline": null,
  "vacancies": 5
}
```

Expected: `201 Created`. Save `id` as `{{jobPostingId}}`.

### Open job posting
```
POST {{baseUrl}}/api/job-postings/{{jobPostingId}}/open
Authorization: Bearer {{officerToken}}
```

Expected: `200 OK`.

---

## 8. Job Applications (STUDENT token required)

### Apply to a job posting
```
POST {{baseUrl}}/api/applications
Authorization: Bearer {{studentToken}}
Content-Type: application/json

{
  "studentId": "{{studentId}}",
  "jobPostingId": "{{jobPostingId}}"
}
```

Expected: `201 Created`. Save `id` as `{{applicationId}}`.

> **Note:** `studentId` must match the student linked to the authenticated user.
> Using another student's ID returns `403 Forbidden`.

### View own applications
```
GET {{baseUrl}}/api/applications/my
Authorization: Bearer {{studentToken}}
```

### Withdraw application
```
POST {{baseUrl}}/api/applications/{{applicationId}}/withdraw
Authorization: Bearer {{studentToken}}
```

Expected: `200 OK`.

---

## 9. Offers (PLACEMENT_OFFICER creates, STUDENT accepts/rejects)

### Create offer
```
POST {{baseUrl}}/api/offers
Authorization: Bearer {{officerToken}}
Content-Type: application/json

{
  "applicationId": "{{applicationId}}",
  "packageLpa": 12.5,
  "joiningDate": "2025-07-01",
  "expiryDate": "2025-06-15"
}
```

Expected: `201 Created`. Save `id` as `{{offerId}}`.

### Student accepts offer
```
POST {{baseUrl}}/api/offers/{{offerId}}/accept
Authorization: Bearer {{studentToken}}
```

### Student rejects offer
```
POST {{baseUrl}}/api/offers/{{offerId}}/reject
Authorization: Bearer {{studentToken}}
```

---

## 10. Certificates (STUDENT submits, OFFICER verifies)

### Submit certificate
```
POST {{baseUrl}}/api/certificates
Authorization: Bearer {{studentToken}}
Content-Type: application/json

{
  "studentId": "{{studentId}}",
  "name": "AWS Certified Developer",
  "issuingOrganization": "Amazon Web Services",
  "issueDate": "2024-01-15",
  "expiryDate": "2027-01-15"
}
```

Expected: `201 Created`. Save `id` as `{{certificateId}}`.

### Verify certificate
```
POST {{baseUrl}}/api/certificates/{{certificateId}}/verify
Authorization: Bearer {{officerToken}}
```

---

## 11. File Upload

### Upload a PDF
```
POST {{baseUrl}}/api/files/upload
Authorization: Bearer {{accessToken}}
Content-Type: multipart/form-data

file: [attach a PDF file]
```

Expected: `201 Created`
```json
{
  "id": "...",
  "filename": "resume.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 1024,
  "sha256Hash": "abc123...",
  "scanStatus": "CLEAN",
  "quarantined": false
}
```

### Download file
```
GET {{baseUrl}}/api/files/{{fileId}}
Authorization: Bearer {{accessToken}}
```

---

## 12. Authorization tests

### Test 1: No token → 401
```
GET {{baseUrl}}/api/users/me
```
Expected: `401 Unauthorized` with `Content-Type: application/problem+json`

### Test 2: Expired/invalid token → 401
```
GET {{baseUrl}}/api/users/me
Authorization: Bearer invalid.token.here
```
Expected: `401 Unauthorized`

### Test 3: Student accessing officer endpoint → 403
```
GET {{baseUrl}}/api/students
Authorization: Bearer {{studentToken}}
```
Expected: `403 Forbidden`

### Test 4: Horizontal privilege escalation → 403
```
POST {{baseUrl}}/api/applications
Authorization: Bearer {{student1Token}}
Content-Type: application/json

{
  "studentId": "{{student2Id}}",   ← different student's ID
  "jobPostingId": "{{jobPostingId}}"
}
```
Expected: `403 Forbidden`

### Test 5: Actuator without ADMIN → 401/403
```
GET {{baseUrl}}/actuator/metrics
```
Expected without auth: `401`

```
GET {{baseUrl}}/actuator/metrics
Authorization: Bearer {{studentToken}}
```
Expected with student token: `403`

```
GET {{baseUrl}}/actuator/metrics
Authorization: Bearer {{adminToken}}
```
Expected with admin token: `200`

---

## Error response format

All errors follow RFC 7807 Problem Detail:

```json
{
  "type": "urn:placement:unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Full authentication is required to access this resource",
  "instance": "/api/users/me"
}
```

Content-Type: `application/problem+json`
