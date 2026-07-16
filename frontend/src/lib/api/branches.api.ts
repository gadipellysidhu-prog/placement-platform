import { apiClient } from '@/lib/axios'

export interface BranchResponse {
  id: string
  name: string
  code: string | null
  description: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateBranchRequest {
  name: string
  code?: string
  description?: string
}

export interface UpdateBranchRequest {
  name: string
  code?: string
  description?: string
}

export interface ListBranchesParams {
  /**
   * Defaults to true server-side (active branches only). Administration passes false
   * to include deactivated branches, which would otherwise be unreachable.
   */
  activeOnly?: boolean
}

export const branchesApi = {
  list: (params?: ListBranchesParams) =>
    apiClient.get<BranchResponse[]>('/api/branches', { params }).then((r) => r.data),

  getById: (id: string) => apiClient.get<BranchResponse>(`/api/branches/${id}`).then((r) => r.data),

  create: (data: CreateBranchRequest) =>
    apiClient.post<BranchResponse>('/api/branches', data).then((r) => r.data),

  update: (id: string, data: UpdateBranchRequest) =>
    apiClient.put<BranchResponse>(`/api/branches/${id}`, data).then((r) => r.data),

  activate: (id: string) =>
    apiClient.post<BranchResponse>(`/api/branches/${id}/activate`).then((r) => r.data),

  deactivate: (id: string) =>
    apiClient.post<BranchResponse>(`/api/branches/${id}/deactivate`).then((r) => r.data),
}
