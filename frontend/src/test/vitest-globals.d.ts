// Adds Vitest's global APIs (describe/it/expect/vi/...) to the type environment
// without a restrictive `types` array in tsconfig — a plain triple-slash reference
// is additive, so it never displaces the existing @types/react / vite/client globals.
/// <reference types="vitest/globals" />
