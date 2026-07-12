import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { jobPostingsApi, queryKeys } from '@/lib/api'
import type {
  CreateJobPostingRequest,
  JobPostingResponse,
  ListJobPostingsParams,
  ManageJobPostingsParams,
  UpdateJobPostingRequest,
} from '@/lib/api'

/** Student browse — OPEN postings only, paginated, optional title filter. */
export function useJobPostings(params?: ListJobPostingsParams) {
  return useQuery({
    queryKey: queryKeys.jobPostings.list(params as Record<string, unknown> | undefined),
    queryFn: () => jobPostingsApi.list(params),
    staleTime: 30_000,
  })
}

/** Officer manage — all statuses, paginated, optional status filter. */
export function useManagedJobPostings(params?: ManageJobPostingsParams) {
  return useQuery({
    queryKey: queryKeys.jobPostings.manage(params as Record<string, unknown> | undefined),
    queryFn: () => jobPostingsApi.listManage(params),
    staleTime: 30_000,
  })
}

/** Detail projection including required skills and eligible branches. */
export function useJobPosting(id: string) {
  return useQuery({
    queryKey: queryKeys.jobPostings.detail(id),
    queryFn: () => jobPostingsApi.getById(id),
    staleTime: 30_000,
    enabled: !!id,
  })
}

export function useCreateJobPosting() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateJobPostingRequest) => jobPostingsApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.jobPostings.all() })
    },
  })
}

export function useUpdateJobPosting(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateJobPostingRequest) => jobPostingsApi.update(id, data),
    onSuccess: () => {
      // Title/CTC/deadline changes surface in every list projection as well as the detail.
      void qc.invalidateQueries({ queryKey: queryKeys.jobPostings.all() })
    },
  })
}

/**
 * Lifecycle transitions (open/close/cancel). A status change moves a posting in and
 * out of the student browse list and flips its badge everywhere, so the whole
 * job-postings cache tree is invalidated. The lifecycle endpoints return the summary
 * projection (skills/branches null), so the detail cache is refetched rather than seeded.
 */
export function useOpenJobPosting() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => jobPostingsApi.open(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.jobPostings.all() })
    },
  })
}

export function useCloseJobPosting() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => jobPostingsApi.close(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.jobPostings.all() })
    },
  })
}

export function useCancelJobPosting() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => jobPostingsApi.cancel(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.jobPostings.all() })
    },
  })
}

/**
 * Tagging mutations. Required skills and eligible branches never appear in list
 * summaries, so only the detail entry is affected. Each endpoint returns the detailed
 * posting, so the detail cache is seeded directly — no refetch needed.
 */
export function useAddJobPostingSkill(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (skillId: string) => jobPostingsApi.addSkill(id, skillId),
    onSuccess: (updated: JobPostingResponse) => {
      qc.setQueryData(queryKeys.jobPostings.detail(id), updated)
    },
  })
}

export function useRemoveJobPostingSkill(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (skillId: string) => jobPostingsApi.removeSkill(id, skillId),
    onSuccess: (updated: JobPostingResponse) => {
      qc.setQueryData(queryKeys.jobPostings.detail(id), updated)
    },
  })
}

export function useAddJobPostingBranch(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (branchId: string) => jobPostingsApi.addBranch(id, branchId),
    onSuccess: (updated: JobPostingResponse) => {
      qc.setQueryData(queryKeys.jobPostings.detail(id), updated)
    },
  })
}

export function useRemoveJobPostingBranch(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (branchId: string) => jobPostingsApi.removeBranch(id, branchId),
    onSuccess: (updated: JobPostingResponse) => {
      qc.setQueryData(queryKeys.jobPostings.detail(id), updated)
    },
  })
}
