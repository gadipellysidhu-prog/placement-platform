/**
 * Centralized query key factory.
 * Ensures consistent, hierarchical cache keys for TanStack Query.
 * All keys are tuples so that partial invalidation works correctly.
 */
export const queryKeys = {
  auth: {
    me: () => ['auth', 'me'] as const,
  },

  students: {
    all: () => ['students'] as const,
    list: (params?: Record<string, unknown>) => ['students', 'list', params] as const,
    pending: (params?: Record<string, unknown>) => ['students', 'pending', params] as const,
    me: () => ['students', 'me'] as const,
    detail: (id: string) => ['students', id] as const,
  },

  companies: {
    all: () => ['companies'] as const,
    list: (params?: Record<string, unknown>) => ['companies', 'list', params] as const,
    detail: (id: string) => ['companies', id] as const,
  },

  jobPostings: {
    all: () => ['job-postings'] as const,
    list: (params?: Record<string, unknown>) => ['job-postings', 'list', params] as const,
    manage: (params?: Record<string, unknown>) => ['job-postings', 'manage', params] as const,
    detail: (id: string) => ['job-postings', id] as const,
  },

  applications: {
    all: () => ['applications'] as const,
    list: (params?: Record<string, unknown>) => ['applications', 'list', params] as const,
    my: () => ['applications', 'my'] as const,
    detail: (id: string) => ['applications', id] as const,
    byStudent: (studentId: string) => ['applications', 'student', studentId] as const,
  },

  offers: {
    all: () => ['offers'] as const,
    list: (params?: Record<string, unknown>) => ['offers', 'list', params] as const,
    my: () => ['offers', 'my'] as const,
    detail: (id: string) => ['offers', id] as const,
  },

  certificates: {
    all: () => ['certificates'] as const,
    list: (params?: Record<string, unknown>) => ['certificates', 'list', params] as const,
    my: () => ['certificates', 'my'] as const,
    detail: (id: string) => ['certificates', id] as const,
    byStudent: (studentId: string) => ['certificates', 'student', studentId] as const,
  },

  branches: {
    all: () => ['branches'] as const,
    list: (params?: Record<string, unknown>) => ['branches', 'list', params] as const,
    detail: (id: string) => ['branches', id] as const,
  },

  skills: {
    all: () => ['skills'] as const,
    list: (params?: Record<string, unknown>) => ['skills', 'list', params] as const,
    search: (q: string) => ['skills', 'search', q] as const,
    detail: (id: string) => ['skills', id] as const,
  },

  academicYears: {
    all: () => ['academic-years'] as const,
    list: (params?: Record<string, unknown>) => ['academic-years', 'list', params] as const,
    active: () => ['academic-years', 'active'] as const,
    detail: (id: string) => ['academic-years', id] as const,
  },

  jobIntelligence: {
    all: () => ['job-intelligence'] as const,
    run: (id: string) => ['job-intelligence', 'run', id] as const,
    latest: (postingId: string) => ['job-intelligence', 'latest', postingId] as const,
  },

  dashboard: {
    summary: () => ['dashboard', 'summary'] as const,
  },
} as const
