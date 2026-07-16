import { Badge, type BadgeProps } from './badge'

type StudentStatus = 'ACTIVE' | 'PLACED' | 'OPTED_OUT' | 'GRADUATED' | 'BLOCKED'
type CompanyStatus = 'ACTIVE' | 'INACTIVE' | 'BLACKLISTED'
type ApplicationStatus =
  | 'APPLIED'
  | 'SHORTLISTED'
  | 'INTERVIEWED'
  | 'OFFERED'
  | 'REJECTED'
  | 'WITHDRAWN'
type OfferStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED'
type CertificateStatus = 'PENDING' | 'VERIFIED' | 'REJECTED'
type JobPostingStatus = 'DRAFT' | 'OPEN' | 'CLOSED' | 'CANCELLED'
type AccountStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED' | 'INVITED'

type Status =
  | StudentStatus
  | CompanyStatus
  | ApplicationStatus
  | OfferStatus
  | CertificateStatus
  | JobPostingStatus
  | AccountStatus

const STATUS_CONFIG: Record<Status, { label: string; variant: BadgeProps['variant'] }> = {
  // Student status
  ACTIVE: { label: 'Active', variant: 'success' },
  PLACED: { label: 'Placed', variant: 'default' },
  OPTED_OUT: { label: 'Opted Out', variant: 'secondary' },
  GRADUATED: { label: 'Graduated', variant: 'outline' },
  BLOCKED: { label: 'Blocked', variant: 'destructive' },

  // Company status
  INACTIVE: { label: 'Inactive', variant: 'secondary' },
  BLACKLISTED: { label: 'Blacklisted', variant: 'destructive' },

  // Application status
  APPLIED: { label: 'Applied', variant: 'secondary' },
  SHORTLISTED: { label: 'Shortlisted', variant: 'warning' },
  INTERVIEWED: { label: 'Interviewed', variant: 'default' },
  OFFERED: { label: 'Offered', variant: 'success' },
  REJECTED: { label: 'Rejected', variant: 'destructive' },
  WITHDRAWN: { label: 'Withdrawn', variant: 'outline' },

  // Offer status
  PENDING: { label: 'Pending', variant: 'warning' },
  ACCEPTED: { label: 'Accepted', variant: 'success' },
  EXPIRED: { label: 'Expired', variant: 'outline' },

  // Certificate / job posting
  VERIFIED: { label: 'Verified', variant: 'success' },

  // Job posting
  DRAFT: { label: 'Draft', variant: 'secondary' },
  OPEN: { label: 'Open', variant: 'success' },
  CLOSED: { label: 'Closed', variant: 'outline' },
  CANCELLED: { label: 'Cancelled', variant: 'destructive' },

  // Account status (ACTIVE is shared with student status above)
  DISABLED: { label: 'Disabled', variant: 'secondary' },
  LOCKED: { label: 'Locked', variant: 'destructive' },
  INVITED: { label: 'Invited', variant: 'warning' },
}

interface StatusBadgeProps {
  status: Status
  className?: string
}

function StatusBadge({ status, className }: StatusBadgeProps) {
  const config = STATUS_CONFIG[status] ?? { label: status, variant: 'secondary' as const }
  return (
    <Badge variant={config.variant} className={className}>
      {config.label}
    </Badge>
  )
}

export { StatusBadge }
export type {
  Status,
  StudentStatus,
  CompanyStatus,
  ApplicationStatus,
  OfferStatus,
  CertificateStatus,
  JobPostingStatus,
  AccountStatus,
}
