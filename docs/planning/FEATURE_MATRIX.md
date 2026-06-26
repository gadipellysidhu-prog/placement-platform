# FEATURE_MATRIX.md
## Complete Feature Matrix — Placement Intelligence & Skill Verification Platform

**Date:** 2026-06-26
**Source:** Verified from backend controller, service, and entity source code.

Legend:
- **Role Access:** ADMIN (A), PLACEMENT_OFFICER (PO), STUDENT (S) — role hierarchy means A includes PO+S, PO includes S
- **Complexity:** Low (L), Medium (M), High (H)
- **Phase:** Implementation phase number

---

## 1. Authentication Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| User login | Public | `POST /auth/login` | LoginPage | M | 1 | Not started |
| User registration | Public | `POST /auth/register` | RegisterPage | M | 1 | Not started |
| Token refresh (silent) | Public | `POST /auth/refresh` | Axios interceptor | H | 1 | Not started |
| Logout | Any auth | `POST /auth/logout` | UserMenu component | L | 1 | Not started |
| Forgot password | Public | `POST /auth/forgot-password` (stub) | ForgotPasswordPage | L | 1 | Not started |
| Email verification | Public | `POST /auth/verify-email` (stub) | — | L | 1 | Not started |
| Session rehydration on page load | Any auth | `GET /api/users/me` | App.tsx startup | M | 1 | Not started |
| Get own user profile | Any auth | `GET /api/users/me` | UserMenu | L | 1 | Not started |

---

## 2. Student Features

### Student Self-Service

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| View own student profile | S | `GET /api/students/me` | StudentProfilePage | L | 2 | Not started |
| View own application list | S | `GET /api/applications/my` | MyApplicationsPage | L | 3 | Not started |
| View own offer list | S | `GET /api/offers/my` | MyOffersPage | L | 4 | Not started |
| Accept offer | S | `POST /api/offers/{id}/accept` | MyOffersPage / OfferCard | L | 4 | Not started |
| Reject offer | S | `POST /api/offers/{id}/reject` | MyOffersPage / OfferCard | L | 4 | Not started |
| View own certificate list | S | `GET /api/certificates/my` | MyCertificatesPage | L | 4 | Not started |
| Submit certificate | S | `POST /api/certificates` | SubmitCertificatePage | H | 4 | Not started |
| Apply to job posting | S | `POST /api/applications` | JobPostingDetailPage | M | 3 | Not started |
| Withdraw application | S | `POST /api/applications/{id}/withdraw` | MyApplicationsPage | L | 3 | Not started |
| Browse open job postings | S | `GET /api/job-postings` | JobPostingsPage | L | 3 | Not started |
| View job posting detail | S | `GET /api/job-postings/{id}` | JobPostingDetailPage | L | 3 | Not started |
| Browse companies | S | `GET /api/companies` | CompaniesListPage | L | 2 | Not started |
| View company detail | S | `GET /api/companies/{id}` | CompanyDetailPage | L | 2 | Not started |
| Download certificate file | S | `GET /api/files/{id}` | CertificateCard | L | 4 | Not started |
| Student dashboard | S | Multiple | StudentDashboardPage | M | 2 | Not started |

### Officer — Student Management

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| List all students (paginated) | PO | `GET /api/students` | StudentsListPage | M | 2 | Not started |
| View student by ID | PO | `GET /api/students/{id}` | StudentDetailPage | L | 2 | Not started |
| Create student profile | PO | `POST /api/students` | StudentDetailPage / modal | M | 2 | Not started |
| Update student profile | PO | `PUT /api/students/{id}` | StudentDetailPage | M | 2 | Not started |
| Update student status | PO | `PUT /api/students/{id}/status` | StudentDetailPage | L | 2 | Not started |
| Evaluate student eligibility | PO | `PUT /api/students/{id}/eligibility` | StudentDetailPage | L | 2 | Not started |
| Assign skill to student | PO | `POST /api/students/{id}/skills/{skillId}` | StudentDetailPage | L | BLOCKED | Needs Skills API |
| Remove skill from student | PO | `DELETE /api/students/{id}/skills/{skillId}` | StudentDetailPage | L | BLOCKED | Needs Skills API |
| View student applications | PO | `GET /api/applications/student/{studentId}` | StudentDetailPage | L | 3 | Not started |
| View student certificates | PO | `GET /api/certificates/student/{studentId}` | StudentDetailPage | L | 4 | Not started |

