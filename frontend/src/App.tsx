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

// Lazy-loaded pages — each route in its own chunk
const NotFoundPage = lazy(() => import('@/pages/NotFoundPage'))
const ForbiddenPage = lazy(() => import('@/pages/ForbiddenPage'))
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))

// Auth feature pages
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('@/features/auth/pages/ForgotPasswordPage'))

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

        {/* Protected routes */}
        <Route element={<ProtectedRoute />}>
          <Route element={<DashboardLayout />}>
            <Route path={ROUTES.DASHBOARD} element={<DashboardPage />} />
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
