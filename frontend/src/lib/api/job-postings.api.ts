import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface JobPostingResponse {
  id: string
  companyId: string
  companyName: string
  title: string
  description: string | null
  ctcMin: number | null
  ctcMax: number | null
  status: 'DRAFT' | 'OPEN' | 'CLOSED' | 'CANCELLED'
  applicationDeadline: string | null
  offerLimit: number
  createdAt: string
  updatedAt: string
}

export interface CreateJobPostingRequest {
  companyId: string
  title: string
  description?: string
  ctcMin?: number
  ctcMax?: number
  applicationDeadline?: string
  offerLimit: number
}

export interface UpdateJobPostingRequest {
  companyId: string
  title: string
  description?: string
  ctcMin?: number
  ctcMax?: number
  applicationDeadline?: string
  offerLimit: number
}

export const jobPostingsApi = {
  create: (data: CreateJobPostingRequest) =>
    apiClient.post<JobPostingResponse>('/api/job-postings', data).then((r) => r.data),

  /** Returns OPEN postings only — see BACKEND_COMPATIBILITY.md HIGH-4 for officer listing gap */
  list: (params?: PageParams) =>
    apiClient.get<Page<JobPostingResponse>>('/api/job-postings', { params }).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<JobPostingResponse>(`/api/job-postings/${id}`).then((r) => r.data),

  update: (id: string, data: UpdateJobPostingRequest) =>
    apiClient.put<JobPostingResponse>(`/api/job-postings/${id}`, data).then((r) => r.data),

  open: (id: string) =>
    apiClient.post<JobPostingResponse>(`/api/job-postings/${id}/open`).then((r) => r.data),

  close: (id: string) =>
    apiClient.post<JobPostingResponse>(`/api/job-postings/${id}/close`).then((r) => r.data),

  cancel: (id: string) =>
    apiClient.post<JobPostingResponse>(`/api/job-postings/${id}/cancel`).then((r) => r.data),
}
