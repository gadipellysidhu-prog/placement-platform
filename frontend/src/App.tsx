import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from '@/providers/ThemeProvider'
import { QueryProvider } from '@/providers/QueryProvider'
import { NotificationProvider } from '@/providers/NotificationProvider'
import { SessionProvider } from '@/features/auth/SessionProvider'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { PublicRoute } from '@/routes/PublicRoute'
import { RoleRoute } from '@/routes/RoleRoute'
import { ROLES } from '@/constants/roles'
import { AuthLayout } from '@/layouts/AuthLayout'
import { DashboardLayout } from '@/layouts/DashboardLayout'
import { ErrorBoundary } from '@/shared/ui/error-boundary'
import { TooltipProvider } from '@/shared/ui/tooltip'
import { ROUTES } from '@/constants/routes'
import LoadingPage from '@/pages/LoadingPage'

// Lazy-loaded pages
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'))
const ForbiddenPage = lazy(() => import('@/pages/ForbiddenPage'))
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))

// Auth feature pages
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('@/features/auth/pages/ForgotPasswordPage'))
const ResetPasswordPage = lazy(() => import('@/features/auth/pages/ResetPasswordPage'))
const VerifyEmailPage = lazy(() => import('@/features/auth/pages/VerifyEmailPage'))
const AcceptInvitationPage = lazy(() => import('@/features/auth/pages/AcceptInvitationPage'))

// Student feature pages
const StudentProfilePage = lazy(() => import('@/features/students/pages/StudentProfilePage'))
const StudentsListPage = lazy(() => import('@/features/students/pages/StudentsListPage'))
const StudentDetailPage = lazy(() => import('@/features/students/pages/StudentDetailPage'))

// Company feature pages
const CompaniesListPage = lazy(() => import('@/features/companies/pages/CompaniesListPage'))
const CompanyDetailPage = lazy(() => import('@/features/companies/pages/CompanyDetailPage'))
const CreateCompanyPage = lazy(() => import('@/features/companies/pages/CreateCompanyPage'))

// Job posting feature pages
const JobPostingsListPage = lazy(() => import('@/features/job-postings/pages/JobPostingsListPage'))
const JobPostingDetailPage = lazy(
  () => import('@/features/job-postings/pages/JobPostingDetailPage'),
)
const ManageJobPostingsPage = lazy(
  () => import('@/features/job-postings/pages/ManageJobPostingsPage'),
)
const CreateJobPostingPage = lazy(
  () => import('@/features/job-postings/pages/CreateJobPostingPage'),
)
const EditJobPostingPage = lazy(() => import('@/features/job-postings/pages/EditJobPostingPage'))

// Applications feature pages
const MyApplicationsPage = lazy(() => import('@/features/applications/pages/MyApplicationsPage'))
const ApplicationsPipelinePage = lazy(
  () => import('@/features/applications/pages/ApplicationsPipelinePage'),
)
const ApplicationDetailPage = lazy(
  () => import('@/features/applications/pages/ApplicationDetailPage'),
)

// Certificates feature pages
const MyCertificatesPage = lazy(() => import('@/features/certificates/pages/MyCertificatesPage'))
const CertificateVerificationPage = lazy(
  () => import('@/features/certificates/pages/CertificateVerificationPage'),
)

// Offers feature pages
const MyOffersPage = lazy(() => import('@/features/offers/pages/MyOffersPage'))
const OffersManagementPage = lazy(() => import('@/features/offers/pages/OffersManagementPage'))

// Administration feature pages (admin only)
const UsersListPage = lazy(() => import('@/features/admin/pages/UsersListPage'))
const UserDetailPage = lazy(() => import('@/features/admin/pages/UserDetailPage'))
const AdminSettingsPage = lazy(() => import('@/features/admin/pages/SettingsPage'))
const AuditLogsPage = lazy(() => import('@/features/admin/pages/AuditLogsPage'))

// Catalog administration pages
const BranchesPage = lazy(() => import('@/features/admin/pages/BranchesPage'))
const SkillsPage = lazy(() => import('@/features/admin/pages/SkillsPage'))
const AcademicYearsPage = lazy(() => import('@/features/admin/pages/AcademicYearsPage'))

