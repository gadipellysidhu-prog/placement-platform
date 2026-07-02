import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface CertificateResponse {
  id: string
  studentId: string
  studentRollNumber: string
  skillId: string | null
  skillName: string | null
  name: string
  issuingOrganization: string | null
  fileKey: string | null
  verificationStatus: 'PENDING' | 'VERIFIED' | 'REJECTED'
  createdAt: string
  updatedAt: string
}

export interface CreateCertificateRequest {
  studentId: string
  name: string
  issuingOrganization?: string
  skillId?: string
  fileKey?: string
}

export const certificatesApi = {
  create: (data: CreateCertificateRequest) =>
    apiClient.post<CertificateResponse>('/api/certificates', data).then((r) => r.data),

  list: (params?: PageParams) =>
    apiClient.get<Page<CertificateResponse>>('/api/certificates', { params }).then((r) => r.data),

  /** Returns unbounded list — pagination gap: see BACKEND_COMPATIBILITY.md HIGH-2 */
  my: () => apiClient.get<CertificateResponse[]>('/api/certificates/my').then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<CertificateResponse>(`/api/certificates/${id}`).then((r) => r.data),

  byStudent: (studentId: string) =>
    apiClient
      .get<CertificateResponse[]>(`/api/certificates/student/${studentId}`)
      .then((r) => r.data),

  verify: (id: string) =>
    apiClient.post<CertificateResponse>(`/api/certificates/${id}/verify`).then((r) => r.data),

  reject: (id: string) =>
    apiClient.post<CertificateResponse>(`/api/certificates/${id}/reject`).then((r) => r.data),
}
