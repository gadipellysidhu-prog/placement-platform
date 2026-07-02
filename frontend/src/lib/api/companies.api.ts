import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface CompanyResponse {
  id: string
  name: string
  website: string | null
  industry: string | null
  description: string | null
  logoUrl: string | null
  status: 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED'
  createdAt: string
  updatedAt: string
}

export interface CreateCompanyRequest {
  name: string
  website?: string
  industry?: string
  description?: string
}

export interface UpdateCompanyRequest {
  name: string
  website?: string
  industry?: string
  description?: string
  logoUrl?: string
}

export const companiesApi = {
  create: (data: CreateCompanyRequest) =>
    apiClient.post<CompanyResponse>('/api/companies', data).then((r) => r.data),

  list: (params?: PageParams) =>
    apiClient.get<Page<CompanyResponse>>('/api/companies', { params }).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<CompanyResponse>(`/api/companies/${id}`).then((r) => r.data),

  update: (id: string, data: UpdateCompanyRequest) =>
    apiClient.put<CompanyResponse>(`/api/companies/${id}`, data).then((r) => r.data),

  activate: (id: string) =>
    apiClient.post<CompanyResponse>(`/api/companies/${id}/activate`).then((r) => r.data),

  deactivate: (id: string) =>
    apiClient.post<CompanyResponse>(`/api/companies/${id}/deactivate`).then((r) => r.data),

  blacklist: (id: string) =>
    apiClient.post<CompanyResponse>(`/api/companies/${id}/blacklist`).then((r) => r.data),
}
