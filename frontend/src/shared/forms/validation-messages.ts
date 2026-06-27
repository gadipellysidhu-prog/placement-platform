/** Standard validation message strings — use these so error wording is consistent. */
export const VALIDATION_MESSAGES = {
  required: (field?: string) => `${field ?? 'This field'} is required`,
  email: 'Please enter a valid email address',
  minLength: (min: number, field?: string) =>
    `${field ?? 'This field'} must be at least ${min} characters`,
  maxLength: (max: number, field?: string) =>
    `${field ?? 'This field'} must not exceed ${max} characters`,
  min: (min: number) => `Value must be at least ${min}`,
  max: (max: number) => `Value must not exceed ${max}`,
  url: 'Please enter a valid URL',
  passwordStrength: 'Password must be at least 8 characters and contain letters and numbers',
  passwordMatch: 'Passwords do not match',
  uuid: 'Invalid ID format',
  date: 'Please enter a valid date (YYYY-MM-DD)',
  fileType: (types: string) => `File type not allowed. Accepted: ${types}`,
  fileSize: (maxMB: number) => `File exceeds the maximum size of ${maxMB}MB`,
  positiveNumber: 'Value must be a positive number',
  integer: 'Value must be a whole number',
} as const
