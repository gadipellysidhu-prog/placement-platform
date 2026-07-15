import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { offersApi, queryKeys } from '@/lib/api'
import type { CreateOfferRequest } from '@/lib/api'
import type { PageParams } from '@/types'

/** Authenticated student's own offers (unbounded list — backend `/my`). */
export function useMyOffers() {
  return useQuery({
    queryKey: queryKeys.offers.my(),
    queryFn: () => offersApi.my(),
    staleTime: 30_000,
  })
}

/** Officer offers management — every offer, paginated (`GET /api/offers`). */
export function useOffers(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.offers.list(params as Record<string, unknown> | undefined),
    queryFn: () => offersApi.list(params),
    staleTime: 30_000,
  })
}

/**
 * Create an offer for an OFFERED application (officer). The backend rejects a
 * non-offerable application with 422 and a duplicate offer with 409 — callers surface
 * those. On success the offers tree and the applications tree are both invalidated
 * (the application now has an offer).
 */
export function useCreateOffer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: CreateOfferRequest) => offersApi.create(data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.offers.all() })
      void qc.invalidateQueries({ queryKey: queryKeys.applications.all() })
    },
  })
}

/** Student accept (POST /{id}/accept). Also flips the student to PLACED server-side. */
export function useAcceptOffer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => offersApi.accept(id),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.offers.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.offers.all() })
      // Placement status changed — refresh dashboards / profile-derived views.
      void qc.invalidateQueries({ queryKey: queryKeys.students.all() })
    },
  })
}

/** Student reject (POST /{id}/reject). */
export function useRejectOffer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => offersApi.reject(id),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.offers.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.offers.all() })
    },
  })
}

/** Officer expire (POST /{id}/expire). Only PENDING offers can be expired (else 422). */
export function useExpireOffer() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => offersApi.expire(id),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.offers.detail(updated.id), updated)
      void qc.invalidateQueries({ queryKey: queryKeys.offers.all() })
    },
  })
}
