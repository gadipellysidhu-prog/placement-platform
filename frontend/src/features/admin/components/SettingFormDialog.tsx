import { useEffect, useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/shared/ui/dialog'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { Label } from '@/shared/ui/label'
import { Textarea } from '@/shared/ui/textarea'
import { Switch } from '@/shared/ui/switch'
import { Spinner } from '@/shared/ui/spinner'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/ui/select'
import type { SettingResponse, SettingUpsertRequest, SettingValueType } from '@/lib/api'

const VALUE_TYPES: SettingValueType[] = ['STRING', 'INTEGER', 'LONG', 'BOOLEAN', 'DECIMAL', 'JSON']

/** Mirrors the @Size constraints on the backend's SettingUpsertRequest. */
const MAX_KEY = 150
const MAX_VALUE = 100_000
const MAX_CATEGORY = 80
const MAX_DESCRIPTION = 500

interface SettingFormDialogProps {
  /** The setting being edited, or null when creating a new one. */
  setting: SettingResponse | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: (data: SettingUpsertRequest) => void
  isPending?: boolean
}

/**
 * Create/edit a setting. Both post the same upsert payload — the backend keys on
 * (settingKey + academicYearId), so editing an existing key is an update and a new
 * key is a create.
 *
 * The value is stored as a string but declared by `valueType`; the input is typed
 * to match so administrators aren't hand-writing raw values into a JSON blob.
 */
export function SettingFormDialog({
  setting,
  open,
  onOpenChange,
  onConfirm,
  isPending,
}: SettingFormDialogProps) {
  const isEdit = setting !== null

  const [settingKey, setSettingKey] = useState('')
  const [valueType, setValueType] = useState<SettingValueType>('STRING')
  const [settingValue, setSettingValue] = useState('')
  const [category, setCategory] = useState('')
  const [description, setDescription] = useState('')

  // Reload the form whenever a different setting (or create) is opened.
  useEffect(() => {
    if (!open) return
    setSettingKey(setting?.settingKey ?? '')
    setValueType(setting?.valueType ?? 'STRING')
    setSettingValue(setting?.settingValue ?? '')
    setCategory(setting?.category ?? '')
    setDescription(setting?.description ?? '')
  }, [open, setting])

  const trimmedKey = settingKey.trim()
  const canSubmit = trimmedKey.length > 0 && !isPending

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!canSubmit) return
    onConfirm({
      settingKey: trimmedKey,
      settingValue: settingValue,
      valueType,
      category: category.trim() || undefined,
      description: description.trim() || undefined,
      // Preserved on edit; new settings are created global.
      academicYearId: setting?.academicYearId ?? undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !isPending && onOpenChange(next)}>
      <DialogContent>
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEdit ? 'Edit setting' : 'New setting'}</DialogTitle>
            <DialogDescription>
              {isEdit
                ? 'Update the stored value. The key and its scope cannot be changed.'
                : 'Settings are keyed by name and scope. Reusing an existing key updates it.'}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="setting-key">Key</Label>
              <Input
                id="setting-key"
                value={settingKey}
                onChange={(e) => setSettingKey(e.target.value)}
                placeholder="e.g. placement.max-offers-per-student"
                maxLength={MAX_KEY}
                // The key identifies the setting; changing it would create a new one.
                disabled={isEdit}
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="setting-type">Value type</Label>
              <Select
                value={valueType}
                onValueChange={(v) => setValueType(v as SettingValueType)}
                disabled={isPending}
              >
                <SelectTrigger id="setting-type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {VALUE_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <SettingValueInput
              valueType={valueType}
              value={settingValue}
              onChange={setSettingValue}
            />

            <div className="space-y-2">
              <Label htmlFor="setting-category">Category</Label>
              <Input
                id="setting-category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g. placement"
                maxLength={MAX_CATEGORY}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="setting-description">Description</Label>
              <Textarea
                id="setting-description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What this setting controls"
                maxLength={MAX_DESCRIPTION}
                rows={2}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={!canSubmit}>
              {isPending ? (
                <>
                  <Spinner size="sm" className="mr-2" />
                  Saving…
                </>
              ) : (
                'Save'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** Renders the input appropriate to the declared value type. */
function SettingValueInput({
  valueType,
  value,
  onChange,
}: {
  valueType: SettingValueType
  value: string
  onChange: (next: string) => void
}) {
  if (valueType === 'BOOLEAN') {
    return (
      <div className="flex items-center justify-between">
        <Label htmlFor="setting-value">Value</Label>
        <Switch
          id="setting-value"
          checked={value === 'true'}
          onCheckedChange={(checked) => onChange(String(checked))}
        />
      </div>
    )
  }

  if (valueType === 'JSON') {
    return (
      <div className="space-y-2">
        <Label htmlFor="setting-value">Value</Label>
        <Textarea
          id="setting-value"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="font-mono text-xs"
          rows={5}
          maxLength={MAX_VALUE}
        />
      </div>
    )
  }

  const isNumeric = valueType === 'INTEGER' || valueType === 'LONG' || valueType === 'DECIMAL'

  return (
    <div className="space-y-2">
      <Label htmlFor="setting-value">Value</Label>
      <Input
        id="setting-value"
        type={isNumeric ? 'number' : 'text'}
        step={valueType === 'DECIMAL' ? 'any' : undefined}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        maxLength={isNumeric ? undefined : MAX_VALUE}
      />
    </div>
  )
}
