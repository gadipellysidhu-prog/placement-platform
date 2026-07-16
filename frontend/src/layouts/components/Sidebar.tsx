import { Link, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Users,
  Building2,
  Briefcase,
  FileCheck2,
  Award,
  Gift,
  GitBranch,
  Sparkles,
  UserCircle,
  ShieldCheck,
  Settings2,
  ScrollText,
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
  /** Groups the item under a labelled heading. Ungrouped items render first, unheaded. */
  section?: string
}

const ADMINISTRATION = 'Administration'

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
    label: 'My Offers',
    href: ROUTES.STUDENT.MY_OFFERS,
    icon: Gift,
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
    label: 'Manage Postings',
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
    label: 'Offers',
    href: ROUTES.OFFICER.OFFERS,
    icon: Gift,
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

  // Administration — admin only, mirroring the backend's hasRole('ADMIN') on /api/admin/**.
  {
    label: 'Users',
    href: ROUTES.ADMIN.USERS,
    icon: ShieldCheck,
    allowedRoles: [ROLES.ADMIN],
    section: ADMINISTRATION,
  },
  {
    label: 'Settings',
    href: ROUTES.ADMIN.SETTINGS,
    icon: Settings2,
    allowedRoles: [ROLES.ADMIN],
    section: ADMINISTRATION,
  },
  {
    label: 'Audit Logs',
    href: ROUTES.ADMIN.AUDIT_LOGS,
    icon: ScrollText,
    allowedRoles: [ROLES.ADMIN],
    section: ADMINISTRATION,
  },
]

interface NavLinkProps {
  item: NavItem
  pathname: string
  collapsed: boolean
  onNavigate: () => void
}

function NavLink({ item, pathname, collapsed, onNavigate }: NavLinkProps) {
  const isActive =
    item.href === ROUTES.DASHBOARD ? pathname === item.href : pathname.startsWith(item.href)
  const Icon = item.icon

  return (
    <li>
      <Link
        to={item.href}
        onClick={onNavigate}
        aria-current={isActive ? 'page' : undefined}
        title={collapsed ? item.label : undefined}
        className={cn(
          'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
          isActive
            ? 'bg-primary/10 text-primary'
            : 'text-muted-foreground hover:bg-accent hover:text-foreground',
          collapsed && 'justify-center px-2',
        )}
      >
        <Icon className="h-4 w-4 shrink-0" aria-hidden="true" />
        {!collapsed && <span className="truncate">{item.label}</span>}
      </Link>
    </li>
  )
}

export function Sidebar() {
  const location = useLocation()
  const user = useAuthStore((s) => s.user)
  const { sidebarOpen, sidebarCollapsed, setSidebarOpen, toggleSidebarCollapsed } = useUIStore()

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.allowedRoles || (user?.role && item.allowedRoles.includes(user.role)),
  )

  // Ungrouped items keep their existing flat, unheaded listing; sectioned items follow
  // under a heading. A section with nothing visible disappears entirely.
  const ungrouped = visibleItems.filter((item) => !item.section)
  const sections = visibleItems.reduce<Map<string, NavItem[]>>((acc, item) => {
    if (!item.section) return acc
    const existing = acc.get(item.section)
    if (existing) existing.push(item)
    else acc.set(item.section, [item])
    return acc
  }, new Map())

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
            {ungrouped.map((item) => (
              <NavLink
                key={item.href}
                item={item}
                pathname={location.pathname}
                collapsed={sidebarCollapsed}
                onNavigate={() => setSidebarOpen(false)}
              />
            ))}
          </ul>

          {[...sections].map(([section, items]) => (
            <div key={section} className="mt-4">
              {sidebarCollapsed ? (
                <div className="mx-2 mb-1 border-t border-border" role="presentation" />
              ) : (
                <h2 className="px-3 pb-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  {section}
                </h2>
              )}
              <ul className="space-y-1">
                {items.map((item) => (
                  <NavLink
                    key={item.href}
                    item={item}
                    pathname={location.pathname}
                    collapsed={sidebarCollapsed}
                    onNavigate={() => setSidebarOpen(false)}
                  />
                ))}
              </ul>
            </div>
          ))}
        </nav>
      </aside>
    </>
  )
}
