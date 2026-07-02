import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface OfferResponse {
  id: string
  applicationId: string
  studentId: string
  studentRollNumber: string
  companyId: string
  companyName: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED'
  ctc: number | null
  joiningDate: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateOfferRequest {
  applicationId: string
  ctc?: number
  joiningDate?: string
}

export const offersApi = {
  create: (data: CreateOfferRequest) =>
    apiClient.post<OfferResponse>('/api/offers', data).then((r) => r.data),

  list: (params?: PageParams) =>
    apiClient.get<Page<OfferResponse>>('/api/offers', { params }).then((r) => r.data),

  /** Returns unbounded list — pagination gap: see BACKEND_COMPATIBILITY.md HIGH-2 */
  my: () => apiClient.get<OfferResponse[]>('/api/offers/my').then((r) => r.data),

  getById: (id: string) => apiClient.get<OfferResponse>(`/api/offers/${id}`).then((r) => r.data),

  accept: (id: string) =>
    apiClient.post<OfferResponse>(`/api/offers/${id}/accept`).then((r) => r.data),

  reject: (id: string) =>
    apiClient.post<OfferResponse>(`/api/offers/${id}/reject`).then((r) => r.data),

  expire: (id: string) =>
    apiClient.post<OfferResponse>(`/api/offers/${id}/expire`).then((r) => r.data),
}
