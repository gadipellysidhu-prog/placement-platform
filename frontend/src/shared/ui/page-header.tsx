import * as React from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  action?: React.ReactNode
}

function PageHeader({ title, description, action }: PageHeaderProps) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div className="space-y-1 min-w-0">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{title}</h1>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  )
}

export { PageHeader }
