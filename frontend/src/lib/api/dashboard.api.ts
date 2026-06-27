import { apiClient } from '@/lib/axios'

export interface DashboardSummary {
  totalStudents: number
  placedStudents: number
  activeCompanies: number
  openJobPostings: number
  pendingApplications: number
  pendingCertificates: number
  placementRatePercent: number
}

export const dashboardApi = {
  summary: () => apiClient.get<DashboardSummary>('/api/dashboard/summary').then((r) => r.data),
}
