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

export const studentsApi = {
  create: (data: CreateStudentRequest) =>
    apiClient.post<StudentResponse>('/api/students', data).then((r) => r.data),

  list: (params?: PageParams) =>
    apiClient.get<Page<StudentResponse>>('/api/students', { params }).then((r) => r.data),

  me: () => apiClient.get<StudentResponse>('/api/students/me').then((r) => r.data),

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
