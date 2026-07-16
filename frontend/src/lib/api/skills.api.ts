import { apiClient } from '@/lib/axios'

/** How a catalog skill came into existence (backend `SkillCreatedSource`). */
export type SkillCreatedSource = 'SEED' | 'MANUAL' | 'AI'

export interface SkillResponse {
  id: string
  name: string
  category: string | null
  verified: boolean
  description: string | null
  parentCategory: string | null
  subcategory: string | null
  popularityScore: number
  industryTags: string | null
  active: boolean
  createdSource: SkillCreatedSource
  aiConfidence: number | null
  /** Populated on detail responses (GET /{id}); null on list responses. */
  aliases: string[] | null
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

export interface SkillAliasResponse {
  id: string
  skillId: string
  alias: string
}

export interface CreateSkillAliasRequest {
  alias: string
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

  /** Detail projection — the only response with `aliases` populated. */
  getById: (id: string) => apiClient.get<SkillResponse>(`/api/skills/${id}`).then((r) => r.data),

  listAliases: (id: string) =>
    apiClient.get<SkillAliasResponse[]>(`/api/skills/${id}/aliases`).then((r) => r.data),

  addAlias: (id: string, data: CreateSkillAliasRequest) =>
    apiClient.post<SkillAliasResponse>(`/api/skills/${id}/aliases`, data).then((r) => r.data),

  removeAlias: (id: string, aliasId: string) =>
    apiClient.delete<void>(`/api/skills/${id}/aliases/${aliasId}`).then((r) => r.data),

  create: (data: CreateSkillRequest) =>
    apiClient.post<SkillResponse>('/api/skills', data).then((r) => r.data),

  update: (id: string, data: UpdateSkillRequest) =>
    apiClient.put<SkillResponse>(`/api/skills/${id}`, data).then((r) => r.data),

  verify: (id: string) =>
    apiClient.post<SkillResponse>(`/api/skills/${id}/verify`).then((r) => r.data),
}
