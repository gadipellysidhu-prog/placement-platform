import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

export interface AcademicYearResponse {
  id: string
  label: string
  /** ISO date (yyyy-MM-dd) — LocalDate on the backend, not an instant. */
  startDate: string
  endDate: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateAcademicYearRequest {
  label: string
  startDate: string
  endDate: string
}

/** Only the dates are updatable — the backend's update request carries no label. */
export interface UpdateAcademicYearRequest {
  startDate: string
  endDate: string
}

/**
 * `/api/academic-years/**` — note the split authorization on the controller:
 * reads require PLACEMENT_OFFICER, while create/update/activate require ADMIN.
 *
 * Exactly one year is active at a time; activating one deactivates the other,
 * server-side.
 */
export const academicYearsApi = {
  list: (params?: PageParams) =>
    apiClient
      .get<Page<AcademicYearResponse>>('/api/academic-years', { params })
      .then((r) => r.data),

  getActive: () =>
    apiClient.get<AcademicYearResponse>('/api/academic-years/active').then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<AcademicYearResponse>(`/api/academic-years/${id}`).then((r) => r.data),

  /** Admin only → 201. */
  create: (data: CreateAcademicYearRequest) =>
    apiClient.post<AcademicYearResponse>('/api/academic-years', data).then((r) => r.data),

  /** Admin only. Updates the date range; the label is immutable. */
  update: (id: string, data: UpdateAcademicYearRequest) =>
    apiClient.put<AcademicYearResponse>(`/api/academic-years/${id}`, data).then((r) => r.data),

  /** Admin only. Deactivates whichever year was previously active. */
  activate: (id: string) =>
    apiClient.post<AcademicYearResponse>(`/api/academic-years/${id}/activate`).then((r) => r.data),
}
