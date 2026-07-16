import { describe, expect, it } from 'vitest'
import { http, HttpResponse } from 'msw'
import { renderWithProviders, screen, server, API_BASE_URL, pageOf, mockSetting } from '@/test'
import SettingsPage from './SettingsPage'

describe('SettingsPage', () => {
  it('lists settings with their declared type and scope', async () => {
    renderWithProviders(<SettingsPage />)

    expect(await screen.findByText('placement.max-offers-per-student')).toBeInTheDocument()
    expect(screen.getByText('INTEGER')).toBeInTheDocument()
    expect(screen.getByText('Global')).toBeInTheDocument()
  })

  it('shows an empty state when there are no settings', async () => {
    server.use(http.get(`${API_BASE_URL}/api/admin/settings`, () => HttpResponse.json(pageOf([]))))
    renderWithProviders(<SettingsPage />)

    expect(await screen.findByText('No settings yet')).toBeInTheDocument()
  })

  it('edits a setting through a typed form, keying on the immutable key', async () => {
    const { user } = renderWithProviders(<SettingsPage />)
    await screen.findByText('placement.max-offers-per-student')

    await user.click(screen.getByLabelText(/edit placement.max-offers-per-student/i))

    // The key identifies the setting, so it cannot be edited into a different one.
    expect(await screen.findByLabelText('Key')).toBeDisabled()
    // INTEGER settings get a number input rather than a raw JSON blob.
    expect(screen.getByLabelText('Value')).toHaveAttribute('type', 'number')

    await user.click(screen.getByRole('button', { name: 'Save' }))
    expect(await screen.findByText('Setting updated.')).toBeInTheDocument()
  })

  it('allows a key to be entered when creating a new setting', async () => {
    const { user } = renderWithProviders(<SettingsPage />)
    await screen.findByText('placement.max-offers-per-student')

    await user.click(screen.getByRole('button', { name: /new setting/i }))

    expect(await screen.findByLabelText('Key')).toBeEnabled()
  })

  it('sends the upsert payload the backend contract expects', async () => {
    let body: unknown = null
    server.use(
      http.put(`${API_BASE_URL}/api/admin/settings`, async ({ request }) => {
        body = await request.json()
        return HttpResponse.json(mockSetting)
      }),
    )

    const { user } = renderWithProviders(<SettingsPage />)
    await screen.findByText('placement.max-offers-per-student')

    await user.click(screen.getByLabelText(/edit placement.max-offers-per-student/i))
    await user.click(await screen.findByRole('button', { name: 'Save' }))
    await screen.findByText('Setting updated.')

    expect(body).toMatchObject({
      settingKey: 'placement.max-offers-per-student',
      valueType: 'INTEGER',
      settingValue: '2',
      category: 'placement',
    })
  })

  it('confirms before deleting, then removes the setting', async () => {
    const { user } = renderWithProviders(<SettingsPage />)
    await screen.findByText('placement.max-offers-per-student')

    await user.click(screen.getByLabelText(/delete placement.max-offers-per-student/i))
    expect(await screen.findByText('Delete setting?')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Delete' }))
    expect(await screen.findByText('Setting deleted.')).toBeInTheDocument()
  })

  it('surfaces a backend failure instead of reporting success', async () => {
    server.use(
      http.put(`${API_BASE_URL}/api/admin/settings`, () =>
        HttpResponse.json(
          { title: 'Bad Request', status: 400, detail: 'settingValue: must not be blank' },
          { status: 400 },
        ),
      ),
    )

    const { user } = renderWithProviders(<SettingsPage />)
    await screen.findByText('placement.max-offers-per-student')

    await user.click(screen.getByLabelText(/edit placement.max-offers-per-student/i))
    await user.click(await screen.findByRole('button', { name: 'Save' }))

    expect(await screen.findByText('settingValue: must not be blank')).toBeInTheDocument()
  })
})