function AppRoutes(): React.ReactElement {
  return (
    <Suspense fallback={<LoadingPage />}>
      <Routes>
        {/* Root redirect */}
        <Route path="/" element={<Navigate to={ROUTES.DASHBOARD} replace />} />

        {/* Public routes — redirect authenticated users away */}
        <Route element={<PublicRoute />}>
          <Route element={<AuthLayout />}>
            <Route path={ROUTES.LOGIN} element={<LoginPage />} />
            <Route path={ROUTES.REGISTER} element={<RegisterPage />} />
            <Route path={ROUTES.FORGOT_PASSWORD} element={<ForgotPasswordPage />} />
          </Route>
        </Route>

        {/* Token-driven auth journeys — reachable by anyone with the email link,
            including a signed-in administrator testing an invitation. Deliberately
            NOT behind PublicRoute (which would bounce authenticated users to the
            dashboard before the token is consumed). */}
        <Route element={<AuthLayout />}>
          <Route path={ROUTES.VERIFY_EMAIL} element={<VerifyEmailPage />} />
          <Route path={ROUTES.RESET_PASSWORD} element={<ResetPasswordPage />} />
          <Route path={ROUTES.ACCEPT_INVITATION} element={<AcceptInvitationPage />} />
        </Route>

        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route element={<DashboardLayout />}>
            {/* Main dashboard */}
            <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />

            {/* Student routes */}
            <Route path={ROUTES.STUDENT.PROFILE} element={<StudentProfilePage />} />

            {/* Applications — student view (own applications + owner detail). The
                detail page is role-aware and 403-safe for non-owners. */}
            <Route path={ROUTES.STUDENT.MY_APPLICATIONS} element={<MyApplicationsPage />} />
            <Route
              path={`${ROUTES.STUDENT.MY_APPLICATIONS}/:id`}
              element={<ApplicationDetailPage />}
            />

            {/* Certificates & Offers — student views (own submissions / own offers). */}
            <Route path={ROUTES.STUDENT.MY_CERTIFICATES} element={<MyCertificatesPage />} />
            <Route path={ROUTES.STUDENT.MY_OFFERS} element={<MyOffersPage />} />

            {/* Officer/Admin routes — Students */}
            <Route path={ROUTES.OFFICER.STUDENTS} element={<StudentsListPage />} />
            <Route path={`${ROUTES.OFFICER.STUDENTS}/:id`} element={<StudentDetailPage />} />

            {/* Companies — all authenticated roles */}
            <Route path={ROUTES.OFFICER.COMPANIES} element={<CompaniesListPage />} />
            <Route path={ROUTES.OFFICER.CREATE_COMPANY} element={<CreateCompanyPage />} />
            <Route path={`${ROUTES.OFFICER.COMPANIES}/:id`} element={<CompanyDetailPage />} />

            {/* Job postings — student browse + shared detail (open to all authenticated
                roles; the backend's role hierarchy lets officers read these too). */}
            <Route path={ROUTES.STUDENT.JOB_POSTINGS} element={<JobPostingsListPage />} />
            <Route path={`${ROUTES.STUDENT.JOB_POSTINGS}/:id`} element={<JobPostingDetailPage />} />

            {/* Job postings — officer/admin management (create, edit, lifecycle, tagging). */}
            <Route element={<RoleRoute minimumRole={ROLES.PLACEMENT_OFFICER} />}>
              <Route path={ROUTES.OFFICER.JOB_POSTINGS} element={<ManageJobPostingsPage />} />
              <Route path={ROUTES.OFFICER.CREATE_JOB_POSTING} element={<CreateJobPostingPage />} />
              <Route
                path={`${ROUTES.OFFICER.JOB_POSTINGS}/:id/edit`}
                element={<EditJobPostingPage />}
              />
              <Route
                path={`${ROUTES.OFFICER.JOB_POSTINGS}/:id`}
                element={<JobPostingDetailPage />}
              />

              {/* Applications — officer pipeline + role-aware detail (transitions). */}
              <Route path={ROUTES.OFFICER.APPLICATIONS} element={<ApplicationsPipelinePage />} />
              <Route
                path={`${ROUTES.OFFICER.APPLICATIONS}/:id`}
                element={<ApplicationDetailPage />}
              />

              {/* Certificate verification + offers management (officer/admin). */}
              <Route path={ROUTES.OFFICER.CERTIFICATES} element={<CertificateVerificationPage />} />
              <Route path={ROUTES.OFFICER.OFFERS} element={<OffersManagementPage />} />

              {/* Catalogue administration. Branch and skill mutations require
                  PLACEMENT_OFFICER (not ADMIN), so these sit at officer level. */}
              <Route path={ROUTES.OFFICER.BRANCHES} element={<BranchesPage />} />
              <Route path={ROUTES.OFFICER.SKILLS} element={<SkillsPage />} />
            </Route>

            {/* Academic years — every mutation requires ADMIN, so the management
                screen is admin-only even though officers may read the years. */}
            <Route element={<RoleRoute minimumRole={ROLES.ADMIN} />}>
              <Route path={ROUTES.ADMIN.ACADEMIC_YEARS} element={<AcademicYearsPage />} />
            </Route>

            {/* Administration — admin only, mirroring @PreAuthorize("hasRole('ADMIN')")
                on /api/admin/**. An officer reaching these lands on 403. */}
            <Route element={<RoleRoute minimumRole={ROLES.ADMIN} />}>
              <Route path={ROUTES.ADMIN.USERS} element={<UsersListPage />} />
              <Route path={`${ROUTES.ADMIN.USERS}/:id`} element={<UserDetailPage />} />
              <Route path={ROUTES.ADMIN.SETTINGS} element={<AdminSettingsPage />} />
              <Route path={ROUTES.ADMIN.AUDIT_LOGS} element={<AuditLogsPage />} />
            </Route>
          </Route>
        </Route>

        {/* Error routes */}
        <Route path={ROUTES.FORBIDDEN} element={<ForbiddenPage />} />
        <Route path={ROUTES.NOT_FOUND} element={<NotFoundPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}

export default function App(): React.ReactElement {
  return (
    <ErrorBoundary>
      <QueryProvider>
        <ThemeProvider>
          <TooltipProvider>
            <NotificationProvider />
            <BrowserRouter>
              <SessionProvider>
                <AppRoutes />
              </SessionProvider>
            </BrowserRouter>
          </TooltipProvider>
        </ThemeProvider>
      </QueryProvider>
    </ErrorBoundary>
  )
}
