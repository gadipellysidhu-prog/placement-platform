import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { applicationsApi, queryKeys } from '@/lib/api'
import type { CreateApplicationRequest, UpdateApplicationStatusRequest } from '@/lib/api'
import type { PageParams } from '@/types'

/** Authenticated student's own applications (unbounded list — backend `/my`). */
export function useMyApplications() {
  return useQuery({
    queryKey: queryKeys.applications.my(),
    queryFn: () => applicationsApi.my(),
    staleTime: 30_000,
  })
}

/** Officer pipeline — every application, paginated. The backend list endpoint
 *  accepts only Spring `Pageable` params (page/size/sort); it exposes no filters. */
export function useApplications(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.applications.list(params as Record<string, unknown> | undefined),
    queryFn: () => applicationsApi.list(params),
    staleTime: 30_000,
  })
}

/** Single application detail (officer read endpoint `/api/applications/{id}`). */
export function useApplication(id: string) {
  return useQuery({
    queryKey: queryKeys.applications.detail(id),
    queryFn: () => applicationsApi.getById(id),
    staleTime: 30_000,
    enabled: !!id,
  })
}

/** Submit a new application (student). Invalidates the student's `/my` list. */
export function useApplyToJob() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateApplicationRequest) => applicationsApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.applications.all() })
    },
  })
}

/**
 * Officer status transition (PUT /status). No optimistic update — the backend
 * enforces the legal transition map and may reject with 409/422, so the cache is
 * only ever seeded from the authoritative response, then the tree is invalidated
 * to force a refetch everywhere the application is shown.
 */
export function useUpdateApplicationStatus(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateApplicationStatusRequest) => applicationsApi.updateStatus(id, data),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.applications.detail(id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.applications.all() })
    },
  })
}

/**
 * Student withdraw (POST /withdraw). No optimistic update; the response is the
 * authoritative WITHDRAWN application. Invalidates the whole applications tree so
 * the dashboard summary and `/my` list refresh.
 */
export function useWithdrawApplication(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => applicationsApi.withdraw(id),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.applications.detail(id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.applications.all() })
    },
  })
}
