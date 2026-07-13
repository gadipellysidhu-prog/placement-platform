import { beforeEach, describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { Route, Routes } from 'react-router-dom'
import { renderWithProviders, screen, waitFor, within } from '@/test'
import { server, API_BASE_URL } from '@/test'
import { ROUTES } from '@/constants/routes'
import type { PendingRegistrationResponse, StudentResponse } from '@/lib/api'
import type { Page } from '@/types'
import StudentsListPage from './StudentsListPage'

function pageOf<T>(content: T[]): Page<T> {
  return {
    content,
    pageable: { pageNumber: 0, pageSize: 20, sort: { sorted: false } },
    totalElements: content.length,
    totalPages: 1,
    last: true,
    first: true,
    numberOfElements: content.length,
    size: 20,
    number: 0,
    empty: content.length === 0,
  }
}

function student(overrides: Partial<StudentResponse> = {}): StudentResponse {
  return {
    id: 'stu-1',
    userId: 'u-1',
    userEmail: 'existing@uni.edu',
    rollNumber: 'CS2021001',
    branchId: null,
    branchName: null,
    cgpa: null,
    currentYear: 2,
    placementEligible: true,
    status: 'ACTIVE',
    skillNames: [],
    createdAt: '2026-07-01T10:00:00Z',
    updatedAt: '2026-07-01T10:00:00Z',
    ...overrides,
  }
}

function pending(
  overrides: Partial<PendingRegistrationResponse> = {},
): PendingRegistrationResponse {
  return {
    userId: 'pending-user-1',
    email: 'jane.doe@uni.edu',
    displayName: 'Jane Doe',
    emailVerified: true,
    createdAt: '2026-07-05T09:00:00Z',
    ...overrides,
  }
}

function stub({
  students = [student()],
  pendingList = [pending()],
}: {
  students?: StudentResponse[]
  pendingList?: PendingRegistrationResponse[]
} = {}) {
  server.use(
    http.get(`${API_BASE_URL}/api/students`, () => HttpResponse.json(pageOf(students))),
    http.get(`${API_BASE_URL}/api/students/pending`, () => HttpResponse.json(pageOf(pendingList))),
  )
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route path={ROUTES.OFFICER.STUDENTS} element={<StudentsListPage />} />
      <Route path={`${ROUTES.OFFICER.STUDENTS}/:id`} element={<div>Student Detail</div>} />
    </Routes>,
    { initialEntries: [ROUTES.OFFICER.STUDENTS] },
  )
}

describe('StudentsListPage — pending registrations', () => {
  beforeEach(() => stub())

  it('lists pending registrations with name, email and created date', async () => {
    renderPage()

    expect(await screen.findByText('Pending Registrations')).toBeInTheDocument()
    expect(screen.getByText('Jane Doe')).toBeInTheDocument()
    expect(screen.getByText(/jane\.doe@uni\.edu · Registered/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /approve/i })).toBeInTheDocument()
  })

  it('hides the pending section when there are no pending registrations', async () => {
    stub({ pendingList: [] })
    renderPage()

    // Students table still renders...
    expect(await screen.findByText('CS2021001')).toBeInTheDocument()
    // ...but the pending section is absent.
    expect(screen.queryByText('Pending Registrations')).not.toBeInTheDocument()
  })

  it('allows approving an unverified registration (approval completes onboarding)', async () => {
    stub({ pendingList: [pending({ emailVerified: false })] })
    renderPage()

    expect(await screen.findByText('Jane Doe')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /approve/i })).toBeEnabled()
  })
})

describe('StudentsListPage — approve flow', () => {
  beforeEach(() => stub())

  it('approves a registration through the dialog and posts the roll number', async () => {
    let approvedUserId: string | null = null
    let body: { rollNumber: string; currentYear: number } | null = null
    server.use(
      http.post(`${API_BASE_URL}/api/students/approvals/:userId`, async ({ params, request }) => {
        approvedUserId = params.userId as string
        body = (await request.json()) as typeof body
        return HttpResponse.json(student({ userEmail: 'jane.doe@uni.edu' }), { status: 201 })
      }),
    )

    const { user } = renderPage()

    await user.click(await screen.findByRole('button', { name: /approve/i }))

    // Dialog appears; enter a roll number and submit.
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText(/roll number/i), 'CS2025099')
    await user.click(within(dialog).getByRole('button', { name: /^approve$/i }))

    await waitFor(() => expect(approvedUserId).toBe('pending-user-1'))
    expect(body).toEqual({ rollNumber: 'CS2025099', currentYear: 1 })
  })

  it('surfaces a backend 409 (roll number taken) as a readable error', async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/students/approvals/:userId`, () =>
        HttpResponse.json(
          { title: 'Conflict', status: 409, detail: 'Roll number already registered' },
          { status: 409 },
        ),
      ),
    )

    const { user } = renderPage()

    await user.click(await screen.findByRole('button', { name: /approve/i }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText(/roll number/i), 'CS2021001')
    await user.click(within(dialog).getByRole('button', { name: /^approve$/i }))

    expect(await screen.findByText(/roll number already registered/i)).toBeInTheDocument()
  })
})