---

## 3. Company Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| List all companies (paginated) | S | `GET /api/companies` | CompaniesListPage | L | 2 | Not started |
| View company detail | S | `GET /api/companies/{id}` | CompanyDetailPage | L | 2 | Not started |
| Create company | PO | `POST /api/companies` | CreateCompanyPage | M | 2 | Not started |
| Update company | PO | `PUT /api/companies/{id}` | CompanyDetailPage | M | 2 | Not started |
| Activate company | PO | `POST /api/companies/{id}/activate` | CompanyDetailPage | L | 2 | Not started |
| Deactivate company | PO | `POST /api/companies/{id}/deactivate` | CompanyDetailPage | L | 2 | Not started |
| Blacklist company | A | `POST /api/companies/{id}/blacklist` | CompanyDetailPage (admin only) | L | 5 | Not started |

---

## 4. Job Posting Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Create job posting (DRAFT) | PO | `POST /api/job-postings` | CreateJobPostingPage | M | 3 | Not started |
| List open job postings | S | `GET /api/job-postings` | JobPostingsPage | L | 3 | Not started |
| View job posting detail | S | `GET /api/job-postings/{id}` | JobPostingDetailPage | L | 3 | Not started |
| List all job postings (officer) | PO | `GET /api/job-postings` (filter client-side or via status) | JobPostingsManagePage | M | 3 | Not started |
| Update DRAFT posting | PO | `PUT /api/job-postings/{id}` | JobPostingManageDetailPage | M | 3 | Not started |
| Open posting (DRAFT → OPEN) | PO | `POST /api/job-postings/{id}/open` | JobPostingManageDetailPage | L | 3 | Not started |
| Close posting (OPEN → CLOSED) | PO | `POST /api/job-postings/{id}/close` | JobPostingManageDetailPage | L | 3 | Not started |
| Cancel posting | PO | `POST /api/job-postings/{id}/cancel` | JobPostingManageDetailPage | L | 3 | Not started |
| Add required skill to posting | PO | NOT AVAILABLE (MEDIUM-6) | — | — | BLOCKED | Needs backend |
| Add eligible branch to posting | PO | NOT AVAILABLE (MEDIUM-6) | — | — | BLOCKED | Needs backend |

---

## 5. Job Application Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Apply to job posting | S | `POST /api/applications` | JobPostingDetailPage | M | 3 | Not started |
| View own applications | S | `GET /api/applications/my` | MyApplicationsPage | L | 3 | Not started |
| Withdraw application | S | `POST /api/applications/{id}/withdraw` | MyApplicationsPage | L | 3 | Not started |
| List all applications (paginated) | PO | `GET /api/applications` | ApplicationsListPage | M | 3 | Not started |
| View application by ID | PO | `GET /api/applications/{id}` | ApplicationDetailPage | L | 3 | Not started |
| View applications by student | PO | `GET /api/applications/student/{studentId}` | StudentDetailPage | L | 3 | Not started |
| Update application status | PO | `PUT /api/applications/{id}/status` | ApplicationDetailPage | M | 3 | Not started |

---

## 6. Offer Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Create offer | PO | `POST /api/offers` | ApplicationDetailPage | M | 4 | Not started |
| List all offers (paginated) | PO | `GET /api/offers` | OffersListPage | L | 4 | Not started |
| View offer by ID | PO | `GET /api/offers/{id}` | OffersListPage detail | L | 4 | Not started |
| View own offers | S | `GET /api/offers/my` | MyOffersPage | L | 4 | Not started |
| Accept offer | S | `POST /api/offers/{id}/accept` | MyOffersPage | L | 4 | Not started |
| Reject offer | S | `POST /api/offers/{id}/reject` | MyOffersPage | L | 4 | Not started |
| Expire offer | PO | `POST /api/offers/{id}/expire` | OffersListPage | L | 4 | Not started |

---

