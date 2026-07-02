import { Outlet } from 'react-router-dom'
import { Sidebar } from './components/Sidebar'
import { TopNav } from './components/TopNav'
import { useUIStore } from '@/stores/ui.store'
import { cn } from '@/utils/cn'

/**
 * Shell for all authenticated pages.
 * Structure: fixed Sidebar | flex-col TopNav + scrollable <main>
 */
export function DashboardLayout() {
  const sidebarCollapsed = useUIStore((s) => s.sidebarCollapsed)

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      <Sidebar />

      {/* Content area shifts right to accommodate sidebar on desktop */}
      <div
        className={cn(
          'flex flex-1 flex-col overflow-hidden transition-all duration-200',
          sidebarCollapsed ? 'lg:ml-16' : 'lg:ml-64',
        )}
      >
        <TopNav />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
