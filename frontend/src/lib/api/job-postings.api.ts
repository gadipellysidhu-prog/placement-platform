import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export type JobPostingStatus = 'DRAFT' | 'OPEN' | 'CLOSED' | 'CANCELLED'

/** Skill/branch references embedded in detail responses (null on list/summary responses). */
export interface JobPostingSkillRef {
  id: string
  name: string
}

export interface JobPostingBranchRef {
  id: string
  name: string
}

export interface JobPostingResponse {
  id: string
  companyId: string
  companyName: string
  title: string
  description: string | null
  ctcMin: number | null
  ctcMax: number | null
  status: JobPostingStatus
  applicationDeadline: string | null
  offerLimit: number
  /** Populated on detail responses (GET /{id} and tagging mutations); null on list responses. */
  requiredSkills: JobPostingSkillRef[] | null
  /** Populated on detail responses (GET /{id} and tagging mutations); null on list responses. */
  eligibleBranches: JobPostingBranchRef[] | null
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

/**
 * Update payload for a DRAFT posting. The backend `JobPostingUpdateRequest` does NOT
 * accept `companyId` — the owning company is fixed at creation time.
 */
export interface UpdateJobPostingRequest {
  title: string
  description?: string
  ctcMin?: number
  ctcMax?: number
  applicationDeadline?: string
  offerLimit: number
}

/** Student-facing browse params: OPEN postings only, optional case-insensitive title filter. */
export interface ListJobPostingsParams extends PageParams {
  title?: string
}

/** Officer-facing manage params: all statuses, optional single-status filter. */
export interface ManageJobPostingsParams extends PageParams {
  status?: JobPostingStatus
}

export const jobPostingsApi = {
  /** Officer only. Creates a posting in DRAFT status → 201. */
  create: (data: CreateJobPostingRequest) =>
    apiClient.post<JobPostingResponse>('/api/job-postings', data).then((r) => r.data),

  /** Student browse. Returns OPEN postings only, paginated, optionally filtered by title. */
  list: (params?: ListJobPostingsParams) =>
    apiClient.get<Page<JobPostingResponse>>('/api/job-postings', { params }).then((r) => r.data),

  /** Officer manage. Returns postings across all statuses, optionally filtered by status. */
  listManage: (params?: ManageJobPostingsParams) =>
    apiClient
      .get<Page<JobPostingResponse>>('/api/job-postings/manage', { params })
      .then((r) => r.data),

  /** Detail projection including required skills and eligible branches. */
  getById: (id: string) =>
    apiClient.get<JobPostingResponse>(`/api/job-postings/${id}`).then((r) => r.data),

  /** Officer only. Update is allowed only while the posting is in DRAFT. */
  update: (id: string, data: UpdateJobPostingRequest) =>
    apiClient.put<JobPostingResponse>(`/api/job-postings/${id}`, data).then((r) => r.data),

  open: (id: string) =>
    apiClient.post<JobPostingResponse>(`/api/job-postings/${id}/open`).then((r) => r.data),

  close: (id: string) =>
    apiClient.post<JobPostingResponse>(`/api/job-postings/${id}/close`).then((r) => r.data),

  cancel: (id: string) =>
    apiClient.post<JobPostingResponse>(`/api/job-postings/${id}/cancel`).then((r) => r.data),

  /** Officer only. Returns the detailed posting (skills + branches populated). */
  addSkill: (id: string, skillId: string) =>
    apiClient
      .post<JobPostingResponse>(`/api/job-postings/${id}/skills/${skillId}`)
      .then((r) => r.data),

  removeSkill: (id: string, skillId: string) =>
    apiClient
      .delete<JobPostingResponse>(`/api/job-postings/${id}/skills/${skillId}`)
      .then((r) => r.data),

  addBranch: (id: string, branchId: string) =>
    apiClient
      .post<JobPostingResponse>(`/api/job-postings/${id}/branches/${branchId}`)
      .then((r) => r.data),

  removeBranch: (id: string, branchId: string) =>
    apiClient
      .delete<JobPostingResponse>(`/api/job-postings/${id}/branches/${branchId}`)
      .then((r) => r.data),
}
