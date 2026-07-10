import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { ThemeProvider } from '@/providers/ThemeProvider'
import { QueryProvider } from '@/providers/QueryProvider'
import { NotificationProvider } from '@/providers/NotificationProvider'
import { SessionProvider } from '@/features/auth/SessionProvider'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { PublicRoute } from '@/routes/PublicRoute'
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

            {/* Officer/Admin routes — Students */}
            <Route path={ROUTES.OFFICER.STUDENTS} element={<StudentsListPage />} />
            <Route path={`${ROUTES.OFFICER.STUDENTS}/:id`} element={<StudentDetailPage />} />

            {/* Companies — all authenticated roles */}
            <Route path={ROUTES.OFFICER.COMPANIES} element={<CompaniesListPage />} />
            <Route path={ROUTES.OFFICER.CREATE_COMPANY} element={<CreateCompanyPage />} />
            <Route path={`${ROUTES.OFFICER.COMPANIES}/:id`} element={<CompanyDetailPage />} />
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
