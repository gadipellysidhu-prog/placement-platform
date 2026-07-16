import { apiClient } from '@/lib/axios'
import type { Page, PageParams } from '@/types'

/** Declared type of a setting's stored string value (backend `SettingValueType`). */
export type SettingValueType = 'STRING' | 'INTEGER' | 'LONG' | 'BOOLEAN' | 'DECIMAL' | 'JSON'

export interface SettingResponse {
  id: string
  settingKey: string
  settingValue: string | null
  valueType: SettingValueType
  category: string | null
  description: string | null
  /** Null for a global setting; otherwise the owning academic year. */
  academicYearId: string | null
  createdAt: string
  updatedAt: string
}

export interface ListSettingsParams extends PageParams {
  category?: string
}

/**
 * Create-or-update payload. The backend upserts on (settingKey + academicYearId),
 * so there is no separate create endpoint — creating and editing post the same shape.
 */
export interface SettingUpsertRequest {
  settingKey: string
  settingValue?: string
  valueType: SettingValueType
  category?: string
  description?: string
  academicYearId?: string | null
}

/** `/api/admin/settings/**` — ADMIN only (class-level `@PreAuthorize("hasRole('ADMIN')")`). */
export const settingsApi = {
  list: (params?: ListSettingsParams) =>
    apiClient.get<Page<SettingResponse>>('/api/admin/settings', { params }).then((r) => r.data),

  getById: (id: string) =>
    apiClient.get<SettingResponse>(`/api/admin/settings/${id}`).then((r) => r.data),

  /** Upsert by key + academic year. Returns 200 for both create and update. */
  upsert: (data: SettingUpsertRequest) =>
    apiClient.put<SettingResponse>('/api/admin/settings', data).then((r) => r.data),

  delete: (id: string) => apiClient.delete<void>(`/api/admin/settings/${id}`).then((r) => r.data),
}
