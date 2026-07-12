export { authApi } from './auth.api'
export { studentsApi } from './students.api'
export { companiesApi } from './companies.api'
export { jobPostingsApi } from './job-postings.api'
export { applicationsApi } from './applications.api'
export { offersApi } from './offers.api'
export { certificatesApi } from './certificates.api'
export { branchesApi } from './branches.api'
export { skillsApi } from './skills.api'
export { dashboardApi } from './dashboard.api'
export { filesApi } from './files.api'
export { queryKeys } from './keys'
export { normalizeApiError, getApiErrorMessage } from './error'

export type { LoginRequest, RegisterRequest } from './auth.api'
export type { StudentResponse, CreateStudentRequest, UpdateStudentRequest } from './students.api'
export type { CompanyResponse, CreateCompanyRequest, UpdateCompanyRequest } from './companies.api'
export type {
  JobPostingResponse,
  JobPostingStatus,
  JobPostingSkillRef,
  JobPostingBranchRef,
  CreateJobPostingRequest,
  UpdateJobPostingRequest,
  ListJobPostingsParams,
  ManageJobPostingsParams,
} from './job-postings.api'
export type {
  JobApplicationResponse,
  CreateApplicationRequest,
  UpdateApplicationStatusRequest,
} from './applications.api'
export type { OfferResponse, CreateOfferRequest } from './offers.api'
export type { CertificateResponse, CreateCertificateRequest } from './certificates.api'
export type { BranchResponse, CreateBranchRequest, UpdateBranchRequest } from './branches.api'
export type { SkillResponse, CreateSkillRequest, UpdateSkillRequest } from './skills.api'
export type { DashboardSummary } from './dashboard.api'
export type { FileResponse } from './files.api'
