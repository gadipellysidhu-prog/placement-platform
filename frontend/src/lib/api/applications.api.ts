import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface JobApplicationResponse {
  id: string
  studentId: string
  studentRollNumber: string
  jobPostingId: string
  jobPostingTitle: string
  companyId: string
  companyName: string
  status: 'APPLIED' | 'SHORTLISTED' | 'INTERVIEWED' | 'OFFERED' | 'REJECTED' | 'WITHDRAWN'
  appliedAt: string
  createdAt: string
  updatedAt: string
}

export interface CreateApplicationRequest {
  studentId: string
  jobPostingId: string
}

export interface UpdateApplicationStatusRequest {
  status: 'APPLIED' | 'SHORTLISTED' | 'INTERVIEWED' | 'OFFERED' | 'REJECTED'
}

export const applicationsApi = {
  create: (data: CreateApplicationRequest) =>
    apiClient.post<JobApplicationResponse>('/api/applications', data).then((r) => r.data),

  list: (params?: PageParams) =>
    apiClient
      .get<Page<JobApplicationResponse>>('/api/applications', { params })
      .then((r) => r.data),

  /** Returns unbounded list — pagination gap: see BACKEND_COMPATIBILITY.md HIGH-2 */
  my: () => apiClient.get<JobApplicationResponse[]>('/api/applications/my').then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<JobApplicationResponse>(`/api/applications/${id}`).then((r) => r.data),

  byStudent: (studentId: string) =>
    apiClient
      .get<JobApplicationResponse[]>(`/api/applications/student/${studentId}`)
      .then((r) => r.data),

  updateStatus: (id: string, data: UpdateApplicationStatusRequest) =>
    apiClient
      .put<JobApplicationResponse>(`/api/applications/${id}/status`, data)
      .then((r) => r.data),

  withdraw: (id: string) =>
    apiClient.post<JobApplicationResponse>(`/api/applications/${id}/withdraw`).then((r) => r.data),
}
