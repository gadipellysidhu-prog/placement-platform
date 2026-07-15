import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { certificatesApi, queryKeys } from '@/lib/api'
import type { CreateCertificateRequest } from '@/lib/api'
import type { PageParams } from '@/types'

/** Authenticated student's own certificates (unbounded list — backend `/my`). */
export function useMyCertificates() {
  return useQuery({
    queryKey: queryKeys.certificates.my(),
    queryFn: () => certificatesApi.my(),
    staleTime: 30_000,
  })
}

/** Officer verification queue — every certificate, paginated (`GET /api/certificates`). */
export function useCertificates(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.certificates.list(params as Record<string, unknown> | undefined),
    queryFn: () => certificatesApi.list(params),
    staleTime: 30_000,
  })
}

/** Submit a new certificate (student). Invalidates the certificates tree. */
export function useSubmitCertificate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateCertificateRequest) => certificatesApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.certificates.all() })
    },
  })
}

/**
 * Officer verify (POST /{id}/verify). The response is the authoritative certificate;
 * the tree is invalidated so the queue, detail, and dashboard counts all refresh.
 */
export function useVerifyCertificate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => certificatesApi.verify(id),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.certificates.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.certificates.all() })
    },
  })
}

/** Officer reject (POST /{id}/reject). No reason payload — the backend accepts none. */
export function useRejectCertificate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => certificatesApi.reject(id),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.certificates.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.certificates.all() })
    },
  })
}
