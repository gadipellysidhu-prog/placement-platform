import { apiClient } from '@/lib/axios'

export interface SkillResponse {
  id: string
  name: string
  category: string | null
  verified: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateSkillRequest {
  name: string
  category?: string
}

export interface UpdateSkillRequest {
  name: string
  category?: string
}

export interface ListSkillsParams {
  category?: string
  verified?: boolean
}

/** Ranked catalog search result (exact > alias > partial > fuzzy). */
export interface SkillSearchResult {
  id: string
  name: string
  category: string | null
  parentCategory: string | null
  popularityScore: number
  matchType: 'EXACT' | 'ALIAS' | 'PARTIAL' | 'FUZZY'
  score: number
}

export const skillsApi = {
  list: (params?: ListSkillsParams) =>
    apiClient.get<SkillResponse[]>('/api/skills', { params }).then((r) => r.data),

  /** Intelligent catalog search: names, aliases, abbreviations, fuzzy — server-ranked. */
  search: (q: string, limit = 20) =>
    apiClient
      .get<SkillSearchResult[]>('/api/skills/search', { params: { q, limit } })
      .then((r) => r.data),

  getById: (id: string) => apiClient.get<SkillResponse>(`/api/skills/${id}`).then((r) => r.data),

  create: (data: CreateSkillRequest) =>
    apiClient.post<SkillResponse>('/api/skills', data).then((r) => r.data),

  update: (id: string, data: UpdateSkillRequest) =>
    apiClient.put<SkillResponse>(`/api/skills/${id}`, data).then((r) => r.data),

  verify: (id: string) =>
    apiClient.post<SkillResponse>(`/api/skills/${id}/verify`).then((r) => r.data),
}