## 7. Certificate Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Submit certificate | S | `POST /api/certificates` | SubmitCertificatePage | H | 4 | Not started |
| View own certificates | S | `GET /api/certificates/my` | MyCertificatesPage | L | 4 | Not started |
| List all certificates (paginated) | PO | `GET /api/certificates` | CertificatesQueuePage | M | 4 | Not started |
| View certificate by ID | PO | `GET /api/certificates/{id}` | CertificateDetail | L | 4 | Not started |
| View certificates by student | PO | `GET /api/certificates/student/{studentId}` | StudentDetailPage | L | 4 | Not started |
| Verify certificate | PO | `POST /api/certificates/{id}/verify` | CertificatesQueuePage | L | 4 | Not started |
| Reject certificate | PO | `POST /api/certificates/{id}/reject` | CertificatesQueuePage | L | 4 | Not started |

---

## 8. File Upload Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Upload file (PDF/image) | S, PO, A | `POST /api/files/upload` | FileUploadWidget | H | 4 | Not started |
| Download file | Any auth | `GET /api/files/{id}` | CertificateCard / inline | L | 4 | Not started |
| Delete file | PO, A | `DELETE /api/files/{id}` | Admin only | L | 5 | Not started |

---

## 9. Skills Features (BLOCKED — No API)

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| List all skills | Any auth | NOT AVAILABLE (CRITICAL-2) | SkillsPage | L | BLOCKED | Needs backend |
| Create skill | PO | NOT AVAILABLE | SkillsPage | L | BLOCKED | Needs backend |
| Verify skill | PO | NOT AVAILABLE | SkillsPage | L | BLOCKED | Needs backend |
| Assign skill to student | PO | NOT AVAILABLE (frontend) | StudentDetailPage | L | BLOCKED | Needs backend list |
| Filter by skill category | PO | NOT AVAILABLE | SkillsPage | L | BLOCKED | Needs backend |

---

## 10. Branch Features (BLOCKED — No API)

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| List all branches | Any auth | NOT AVAILABLE (CRITICAL-1) | BranchesPage | L | BLOCKED | Needs backend |
| Create branch | PO | NOT AVAILABLE | BranchesPage | L | BLOCKED | Needs backend |
| Update branch | PO | NOT AVAILABLE | BranchesPage | L | BLOCKED | Needs backend |
| Activate/deactivate branch | PO | NOT AVAILABLE | BranchesPage | L | BLOCKED | Needs backend |

---

## 11. Analytics / Dashboard Features (BLOCKED — No API)

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Platform summary stats | PO | NOT AVAILABLE (CRITICAL-3) | OfficerDashboardPage | M | BLOCKED | Needs backend |
| Placement rate chart | PO | NOT AVAILABLE | OfficerDashboardPage | H | BLOCKED | Needs backend |
| Applications by status chart | PO | NOT AVAILABLE | OfficerDashboardPage | M | BLOCKED | Needs backend |
| Placements over time | PO | NOT AVAILABLE | OfficerDashboardPage | H | BLOCKED | Needs backend |

---

## 12. Notification Features (BLOCKED — No API)

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| In-app notification inbox | Any auth | NOT AVAILABLE | NotificationPanel | M | BLOCKED | Needs backend |
| Notification bell count | Any auth | NOT AVAILABLE | DashboardLayout header | L | BLOCKED | Needs backend |

---

## 13. Admin Features

| Feature | Role Access | Backend Endpoint | Frontend Page/Component | Complexity | Phase | Status |
|---|---|---|---|---|---|---|
| Blacklist company | A | `POST /api/companies/{id}/blacklist` | CompanyDetailPage | L | 5 | Not started |
| Create admin/officer user | A | `POST /auth/register` (role=ROLE_ADMIN/PO) | UserManagementPage | M | 5 | Workaround only |
| View audit logs | A | NOT AVAILABLE (MEDIUM-7) | AuditLogsPage | M | BLOCKED | Needs backend |
| View system metrics | A | `GET /actuator/metrics` | SystemHealthPage | M | 5 | Not started |
| Admin-only test endpoint | A | `GET /api/users/admin-only` | — | L | — | Dev/test only |

---

## Feature Count Summary

| Phase | Features | Blocked |
|---|---|---|
| Phase 1 | 8 | 0 |
| Phase 2 | 18 | 0 |
| Phase 3 | 14 | 2 (skill/branch on job posting) |
| Phase 4 | 16 | 0 |
| Phase 5 | 5 | 2 (audit, analytics) |
| BLOCKED | — | 17 |
| **Total** | **61** | **19** |
