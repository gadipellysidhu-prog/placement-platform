import { apiClient } from '@/lib/axios'

/** Pipeline stages surfaced live by the backend run row. */
export type RunStatus =
  | 'PENDING'
  | 'FETCHING'
  | 'EXTRACTING'
  | 'NORMALIZING'
  | 'TAGGING'
  | 'PREDICTING_BRANCHES'
  | 'COMPLETED'
  | 'FAILED'

export interface JobIntelligenceRun {
  id: string
  jobPostingId: string
  officialUrl: string
  status: RunStatus
  terminal: boolean
  provider: string | null
  model: string | null
  confidence: number | null
  skillsExtracted: number
  skillsCreated: number
  skillsTagged: number
  predictedBranches: string[]
  warnings: string[]
  errorMessage: string | null
  retryCount: number
  startedAt: string | null
  completedAt: string | null
  durationMs: number | null
  createdAt: string
}

export interface StartRunRequest {
  jobPostingId: string
  officialUrl: string
}

export const jobIntelligenceApi = {
  /** Officer only. 202 — extraction continues asynchronously; poll the run. */
  startRun: (data: StartRunRequest) =>
    apiClient.post<JobIntelligenceRun>('/api/job-intelligence/runs', data).then((r) => r.data),

  getRun: (id: string) =>
    apiClient.get<JobIntelligenceRun>(`/api/job-intelligence/runs/${id}`).then((r) => r.data),

  /** Latest run for a posting; 404 when the posting has never been analyzed. */
  latestForPosting: (postingId: string) =>
    apiClient
      .get<JobIntelligenceRun>(`/api/job-intelligence/postings/${postingId}/latest`)
      .then((r) => r.data),

  /** Officer only. Re-enters the failed run (skills are never duplicated). */
  retry: (id: string) =>
    apiClient
      .post<JobIntelligenceRun>(`/api/job-intelligence/runs/${id}/retry`)
      .then((r) => r.data),
}
