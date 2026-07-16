export { authApi } from './auth.api'
export { studentsApi } from './students.api'
export { companiesApi } from './companies.api'
export { jobPostingsApi } from './job-postings.api'
export { applicationsApi } from './applications.api'
export { offersApi } from './offers.api'
export { certificatesApi } from './certificates.api'
export { branchesApi } from './branches.api'
export { skillsApi } from './skills.api'
export { jobIntelligenceApi } from './job-intelligence.api'
export { dashboardApi } from './dashboard.api'
export { filesApi } from './files.api'
export { academicYearsApi } from './academic-years.api'
export { queryKeys } from './keys'
export { normalizeApiError, getApiErrorMessage } from './error'

export type { LoginRequest, RegisterRequest } from './auth.api'
export type {
  StudentResponse,
  CreateStudentRequest,
  UpdateStudentRequest,
  PendingRegistrationResponse,
  ApproveRegistrationRequest,
} from './students.api'
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
  ApplicationStatus,
  JobApplicationResponse,
  CreateApplicationRequest,
  UpdateApplicationStatusRequest,
} from './applications.api'
export type { OfferResponse, CreateOfferRequest } from './offers.api'
export type { CertificateResponse, CreateCertificateRequest } from './certificates.api'
export type {
  BranchResponse,
  CreateBranchRequest,
  UpdateBranchRequest,
  ListBranchesParams,
} from './branches.api'
export type {
  SkillResponse,
  CreateSkillRequest,
  UpdateSkillRequest,
  SkillSearchResult,
  SkillCreatedSource,
  SkillAliasResponse,
  CreateSkillAliasRequest,
  ListSkillsParams,
} from './skills.api'
export type {
  AcademicYearResponse,
  CreateAcademicYearRequest,
  UpdateAcademicYearRequest,
} from './academic-years.api'
export type { JobIntelligenceRun, RunStatus, StartRunRequest } from './job-intelligence.api'
export type { DashboardSummary } from './dashboard.api'
export type { FileResponse, FileScanStatus, FileDownloadLinkResponse } from './files.api'
export type {
  AccountStatus,
  AdminUserResponse,
  ListAdminUsersParams,
  InviteUserRequest,
  AssignRoleRequest,
  MessageResponse,
} from './admin-users.api'
export type {
  SettingResponse,
  SettingValueType,
  SettingUpsertRequest,
  ListSettingsParams,
} from './settings.api'
export type { AuditLogResponse, ListAuditLogsParams } from './audit-logs.api'
