import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface StudentResponse {
  id: string
  userId: string
  userEmail: string
  rollNumber: string
  branchId: string | null
  branchName: string | null
  cgpa: number | null
  currentYear: number
  placementEligible: boolean
  status: 'ACTIVE' | 'PLACED' | 'OPTED_OUT' | 'GRADUATED' | 'BLOCKED'
  skillNames: string[]
  createdAt: string
  updatedAt: string
}

export interface CreateStudentRequest {
  userId: string
  rollNumber: string
  branchId?: string
  currentYear: number
}

export interface UpdateStudentRequest {
  branchId?: string
  cgpa?: number
  currentYear: number
}

export interface UpdateStudentStatusRequest {
  status: StudentResponse['status']
}

/** A registered ROLE_STUDENT account with no profile yet — awaiting officer approval. */
export interface PendingRegistrationResponse {
  userId: string
  email: string
  displayName: string
  emailVerified: boolean
  createdAt: string
}

/** Profile details assigned when approving a pending registration. */
export interface ApproveRegistrationRequest {
  rollNumber: string
  branchId?: string
  currentYear: number
}

export const studentsApi = {
  create: (data: CreateStudentRequest) =>
    apiClient.post<StudentResponse>('/api/students', data).then((r) => r.data),

  list: (params?: PageParams) =>
    apiClient.get<Page<StudentResponse>>('/api/students', { params }).then((r) => r.data),

  me: () => apiClient.get<StudentResponse>('/api/students/me').then((r) => r.data),

  /** Registrations awaiting approval (paginated). */
  listPending: (params?: PageParams) =>
    apiClient
      .get<Page<PendingRegistrationResponse>>('/api/students/pending', { params })
      .then((r) => r.data),

  /** Approve a pending registration — creates and links the student profile. */
  approve: (userId: string, data: ApproveRegistrationRequest) =>
    apiClient.post<StudentResponse>(`/api/students/approvals/${userId}`, data).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<StudentResponse>(`/api/students/${id}`).then((r) => r.data),

  update: (id: string, data: UpdateStudentRequest) =>
    apiClient.put<StudentResponse>(`/api/students/${id}`, data).then((r) => r.data),

  updateStatus: (id: string, data: UpdateStudentStatusRequest) =>
    apiClient.put<StudentResponse>(`/api/students/${id}/status`, data).then((r) => r.data),

  updateEligibility: (id: string) =>
    apiClient.put<StudentResponse>(`/api/students/${id}/eligibility`).then((r) => r.data),

  addSkill: (id: string, skillId: string) =>
    apiClient.post<StudentResponse>(`/api/students/${id}/skills/${skillId}`).then((r) => r.data),

  removeSkill: (id: string, skillId: string) =>
    apiClient.delete<StudentResponse>(`/api/students/${id}/skills/${skillId}`).then((r) => r.data),
}
