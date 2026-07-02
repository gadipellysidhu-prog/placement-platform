import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { companiesApi, queryKeys } from '@/lib/api'
import type { CreateCompanyRequest, UpdateCompanyRequest } from '@/lib/api'
import type { PageParams } from '@/types'

export function useCompanies(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.companies.list(params as Record<string, unknown> | undefined),
    queryFn: () => companiesApi.list(params),
    staleTime: 30_000,
  })
}

export function useCompany(id: string) {
  return useQuery({
    queryKey: queryKeys.companies.detail(id),
    queryFn: () => companiesApi.getById(id),
    staleTime: 30_000,
    enabled: !!id,
  })
}

export function useCreateCompany() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCompanyRequest) => companiesApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.companies.all() })
    },
  })
}

export function useUpdateCompany(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateCompanyRequest) => companiesApi.update(id, data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.companies.all() })
    },
  })
}

export function useActivateCompany() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => companiesApi.activate(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.companies.all() })
    },
  })
}

export function useDeactivateCompany() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => companiesApi.deactivate(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.companies.all() })
    },
  })
}

export function useBlacklistCompany() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => companiesApi.blacklist(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.companies.all() })
    },
  })
}
