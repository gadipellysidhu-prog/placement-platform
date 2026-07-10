/**
 * Central route path registry. Never hardcode route strings in components —
 * always import from here so refactoring is safe.
 */
export const ROUTES = {
  // Public
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',

  // Auth journeys — paths MUST match the SPA URLs the backend embeds in emails
  // (auth.verification.* in application.yml: /verify-email, /reset-password,
  // /accept-invitation, each carrying a `?token=` query parameter).
  VERIFY_EMAIL: '/verify-email',
  RESET_PASSWORD: '/reset-password',
  ACCEPT_INVITATION: '/accept-invitation',

  // Root
  DASHBOARD: '/dashboard',

  // Student routes
  STUDENT: {
    PROFILE: '/dashboard/profile',
    JOB_POSTINGS: '/dashboard/jobs',
    JOB_POSTING_DETAIL: (id: string) => `/dashboard/jobs/${id}` as const,
    MY_APPLICATIONS: '/dashboard/applications',
    MY_OFFERS: '/dashboard/offers',
    MY_CERTIFICATES: '/dashboard/certificates',
    SUBMIT_CERTIFICATE: '/dashboard/certificates/submit',
  },

  // Officer / admin routes
  OFFICER: {
    STUDENTS: '/dashboard/students',
    STUDENT_DETAIL: (id: string) => `/dashboard/students/${id}` as const,
    COMPANIES: '/dashboard/companies',
    COMPANY_DETAIL: (id: string) => `/dashboard/companies/${id}` as const,
    CREATE_COMPANY: '/dashboard/companies/new',
    JOB_POSTINGS: '/dashboard/manage/jobs',
    JOB_POSTING_DETAIL: (id: string) => `/dashboard/manage/jobs/${id}` as const,
    CREATE_JOB_POSTING: '/dashboard/manage/jobs/new',
    APPLICATIONS: '/dashboard/manage/applications',
    APPLICATION_DETAIL: (id: string) => `/dashboard/manage/applications/${id}` as const,
    OFFERS: '/dashboard/manage/offers',
    CERTIFICATES: '/dashboard/manage/certificates',
    SKILLS: '/dashboard/manage/skills',
    BRANCHES: '/dashboard/manage/branches',
  },

  // Error
  FORBIDDEN: '/403',
  NOT_FOUND: '/404',
} as const
