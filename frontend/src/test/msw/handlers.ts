import { http, HttpResponse } from 'msw'
import { API_BASE_URL } from '../constants'
import type { AuthTokens, User, Page } from '@/types'
import type { StudentResponse, CertificateResponse, OfferResponse, FileResponse } from '@/lib/api'

/** Canonical fixtures reused across tests so assertions stay in sync with handlers. */
export const mockStudentUser: User = {
  email: 'student@university.edu',
  role: 'ROLE_STUDENT',
}

export const mockOfficerUser: User = {
  email: 'officer@university.edu',
  role: 'ROLE_PLACEMENT_OFFICER',
}

export const mockTokens: AuthTokens = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  accessTokenExpiresIn: 900,
  tokenType: 'Bearer',
}

/** Authenticated student's own profile (GET /api/students/me). */
export const mockStudentProfile: StudentResponse = {
  id: 'student-1',
  userId: 'user-1',
  userEmail: 'student@university.edu',
  rollNumber: 'CS2021001',
  branchId: 'branch-1',
  branchName: 'Computer Science',
  cgpa: 8.5,
  currentYear: 4,
  placementEligible: true,
  status: 'ACTIVE',
  skillNames: [],
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

export const mockFileResponse: FileResponse = {
  id: 'file-1',
  filename: 'certificate.pdf',
  contentType: 'application/pdf',
  sizeBytes: 2048,
  sha256Hash: 'abc123',
  scanStatus: 'CLEAN',
  quarantined: false,
  uploadedBy: 'student@university.edu',
  uploadedAt: '2026-02-01T00:00:00Z',
}

export const mockCertificate: CertificateResponse = {
  id: 'cert-1',
  studentId: 'student-1',
  studentRollNumber: 'CS2021001',
  skillId: null,
  skillName: null,
  name: 'AWS Certified Developer',
  issuingOrganization: 'Amazon Web Services',
  fileKey: 'file-1',
  verificationStatus: 'PENDING',
  createdAt: '2026-02-01T00:00:00Z',
  updatedAt: '2026-02-01T00:00:00Z',
}

export const mockOffer: OfferResponse = {
  id: 'offer-1',
  applicationId: 'app-1',
  studentId: 'student-1',
  studentRollNumber: 'CS2021001',
  companyId: 'company-1',
  companyName: 'Acme Corp',
  status: 'PENDING',
  ctc: 12.5,
  joiningDate: '2026-07-01',
  createdAt: '2026-02-01T00:00:00Z',
  updatedAt: '2026-02-01T00:00:00Z',
}

/** Wrap a content array in a minimal Spring Data Page envelope. */
export function pageOf<T>(content: T[]): Page<T> {
  return {
    content,
    pageable: { pageNumber: 0, pageSize: 10, sort: { sorted: false } },
    totalElements: content.length,
    totalPages: 1,
    last: true,
    first: true,
    numberOfElements: content.length,
    size: 10,
    number: 0,
    empty: content.length === 0,
  }
}

/**
 * Default happy-path handlers. Individual tests override these with `server.use(...)`
 * to exercise failure, 401/refresh, and edge-case flows against the real axios client.
 */
export const handlers = [
  http.post(`${API_BASE_URL}/auth/login`, () => HttpResponse.json(mockTokens)),

  http.post(`${API_BASE_URL}/auth/refresh`, () => HttpResponse.json(mockTokens)),

  http.post(`${API_BASE_URL}/auth/logout`, () => new HttpResponse(null, { status: 204 })),

  // ── Auth journeys ────────────────────────────────────────────────────────
  // The backend returns a MessageResponse envelope; success statuses mirror
  // AuthController (200 for confirm/reset/accept, 202 for the "send email" pair).
  http.post(`${API_BASE_URL}/auth/verify-email/confirm`, () =>
    HttpResponse.json({ message: 'Email verified successfully.' }),
  ),

  http.post(`${API_BASE_URL}/auth/resend-verification`, () =>
    HttpResponse.json(
      { message: 'If an account exists for that email, a message has been sent.' },
      { status: 202 },
    ),
  ),

  http.post(`${API_BASE_URL}/auth/forgot-password`, () =>
    HttpResponse.json(
      { message: 'If an account exists for that email, a message has been sent.' },
      { status: 202 },
    ),
  ),

  http.post(`${API_BASE_URL}/auth/reset-password`, () =>
    HttpResponse.json({ message: 'Password has been reset. Please sign in again.' }),
  ),

  http.post(`${API_BASE_URL}/auth/accept-invitation`, () =>
    HttpResponse.json({
      message: 'Invitation accepted. Your account is now active — please sign in.',
    }),
  ),

  http.get(`${API_BASE_URL}/api/users/me`, () => HttpResponse.json(mockStudentUser)),

  // ── Student profile ──────────────────────────────────────────────────────
  http.get(`${API_BASE_URL}/api/students/me`, () => HttpResponse.json(mockStudentProfile)),

  // ── File pipeline ────────────────────────────────────────────────────────
  http.post(`${API_BASE_URL}/api/files/upload`, () =>
    HttpResponse.json(mockFileResponse, { status: 201 }),
  ),
  http.get(`${API_BASE_URL}/api/files/:id`, () =>
    HttpResponse.arrayBuffer(new ArrayBuffer(8), {
      headers: { 'Content-Type': 'application/pdf' },
    }),
  ),
  http.delete(`${API_BASE_URL}/api/files/:id`, () => new HttpResponse(null, { status: 204 })),

  // ── Certificates ─────────────────────────────────────────────────────────
  http.get(`${API_BASE_URL}/api/certificates/my`, () => HttpResponse.json([mockCertificate])),
  http.get(`${API_BASE_URL}/api/certificates`, () => HttpResponse.json(pageOf([mockCertificate]))),
  http.post(`${API_BASE_URL}/api/certificates`, () =>
    HttpResponse.json(mockCertificate, { status: 201 }),
  ),
  http.post(`${API_BASE_URL}/api/certificates/:id/verify`, () =>
    HttpResponse.json({ ...mockCertificate, verificationStatus: 'VERIFIED' }),
  ),
  http.post(`${API_BASE_URL}/api/certificates/:id/reject`, () =>
    HttpResponse.json({ ...mockCertificate, verificationStatus: 'REJECTED' }),
  ),

  // ── Offers ───────────────────────────────────────────────────────────────
  http.get(`${API_BASE_URL}/api/offers/my`, () => HttpResponse.json([mockOffer])),
  http.get(`${API_BASE_URL}/api/offers`, () => HttpResponse.json(pageOf([mockOffer]))),
  http.post(`${API_BASE_URL}/api/offers`, () => HttpResponse.json(mockOffer, { status: 201 })),
  http.post(`${API_BASE_URL}/api/offers/:id/accept`, () =>
    HttpResponse.json({ ...mockOffer, status: 'ACCEPTED' }),
  ),
  http.post(`${API_BASE_URL}/api/offers/:id/reject`, () =>
    HttpResponse.json({ ...mockOffer, status: 'REJECTED' }),
  ),
  http.post(`${API_BASE_URL}/api/offers/:id/expire`, () =>
    HttpResponse.json({ ...mockOffer, status: 'EXPIRED' }),
  ),
]
