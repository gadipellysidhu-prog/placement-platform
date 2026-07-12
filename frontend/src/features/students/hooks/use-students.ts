import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { studentsApi, queryKeys } from '@/lib/api'
import type { UpdateStudentRequest, ApproveRegistrationRequest } from '@/lib/api'
import type { UpdateStudentStatusRequest } from '@/lib/api/students.api'
import type { PageParams } from '@/types'

export function useStudents(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.students.list(params as Record<string, unknown> | undefined),
    queryFn: () => studentsApi.list(params),
    staleTime: 30_000,
  })
}

export function useStudent(id: string) {
  return useQuery({
    queryKey: queryKeys.students.detail(id),
    queryFn: () => studentsApi.getById(id),
    staleTime: 30_000,
    enabled: !!id,
  })
}

export function useMyStudentProfile() {
  return useQuery({
    queryKey: queryKeys.students.me(),
    queryFn: () => studentsApi.me(),
    staleTime: 30_000,
  })
}

/** Registrations awaiting officer approval (ROLE_STUDENT accounts with no profile). */
export function usePendingRegistrations(params?: PageParams) {
  return useQuery({
    queryKey: queryKeys.students.pending(params as Record<string, unknown> | undefined),
    queryFn: () => studentsApi.listPending(params),
    staleTime: 30_000,
  })
}

/**
 * Approve a pending registration. Invalidates the whole students tree so the pending
 * list and the students list both refresh (the approved user leaves the former and
 * appears in the latter).
 */
export function useApproveRegistration() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (vars: { userId: string; data: ApproveRegistrationRequest }) =>
      studentsApi.approve(vars.userId, vars.data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.students.all() })
    },
  })
}

export function useUpdateStudent(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateStudentRequest) => studentsApi.update(id, data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.students.all() })
    },
  })
}

export function useUpdateStudentStatus(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (data: UpdateStudentStatusRequest) => studentsApi.updateStatus(id, data),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.students.all() })
    },
  })
}

export function useUpdateStudentEligibility(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => studentsApi.updateEligibility(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.students.detail(id) })
    },
  })
}

export function useAddStudentSkill(studentId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (skillId: string) => studentsApi.addSkill(studentId, skillId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.students.detail(studentId) })
    },
  })
}

export function useRemoveStudentSkill(studentId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (skillId: string) => studentsApi.removeSkill(studentId, skillId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.students.detail(studentId) })
    },
  })
}
