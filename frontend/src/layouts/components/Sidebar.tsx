import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  Building2,
  Briefcase,
  FileCheck2,
  Award,
  GitBranch,
  Sparkles,
  UserCircle,
  ChevronLeft,
  X,
} from 'lucide-react'
import { useAuthStore } from '@/stores/auth.store'
import { useUIStore } from '@/stores/ui.store'
import { ROUTES } from '@/constants/routes'
import { ROLES } from '@/constants/roles'
import { cn } from '@/utils/cn'
import type { Role } from '@/types/auth'

interface NavItem {
  label: string
  href: string
  icon: React.ElementType
  allowedRoles?: Role[]
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', href: ROUTES.DASHBOARD, icon: LayoutDashboard },
  {
    label: 'My Profile',
    href: ROUTES.STUDENT.PROFILE,
    icon: UserCircle,
    allowedRoles: [ROLES.STUDENT],
  },
  {
    label: 'Browse Jobs',
    href: ROUTES.STUDENT.JOB_POSTINGS,
    icon: Briefcase,
    allowedRoles: [ROLES.STUDENT],
  },
  {
    label: 'My Applications',
    href: ROUTES.STUDENT.MY_APPLICATIONS,
    icon: FileCheck2,
    allowedRoles: [ROLES.STUDENT],
  },
  {
    label: 'My Certificates',
    href: ROUTES.STUDENT.MY_CERTIFICATES,
    icon: Award,
    allowedRoles: [ROLES.STUDENT],
  },
  {
    label: 'Students',
    href: ROUTES.OFFICER.STUDENTS,
    icon: Users,
    allowedRoles: [ROLES.PLACEMENT_OFFICER, ROLES.ADMIN],
  },
  { label: 'Companies', href: ROUTES.OFFICER.COMPANIES, icon: Building2 },
  {
    label: 'Job Postings',
    href: ROUTES.OFFICER.JOB_POSTINGS,
    icon: Briefcase,
    allowedRoles: [ROLES.PLACEMENT_OFFICER, ROLES.ADMIN],
  },
  {
    label: 'Applications',
    href: ROUTES.OFFICER.APPLICATIONS,
    icon: FileCheck2,
    allowedRoles: [ROLES.PLACEMENT_OFFICER, ROLES.ADMIN],
  },
  {
    label: 'Certificates',
    href: ROUTES.OFFICER.CERTIFICATES,
    icon: Award,
    allowedRoles: [ROLES.PLACEMENT_OFFICER, ROLES.ADMIN],
  },
  {
    label: 'Skills',
    href: ROUTES.OFFICER.SKILLS,
    icon: Sparkles,
    allowedRoles: [ROLES.PLACEMENT_OFFICER, ROLES.ADMIN],
  },
  {
    label: 'Branches',
    href: ROUTES.OFFICER.BRANCHES,
    icon: GitBranch,
    allowedRoles: [ROLES.PLACEMENT_OFFICER, ROLES.ADMIN],
  },
]

export function Sidebar() {
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const { sidebarOpen, sidebarCollapsed, setSidebarOpen, toggleSidebarCollapsed } = useUIStore()

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.allowedRoles || (user?.role && item.allowedRoles.includes(user.role)),
  )

  return (
    <>
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-20 bg-black/50 lg:hidden"
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside
        aria-label="Main navigation"
        className={cn(
          'fixed inset-y-0 left-0 z-30 flex flex-col border-r border-border bg-card transition-all duration-200',
          sidebarCollapsed ? 'w-16' : 'w-64',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0',
        )}
      >
        <div className="flex h-14 shrink-0 items-center justify-between border-b border-border px-4">
          {!sidebarCollapsed && (
            <span className="truncate text-sm font-semibold text-foreground">Placement Intel</span>
          )}
          <div className="flex items-center">
            <button
              onClick={toggleSidebarCollapsed}
              className="hidden rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground lg:flex"
              aria-label={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            >
              <ChevronLeft
                className={cn('h-4 w-4 transition-transform', sidebarCollapsed && 'rotate-180')}
              />
            </button>
            <button
              onClick={() => setSidebarOpen(false)}
              className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground lg:hidden"
              aria-label="Close navigation"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto p-2" aria-label="Primary">
          <ul className="space-y-1">
            {visibleItems.map((item) => {
              const isActive =
                item.href === ROUTES.DASHBOARD
                  ? location.pathname === item.href
                  : location.pathname.startsWith(item.href)
              const Icon = item.icon

              return (
                <li key={item.href}>
                  <Link
                    to={item.href}
                    onClick={() => setSidebarOpen(false)}
                    aria-current={isActive ? 'page' : undefined}
                    title={sidebarCollapsed ? item.label : undefined}
                    className={cn(
                      'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                      isActive
                        ? 'bg-primary/10 text-primary'
                        : 'text-muted-foreground hover:bg-accent hover:text-foreground',
                      sidebarCollapsed && 'justify-center px-2',
                    )}
                  >
                    <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
                    {!sidebarCollapsed && <span className="truncate">{item.label}</span>}
                  </Link>
                </li>
              )
            })}
          </ul>
        </nav>
      </aside>
    </>
  )
}
