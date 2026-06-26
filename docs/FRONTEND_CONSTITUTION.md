# FRONTEND_CONSTITUTION.md

**Document Title:** FRONTEND CONSTITUTION — Engineering Standards and Governance
**Purpose:** This constitution establishes the immutable engineering principles, architectural standards, and quality expectations for the frontend project. It serves as the authoritative reference for all technical decisions and guides the long-term evolution of the codebase.
**Audience:** All engineers, architects, technical leads, reviewers, and contributors working on the frontend system.
**Status:** Ratified
**Version:** 1.0.0
**Last Updated:** 2026-06-26
**Owner:** Principal Architecture Group
**Scope:** This document governs all frontend engineering activities, including design, development, testing, deployment, and maintenance. It applies to every module, component, service, and integration within the frontend repository.

---

## Table of Contents

1. Purpose
2. Engineering Principles
3. Documentation Hierarchy
4. Frontend Technology Stack
5. Dependency Policy
6. Architecture Philosophy
7. Component Philosophy
8. Routing Philosophy
9. State Management Philosophy
10. API Philosophy
11. Authentication Philosophy
12. UI / UX Philosophy
13. Accessibility Principles
14. Responsive Design Principles
15. Performance Principles
16. Security Principles
17. Error Handling Principles
18. Quality Standards
19. Definition of Done
20. Engineering Rules
21. Long-Term Maintainability
22. References

---

## 1. Purpose

### 1.1 Role of the Constitution

This constitution defines the fundamental engineering principles, architectural decisions, and quality standards that govern the frontend project. It is the highest-level technical document in the repository and SHALL remain stable over the project lifetime.

### 1.2 Relationship to Other Documents

- **MASTER_PROMPT.md** — Provides the overall project vision, objectives, and high-level context. This constitution derives its engineering direction from that vision.
- **CLAUDE.md** — Contains operational workflows, inspection procedures, phase planning, and review processes. This constitution defines the *what* and *why*; CLAUDE.md defines the *how* and *when*.
- **Architecture Documents** — Detailed technical designs (e.g., system diagrams, data flow, component contracts) SHALL align with the principles herein.
- **Planning Documents** — Feature breakdowns, roadmaps, and phase plans SHALL respect the engineering rules and quality gates defined here.
- **Governance Documents** — Coding standards, testing policies, and security guidelines SHALL be derived from this constitution and SHALL NOT contradict it.

### 1.3 Amendment Process

This constitution SHALL change infrequently. Any amendment requires:
- A formal proposal detailing the rationale, impact, and migration plan
- Review by the Principal Architecture Group
- Approval by the Engineering Leadership Team
- A documented version bump and update to all affected downstream documents

---

## 2. Engineering Principles

### 2.1 Maintainability

The codebase SHALL be designed for long-term maintainability. Code SHALL be readable, well‑named, and self‑documenting. Complex logic SHALL be isolated and commented. The system SHALL support the addition of new features without requiring widespread changes.

### 2.2 Scalability

The frontend architecture SHALL scale to support multiple teams, numerous features, and increasing user loads. Modules SHALL be loosely coupled, and performance degradation SHALL be graceful. The system SHALL accommodate future growth in both feature count and user concurrency.

### 2.3 Consistency

All parts of the application SHALL follow consistent patterns for routing, state management, API integration, styling, and error handling. Inconsistency increases cognitive load and technical debt; it MUST be avoided.

### 2.4 Simplicity

Simplicity SHALL be preferred over complexity. Solutions SHALL solve the problem at hand without over‑engineering. Premature abstractions SHALL be avoided. The simplest design that meets requirements SHALL be selected, subject to maintainability and scalability needs.

### 2.5 Modularity

The codebase SHALL be composed of independent, cohesive modules with well‑defined boundaries. Modules SHALL encapsulate their internal implementation and expose minimal, stable interfaces. This enables parallel development, independent deployment, and easier testing.

### 2.6 Traceability

Every decision, architecture change, and implementation SHALL be traceable back to a requirement, a user story, or a documented rationale. This includes API contracts, state design, and dependency selections. Traceability SHALL be maintained through documentation and commit history.

### 2.7 Testability

All code SHALL be designed to be testable. Dependencies SHALL be injectable or mockable. Side effects SHALL be minimized. The test suite SHALL provide high confidence in the correctness of the system and SHALL be fast enough to run frequently.

### 2.8 Accessibility

The application SHALL be usable by people with disabilities. Accessibility is not an afterthought; it SHALL be considered from the design phase and verified at every implementation step. The application SHALL conform to WCAG 2.1 Level AA as a minimum.

### 2.9 Security

Security SHALL be a first‑class concern. The frontend SHALL implement best practices to protect against XSS, CSRF, and other client‑side vulnerabilities. However, the frontend SHALL NOT be the sole security layer; it complements robust backend security.

### 2.10 Performance

Performance SHALL be continuously monitored and optimized. The application SHALL load quickly, respond to user interactions without perceptible delay, and keep resource usage within acceptable budgets.

### 2.11 Developer Experience

The developer experience SHALL be smooth and efficient. Tooling SHALL be well‑configured, documentation SHALL be accurate, and onboarding SHALL be straightforward. Good DX reduces errors and accelerates delivery.

### 2.12 Production Readiness

All code delivered to production SHALL be robust, monitored, and recoverable. The system SHALL handle errors gracefully, provide meaningful feedback, and support rapid incident response.

### 2.13 Long‑Term Evolution

The architecture SHALL be designed to evolve over many years. Technology choices SHALL be stable and well‑supported. Deprecation policies SHALL be defined. The codebase SHALL be continuously refactored to keep pace with changing requirements and technological progress.

---

## 3. Documentation Hierarchy

### 3.1 Precedence Order

When conflicts arise between documents, the following precedence SHALL apply:

1. **Repository** — The actual source code, database schema, and configuration files in the repository are the ultimate source of truth. If documentation contradicts the implementation, the implementation wins.
2. **Backend APIs** — The actual API endpoints, DTOs, and validation rules as defined in the backend repository SHALL take precedence over any frontend‑side assumptions or documentation.
3. **Database Schema** — The current schema (including Flyway migrations) SHALL be authoritative for data structures.
4. **Current Implementation** — The deployed or current development version of the frontend SHALL reflect the actual state.
5. **Architecture Documentation** — High‑level architectural diagrams and design documents.
6. **Planning Documentation** — Feature specifications, roadmaps, and phase plans.
7. **Reference Material** — External standards, library documentation, and best‑practice guides.

### 3.2 Conflict Resolution

If a conflict is identified:
- **Do not silently choose a side.** The conflict SHALL be documented.
- **Provide evidence** for both positions (e.g., code snippet vs. document).
- **Recommend a resolution** based on the principle of least surprise and the overall system integrity.
- **Escalate** to the architecture group if the conflict cannot be resolved at the team level.

---

## 4. Frontend Technology Stack

The following technologies are **mandatory** for all frontend development. Exceptions require architecture group approval.

### 4.1 React 19

- **Responsibility**: Component rendering, lifecycle management, and interactive UI.
- **MUST use**: Functional components, hooks, and concurrent features where beneficial.
- **MUST NOT use**: Class components (deprecated) or React 18‑specific idioms that are superseded in v19.

### 4.2 TypeScript

- **Responsibility**: Static typing for all JavaScript code.
- **MUST use**: Strict mode (`strict: true`), explicit return types, and union/discriminated union types for state discrimination.
- **MUST NOT use**: `any` unless absolutely necessary; if used, it SHALL be isolated and documented with justification.

### 4.3 Vite

- **Responsibility**: Build tooling, development server, and production bundling.
- **MUST use**: Standard Vite configuration with environment variables.
- **MUST NOT use**: Alternative bundlers (Webpack, Parcel, etc.) without formal exception.

### 4.4 Tailwind CSS

- **Responsibility**: Utility‑first styling for rapid UI development.
- **MUST use**: Tailwind classes for all styling; custom CSS only for complex animations or third‑party overrides.
- **MUST NOT use**: Inline styles or global CSS (except reset and base styles provided by Tailwind).

### 4.5 shadcn/ui

- **Responsibility**: Provide accessible, reusable UI components built on Radix UI and Tailwind.
- **MUST use**: As the primary component library for forms, dialogs, dropdowns, tables, etc.
- **MUST NOT use**: Other UI component libraries (Material‑UI, Ant Design, Chakra, etc.) unless approved and integrated.

### 4.6 TanStack Query

- **Responsibility**: Server‑state management, caching, synchronization, and background updates.
- **MUST use**: For all asynchronous data fetching from the backend (GET, POST, PUT, DELETE).
- **MUST NOT use**: For local UI state or client‑only data. That belongs to Zustand or local component state.

### 4.7 React Router

- **Responsibility**: Client‑side routing, nested routes, and route protection.
- **MUST use**: For all navigation and URL management.
- **MUST NOT use**: Custom routing implementations or other routing libraries.

### 4.8 React Hook Form

- **Responsibility**: Form management, validation, and submission handling.
- **MUST use**: For all forms (login, registration, data entry, filters, etc.).
- **MUST NOT use**: Controlled forms with `useState` for complex forms (simple forms may use `useState` if no validation is required, but prefer React Hook Form for consistency).

### 4.9 Zod

- **Responsibility**: Schema declaration and validation, both for forms and API responses.
- **MUST use**: To validate incoming data, define form schemas, and enforce runtime types.
- **MUST NOT use**: Other validation libraries (Yup, Joi, etc.) unless required by integration.

### 4.10 Axios

- **Responsibility**: HTTP client for API requests, with interceptors for authentication and error handling.
- **MUST use**: For all backend communication.
- **MUST NOT use**: `fetch` directly (except for streaming or special cases, which require approval).

### 4.11 Zustand

- **Responsibility**: Client‑side state management for application‑wide UI state, user preferences, session data, and other local state.
- **MUST use**: For global client state that is not server‑cached.
- **MUST NOT use**: Context API or Redux for new features; legacy contexts may be refactored.

---

## 5. Dependency Policy

Every dependency introduced into the project SHALL be evaluated against the following criteria. A dependency SHALL not be added without documented justification.

### 5.1 Evaluation Criteria

- **Purpose**: What problem does the dependency solve? Is it essential?
- **Alternatives**: What existing dependencies or custom solutions could be used instead? Why are they insufficient?
- **Maintenance Status**: Is the library actively maintained? What is the issue response time and release cadence?
- **Bundle Impact**: What is the minified/gzipped size addition? Does it affect code‑splitting?
- **Compatibility**: Does it work with our current stack (React 19, TypeScript, Vite)? Are there known peer‑dependency conflicts?
- **Reason Selected**: Given the alternatives, why is this the best choice?
- **Long‑Term Risk**: What is the risk of abandonment, API churn, or security vulnerabilities? What is our exit strategy?
- **Removal Strategy**: If we decide to remove or replace this dependency in the future, what steps would be required?

### 5.2 Approval Process

- All new dependencies (except development tooling) SHALL be reviewed by at least one senior engineer and logged in a central dependency registry.
- The review SHALL be documented in the pull request that introduces the dependency.
- Dependencies that are large, introduce new paradigms, or have high risk SHALL require architecture group approval.

### 5.3 Prohibited Dependencies

- Libraries with incompatible licenses (e.g., GPL without exception)
- Libraries with known security vulnerabilities that are not patched
- Libraries that are deprecated or have not been updated in over 18 months
- Libraries that duplicate existing functionality (unless the existing solution is clearly inferior)

---

## 6. Architecture Philosophy

### 6.1 Feature‑Based Architecture

The codebase SHALL be organised by feature rather than by technical layer. A feature module contains all components, hooks, services, and tests relevant to that feature. This promotes cohesion and reduces cross‑feature coupling.

### 6.2 Modularity

Every module SHALL have a single, well‑defined responsibility. Modules SHALL expose a public API (index file) and hide internal implementation details. This enables independent development and testing.

### 6.3 Composition Over Inheritance

React components and business logic SHALL favour composition (higher‑order components, render props, hooks) over inheritance. Inheritance hierarchies SHALL be shallow and only used for true is‑a relationships.

### 6.4 Separation of Concerns

- **Presentation** (components) SHALL be separated from **logic** (hooks, services) and **state** (stores, queries).
- **Data access** (API clients, TanStack Query) SHALL be separate from UI components.
- **Routing** SHALL be defined in a central configuration, not scattered throughout the codebase.

### 6.5 Dependency Direction

Dependencies SHALL flow inward:
- Feature modules MAY depend on shared modules (UI components, utilities, constants).
- Shared modules MUST NOT depend on feature modules.
- UI components MUST NOT depend on specific business logic unless they are part of that feature.

### 6.6 Shared vs. Feature Modules

- **Shared modules** (`/shared`) contain reusable UI components, utilities, hooks, and constants that are used across multiple features.
- **Feature modules** (`/features/<feature-name>`) contain everything specific to that feature, including pages, components, hooks, and services.
- Shared modules SHALL be stable and well‑tested; they SHALL NOT reference feature modules.

### 6.7 Layer Boundaries

The architecture SHALL enforce clear layers:
- **UI Layer** — Components, layouts, pages.
- **State Layer** — Zustand stores, TanStack Query hooks.
- **Service Layer** — API clients, utility functions, business logic.
- **Data Layer** — TanStack Query caching, local storage, etc.

Each layer SHALL only depend on layers below it; no upward dependencies.

### 6.8 No Cyclic Dependencies

Cyclic dependencies are forbidden. They SHALL be detected by tooling (e.g., `madge`) and resolved before merging.

### 6.9 Long‑Term Scalability

The architecture SHALL support:
- Adding new features without refactoring existing ones.
- Splitting the codebase into independently deployable micro‑frontends if needed in the future.
- Evolving the UI framework (React) without rewriting the entire application.

---

## 7. Component Philosophy

### 7.1 Component Hierarchy

Components SHALL follow a clear hierarchy:

1. **Shared UI Components** (`/shared/ui`) — Generic, presentation‑only components (Button, Input, Modal, Card) with no business logic. These are the building blocks.
2. **Business Components** (`/shared/business`) — Reusable components that encapsulate domain‑specific behaviour (e.g., UserAvatar, ProductCard).
3. **Feature Components** (`/features/<feature>/components`) — Components that are used only within a specific feature.
4. **Page Components** (`/features/<feature>/pages`) — Top‑level components that correspond to routes; they compose feature and shared components.
5. **Layouts** (`/layouts`) — Structural wrappers (e.g., DashboardLayout, AuthLayout) that define page structure and contain shared elements like navigation and footers.

### 7.2 Composition

Components SHALL be composed using props, children, and slots. Prefer composition over config props for complex customisation.

### 7.3 Variants

For shared components with multiple appearances (e.g., Button variants: primary, secondary, destructive), define a `variant` prop with a union type. Use Tailwind classes to implement variants.

### 7.4 Extensibility

Components SHALL be extendable via className props, as prop overrides, and by forwarding refs. Avoid internal state that cannot be overridden.

### 7.5 Accessibility

All interactive components SHALL be accessible by default: proper semantic HTML, ARIA attributes, keyboard navigation, and focus management.

### 7.6 Reusability

Before creating a new component, assess whether an existing component can be reused or adapted. Duplication of component logic is prohibited; if a component appears in multiple places, it SHALL be extracted.

### 7.7 Rules Against Duplication

- No two components SHALL implement the same visual or behavioural pattern.
- No two components SHALL contain the same business logic.
- If a component is copied and modified, the original SHALL be refactored to support the new use case, or a new abstraction SHALL be created.

---

## 8. Routing Philosophy

### 8.1 Nested Routing

Routes SHALL be nested to reflect the UI hierarchy (e.g., `/dashboard/settings/profile`). React Router’s nested `<Outlet>` mechanism SHALL be used for layout composition.

### 8.2 Protected Routing

Routes that require authentication SHALL be guarded by a `<ProtectedRoute>` component that checks authentication status and redirects to `/login` if unauthenticated.

### 8.3 Role‑Based Routing

Routes that require specific roles SHALL be guarded by a `<RoleRoute>` component that checks the user’s role and redirects to a `403` page if insufficient.

### 8.4 Public Routes

Public routes (login, register, password reset, etc.) SHALL be accessible without authentication. If a user is already authenticated, they SHALL be redirected away from these routes (e.g., to `/dashboard`).

### 8.5 Authenticated Routes

Authenticated routes SHALL be grouped under a common layout that includes navigation, user menu, and other authenticated‑only elements.

### 8.6 Layout Routes

Layouts SHALL be defined as parent routes that wrap their children. This avoids repeated layout code in each page.

### 8.7 Error Routes

A catch‑all error route (`*`) SHALL be defined for 404 pages. Additionally, error boundaries SHALL be used at the route level to handle rendering errors gracefully.

### 8.8 Route Constants

All route paths SHALL be defined as constants in a single file (`/constants/routes.ts`). This prevents hardcoded strings and makes refactoring easier.

### 8.9 Navigation Consistency

Navigation menus SHALL be generated from a central configuration that maps routes to labels, icons, and permissions. This ensures that the sidebar and header navigation are always in sync.

### 8.10 No Hardcoded Routes

Route paths SHALL NOT be hardcoded in components; they SHALL be imported from route constants. This applies to `Link`, `useNavigate`, and programmatic navigation.

---

## 9. State Management Philosophy

### 9.1 Responsibilities

The state management strategy is partitioned as follows:

| **State Type** | **Technology** | **Responsibility** |
|----------------|----------------|---------------------|
| Server state (data fetched from API) | TanStack Query | Caching, background updates, optimistic updates, pagination, invalidation. |
| Global client state (user, theme, UI settings, notifications) | Zustand | Application‑wide UI state that is not server‑cached. |
| Form state | React Hook Form + Zod | Form values, validation, submission, and error display. |
| Local component state | `useState` / `useReducer` | Ephemeral state that affects only a single component (e.g., toggle visibility, input focus). |
| Derived state | `useMemo`, `useSelector` | Computed values based on other state; avoid storing derived state. |

### 9.2 TanStack Query

- **Use for**: All GET requests, mutations (POST, PUT, DELETE), and any data that comes from the backend.
- **Caching**: Use `staleTime` and `cacheTime` appropriately to balance freshness and performance.
- **Synchronization**: Use `invalidateQueries` after mutations to update dependent queries.
- **Optimistic updates**: Implement for better UX, but ensure rollback on error.
- **Prefetching**: Use for critical data to reduce loading times.

### 9.3 Zustand

- **Use for**: User session information, theme preference, notifications, sidebar state, and any other global UI state.
- **Structure**: Define slices for different domains (e.g., `userSlice`, `uiSlice`). Use `immer` for immutable updates.
- **Persistence**: Use `persist` middleware for state that should survive page refresh (e.g., theme, authentication token).

### 9.4 React Hook Form

- **Use for**: All forms, including validation with Zod schemas.
- **Validation**: Define Zod schemas and integrate with `zodResolver`.
- **Error handling**: Display field‑level and form‑level errors clearly.

### 9.5 Avoid Duplicated State

- Never store the same piece of data in multiple stores.
- Server state (from TanStack Query) SHALL NOT be duplicated in Zustand.
- Derived state SHALL be computed, not stored.

---

## 10. API Philosophy

### 10.1 Repository as API Source of Truth

The backend repository SHALL be the sole source of truth for API contracts. The frontend SHALL NOT invent or assume endpoints, DTOs, or behaviour. All integration SHALL be based on actual inspection of the backend code.

### 10.2 DTO Usage

- All API responses SHALL be validated against Zod schemas that mirror backend DTOs.
- DTOs SHALL be defined in a separate file per domain and shared across services.
- The frontend SHALL transform DTOs into internal models only when necessary (e.g., for UI formatting).

### 10.3 Validation

- Incoming API responses SHALL be validated using Zod to ensure data integrity.
- Validation errors SHALL be caught and handled gracefully, with appropriate user feedback and logging.

### 10.4 Error Handling

- Use Axios interceptors to handle common errors (e.g., 401 → redirect to login, 500 → show generic error).
- All API calls SHALL have error handling that displays user‑friendly messages.
- Network errors SHALL be distinguished from server errors.

### 10.5 Response Mapping

- Transform API responses to UI‑friendly data shapes in the service layer, not in components.
- Use `select` options in TanStack Query to transform data on the client side where appropriate.

### 10.6 Pagination

- Use pagination strategies consistent with the backend (page-based, cursor‑based).
- Implement infinite scrolling or page buttons using TanStack Query’s pagination capabilities.

### 10.7 Authentication

- Include the access token in the `Authorization` header for all authenticated requests.
- Use Axios interceptors to attach the token.
- Handle token refresh (see Section 11).

### 10.8 Authorization

- The frontend SHALL display/hide UI elements based on user roles and permissions, but SHALL NOT rely on this for security—backend SHALL enforce all authorization.
- Permissions SHALL be derived from the user object (retrieved from the session).

### 10.9 Retries

- Implement retry logic (with exponential backoff) for idempotent GET requests that fail due to network issues.
- Mutations SHALL NOT be retried automatically unless they are idempotent; otherwise, provide a manual retry button.

### 10.10 Caching

- Leverage TanStack Query’s cache to reduce redundant API calls.
- Set appropriate `staleTime` to avoid unnecessary requests while keeping data fresh.
- Use cache invalidation after mutations to ensure consistency.

### 10.11 Never Invent APIs

- Do not create mock or placeholder endpoints.
- Do not assume the structure of a response without verifying it in the backend.
- If an API is missing, document the gap and request it from the backend team.

---

## 11. Authentication Philosophy

### 11.1 JWT

- The backend issues JSON Web Tokens (JWT) for authentication.
- The frontend stores the access token securely (in memory; may be persisted in localStorage with caution).
- The token is included in the `Authorization: Bearer <token>` header for all authenticated requests.

### 11.2 Refresh Tokens

- When the access token expires, the frontend SHALL automatically attempt to refresh it using a refresh token (stored in an HTTP‑only cookie or similar secure storage).
- The refresh mechanism SHALL be transparent to the user (silent refresh).
- If refresh fails, the user SHALL be logged out.

### 11.3 Protected Routes

- Routes that require authentication SHALL use a guard component that verifies the user’s authentication status.
- If the user is not authenticated, they SHALL be redirected to the login page with a return URL.

### 11.4 Session Persistence

- The authentication state SHALL persist across page reloads by re‑hydrating from the stored token and validating it with the backend.
- Zustand SHALL hold the user object and authentication status; it SHALL be rehydrated from localStorage or sessionStorage.

### 11.5 Logout

- Logout SHALL clear the authentication state, remove tokens from storage, and redirect to the login page.
- All pending requests SHALL be cancelled or ignored upon logout.

### 11.6 Role Awareness

- The user object SHALL contain the user’s roles and permissions.
- UI elements SHALL be conditionally rendered based on these roles.
- The guard component SHALL enforce role requirements at the route level.

### 11.7 Permission Boundaries

- The frontend SHALL use permissions for UX (e.g., hiding buttons), but the backend SHALL enforce all permissions.
- Permission checks SHALL be centralised in a `usePermissions` hook that reads from the user store.

### 11.8 Frontend Responsibilities

- The frontend SHALL: manage the login flow, store tokens, refresh tokens, and route protection.
- The frontend SHALL NOT: generate or validate tokens, perform sensitive operations without backend confirmation.

### 11.9 Backend Responsibilities

- The backend SHALL: issue tokens, validate tokens, refresh tokens, and enforce all authorization policies.
- The backend SHALL be the sole authority on authentication.

---

## 12. UI / UX Philosophy

### 12.1 Enterprise SaaS Experience

The UI SHALL deliver a professional, polished, and efficient experience suitable for enterprise customers. It SHALL be inspired by modern SaaS products such as Linear, Vercel, GitHub, Stripe Dashboard, and Notion.

### 12.2 Visual Hierarchy

- Information SHALL be organised with clear visual hierarchies: headings, subheadings, body text, and metadata.
- The most important actions SHALL be visually prominent (primary buttons).
- Secondary and tertiary actions SHALL be less prominent (secondary, ghost, or icon buttons).

### 12.3 Spacing

- Consistent spacing tokens SHALL be used (based on a scale, e.g., 4px, 8px, 16px, 24px, 32px).
- Vertical and horizontal spacing SHALL be deliberate to improve readability and reduce clutter.

### 12.4 Typography

- A single sans‑serif font family SHALL be used (e.g., Inter, SF Pro).
- Font sizes, weights, and line heights SHALL follow a defined scale.
- Text SHALL be legible with sufficient contrast against backgrounds.

### 12.5 Color System

- A well‑defined color palette SHALL be used, including primary, secondary, success, warning, danger, and neutral shades.
- Colors SHALL be applied consistently: primary for action, success for confirmations, danger for destructive actions, etc.
- Dark mode SHALL be supported (the design system SHOULD include both light and dark themes).

### 12.6 Icons

- A single icon set SHALL be used consistently (e.g., Lucide Icons).
- Icons SHALL be used to reinforce meaning, not as decoration.
- Icon sizes SHALL be relative to the surrounding text.

### 12.7 Motion

- Motion SHALL be minimal and purposeful: e.g., subtle transitions, loading states, and hover feedback.
- Animations SHALL be smooth (60fps) and SHALL NOT be distracting.
- Reduced motion preferences SHALL be respected using the `prefers-reduced-motion` media query.

### 12.8 Micro‑interactions

- Feedback on user actions SHALL be immediate: hover states, focus rings, loading spinners, and success/error toasts.
- Buttons SHALL show a loading state when submitting to prevent double‑clicks.

### 12.9 Feedback

- All user actions SHALL have clear feedback: success messages, error messages, and confirmation dialogs for destructive actions.
- Toast notifications SHALL be used for non‑critical feedback; inline messages for form errors.

### 12.10 Consistency

- The UI SHALL be consistent across all pages: navigation, forms, modals, tables, and buttons.
- Components SHALL follow the same design language defined by shadcn/ui.

### 12.11 Professional Appearance

- The interface SHALL be clean, uncluttered, and visually appealing.
- Excessive whitespace is acceptable; it improves readability.
- The overall impression SHALL be that of a mature, enterprise‑grade product.

### 12.12 Avoid

- **Heavy glassmorphism** or excessive transparency — these degrade readability and feel trendy.
- **Neumorphism** — it’s visually ambiguous and lacks accessibility.
- **Over‑animation** — constant motion distracts and slows down users.
- **Visual clutter** — avoid too many colours, mismatched styles, or crowded layouts.

---

## 13. Accessibility Principles

### 13.1 WCAG Compliance

The application SHALL conform to the Web Content Accessibility Guidelines (WCAG) 2.1 Level AA as a minimum. Level AAA compliance is encouraged where feasible.

### 13.2 Semantic HTML

- Use semantic HTML elements (`<header>`, `<nav>`, `<main>`, `<section>`, `<article>`, `<aside>`, `<footer>`) to convey structure.
- Use `<button>` for clickable actions, `<a>` for links, `<h1>`–`<h6>` for headings.
- Avoid `<div>` and `<span>` for interactive elements unless absolutely necessary and properly ARIA‑labelled.

### 13.3 Keyboard Navigation

- All interactive elements SHALL be reachable and operable via keyboard (Tab, Enter, Space, Escape).
- Focus order SHALL follow a logical sequence matching the visual layout.
- Visible focus indicators SHALL be provided (focus rings).

### 13.4 Screen Reader Support

- All meaningful content SHALL be announced by screen readers using appropriate ARIA attributes.
- Use `aria-label`, `aria-labelledby`, `aria-describedby` to provide context.
- Use `aria-live` regions for dynamic content updates (e.g., toast notifications).

### 13.5 ARIA Usage

- ARIA SHALL be used only when native HTML semantics are insufficient.
- Follow the WAI‑ARIA Authoring Practices 1.2 for common patterns (accordion, tabs, modal, etc.).

### 13.6 Focus Management

- Manage focus when navigating via routes, opening modals, and closing dialogs (return focus to the previous element).
- Use the `useFocus` hook or similar to programmatically manage focus when necessary.

### 13.7 Contrast

- All text SHALL meet WCAG contrast requirements: at least 4.5:1 for normal text, 3:1 for large text.
- UI components SHALL also meet contrast requirements for their states (hover, active, disabled).

### 13.8 Responsive Typography

- Font sizes SHALL be scalable; use `rem` units to respect browser zoom settings.
- Text SHALL NOT be clipped or hidden when enlarged; ensure reflow.

### 13.9 Touch Targets

- Interactive elements SHALL have a minimum touch target size of 44×44 CSS pixels (following WCAG 2.5.5).

### 13.10 Accessible Forms

- Every form input SHALL have an associated `<label>` (visible or via `aria-label`).
- Error messages SHALL be announced to screen readers using `aria-describedby` or `role="alert"`.
- Field validation errors SHALL be displayed clearly and in a timely manner.

### 13.11 Accessible Tables

- Tables SHALL have proper `<caption>` and `<th>` elements with `scope` attributes.
- Complex tables SHALL use `aria-describedby` to provide explanations.

### 13.12 Accessible Dialogs

- Modals SHALL trap focus within the dialog.
- They SHALL be announced as dialogs using `role="dialog"` and `aria-modal="true"`.
- The dialog SHALL have `aria-labelledby` pointing to the title.

---

## 14. Responsive Design Principles

### 14.1 Desktop‑First

The default design SHALL target desktop screens (≥ 1024px). Responsiveness SHALL be applied to adapt to smaller screens, not the other way around. This ensures the richest experience for the primary enterprise audience.

### 14.2 Fully Responsive

The application SHALL be fully functional and usable on tablets and mobile phones. All pages, tables, forms, and navigation SHALL adapt gracefully.

### 14.3 Breakpoints

Use the following standard breakpoints (aligned with Tailwind):

| Breakpoint | Prefix | Min‑width |
|------------|--------|-----------|
| sm         | `sm:`  | 640px     |
| md         | `md:`  | 768px     |
| lg         | `lg:`  | 1024px    |
| xl         | `xl:`  | 1280px    |
| 2xl        | `2xl:` | 1536px    |

Components SHALL use these breakpoints consistently.

### 14.4 Layout Adaptation

- Sidebars SHALL collapse to a hamburger menu on small screens.
- Multi‑column layouts SHALL stack into single columns.
- Navigation SHALL be accessible via a bottom or top drawer on mobile.

### 14.5 Tables

- Tables SHALL be horizontally scrollable on small screens, or use card‑based alternatives for each row.
- Ensure table headers are sticky for long tables.

### 14.6 Navigation

- Primary navigation SHALL be persistent and accessible on all screen sizes.
- Use a drawer for mobile navigation; ensure it is keyboard‑accessible.

### 14.7 Dialogs

- Dialogs SHALL be full‑screen on mobile (or near‑full) for better touch interaction.
- On larger screens, they SHALL be centered with a max‑width.

### 14.8 Cards

- Card layouts SHALL adapt from multi‑column to single‑column on small screens.
- Card content SHALL remain readable; avoid text overflow.

### 14.9 Forms

- Form fields SHALL be full‑width on mobile.
- Labels SHALL remain visible; avoid placeholder‑as‑label.

### 14.10 Charts

- Charts SHALL be responsive and maintain legibility at all sizes.
- Tooltips SHALL be touch‑friendly.

### 14.11 Mobile Interaction

- Touch targets SHALL be at least 44px.
- Avoid hover‑only interactions on mobile; provide tap feedback.

---

## 15. Performance Principles

### 15.1 Lazy Loading

- Feature modules SHALL be lazy‑loaded using React’s `lazy` and Suspense.
- Route‑based code splitting SHALL be implemented to reduce initial bundle size.

### 15.2 Route Splitting

- Each route SHALL have its own chunk, loaded only when the route is visited.
- Use dynamic imports for components that are not immediately needed.

### 15.3 Bundle Optimization

- Analyze the bundle using tools like `vite-bundle-analyzer`.
- Eliminate duplicate dependencies and use tree‑shaking.
- Keep the initial load bundle under 200KB (gzipped) for first‑time visitors.

### 15.4 Memoization

- Use `React.memo` for components that re‑render often with the same props.
- Use `useMemo` and `useCallback` to avoid unnecessary calculations and function re‑creation.

### 15.5 Image Optimization

- Images SHALL be served in modern formats (WebP, AVIF) when possible.
- Use `srcset` and `sizes` for responsive images.
- Lazy‑load off‑screen images using the `loading="lazy"` attribute.

### 15.6 Virtualization

- For long lists, use virtualization (e.g., `react-window`, `react-virtual`) to render only visible items.
- This applies to tables, dropdowns with many options, and infinite scroll.

### 15.7 Caching

- Use TanStack Query’s caching to avoid redundant network requests.
- Cache static assets with appropriate Cache‑Control headers.

### 15.8 Suspense

- Use Suspense boundaries at the route level to show fallback loaders.
- Combined with React Query’s `useSuspenseQuery` for data‑fetching.

### 15.9 Performance Budgets

Set and enforce the following budgets:

- First Contentful Paint (FCP) < 1.5s
- Largest Contentful Paint (LCP) < 2.5s
- Time to Interactive (TTI) < 3.5s
- Total bundle size (gzip) < 500KB for all assets combined
- Lighthouse performance score ≥ 90

### 15.10 Lighthouse Goals

- Performance ≥ 90
- Accessibility ≥ 95
- Best Practices ≥ 95
- SEO ≥ 90 (where applicable)

---

## 16. Security Principles

### 16.1 XSS Prevention

- Always sanitize user‑generated content before rendering.
- Use React’s built‑in escaping (JSX escapes by default) and avoid `dangerouslySetInnerHTML` unless absolutely necessary; if used, sanitize the HTML.
- Never trust user input; validate and encode appropriately.

### 16.2 Token Handling

- Access tokens SHALL be stored in memory where possible. If stored in localStorage or sessionStorage, ensure the token is short‑lived and refresh token rotation is used.
- Tokens SHALL NOT be exposed in URLs.
- Use HTTP‑only cookies for refresh tokens when feasible.

### 16.3 Input Validation

- All user input SHALL be validated on the frontend (for UX) AND on the backend (for security).
- Frontend validation SHALL NOT be the sole defence; it is a convenience.

### 16.4 Upload Security

- File uploads SHALL be restricted by type and size on the frontend.
- Validate the file’s MIME type and extension before sending.

### 16.5 Safe Rendering

- Avoid inline event handlers that evaluate strings (e.g., `onclick="..."`).
- Use CSP (Content Security Policy) headers to restrict sources of scripts and styles.

### 16.6 Content Security Policy Compatibility

- The frontend SHALL be compatible with a strict CSP that disallows `unsafe-inline` and `unsafe-eval`.
- Use nonces or hashes for inline scripts.

### 16.7 Authorization Awareness

- The frontend SHALL check permissions for UI rendering, but SHALL NOT rely on them for security.
- All sensitive operations SHALL be re‑authorized by the backend.

### 16.8 Frontend Security Complements Backend

- The frontend is a client; it can be inspected and manipulated. Therefore, all critical security decisions (authentication, authorization, data validation) MUST be implemented on the backend. The frontend’s security measures are additional layers to improve UX and reduce unnecessary backend calls.

---

## 17. Error Handling Principles

### 17.1 Global Errors

- Use an error boundary at the top level of the app to catch unhandled rendering errors and display a fallback UI.
- Log these errors to an external service (e.g., Sentry) for monitoring.

### 17.2 API Errors

- All API calls SHALL have error handling that differentiates between network errors, 4xx client errors, and 5xx server errors.
- Show user‑friendly messages for common errors (e.g., “Something went wrong. Please try again.”).
- For 401, automatically trigger a token refresh or logout.

### 17.3 Validation Errors

- Display validation errors inline, near the offending field, with clear messages.
- Use React Hook Form’s error handling to display `errors` objects.

### 17.4 Network Errors

- Detect offline status using `navigator.onLine` and show appropriate messages.
- Retry failed requests with exponential backoff for idempotent operations.

### 17.5 Authentication Errors

- Handle token expiration gracefully by attempting a silent refresh.
- If refresh fails, redirect to login with a message.

### 17.6 Unexpected Exceptions

- All uncaught exceptions SHALL be captured by the error boundary.
- The error boundary SHALL display a generic “We encountered an error” message and provide a “Reload” button.

### 17.7 User Feedback

- All error states SHALL provide actionable feedback: what went wrong and what the user can do (e.g., “Try again”).
- Toast notifications or banner alerts SHALL be used for non‑critical errors.

### 17.8 Logging

- Errors SHALL be logged to the console during development.
- In production, errors SHALL be sent to a logging service with context (user, route, action, stack trace).

### 17.9 Recovery

- Where possible, provide a way to recover from errors without reloading the entire page.
- Use fallback data or stale cache when available.

---

## 18. Quality Standards

### 18.1 TypeScript

- TypeScript strict mode (`strict: true`) SHALL be enabled.
- All code SHALL have explicit type annotations for function parameters and return types.
- The project SHALL have zero TypeScript errors.
- The use of `any` is prohibited except in tightly scoped, documented cases.

### 18.2 Linting

- ESLint with the recommended rules and TypeScript support SHALL be used.
- The configuration SHALL include rules for React hooks (exhaustive‑deps), accessibility (jsx‑a11y), and import ordering.
- The codebase SHALL have zero ESLint errors; warnings SHALL be reviewed and fixed.

### 18.3 Formatting

- Prettier SHALL be used for automatic code formatting.
- The configuration SHALL be standardised across the team.
- All code SHALL be formatted before commit (pre‑commit hook).

### 18.4 Testing

- **Unit tests** SHALL cover business logic, utilities, and hooks. Use Vitest.
- **Component tests** SHALL cover UI components with user interactions. Use React Testing Library.
- **Integration tests** SHALL cover feature workflows and API integration.
- **E2E tests** (optional but recommended) using Cypress or Playwright for critical user journeys.
- Test coverage SHALL be at least 80% for all new code, with a focus on critical paths.

### 18.5 Accessibility

- Automated accessibility testing SHALL be run using `axe` or `@testing-library/jest-dom` extensions.
- Manual accessibility reviews SHALL be performed for new features.

### 18.6 Responsiveness

- All UI changes SHALL be verified on multiple screen sizes using browser dev tools.
- Visual regression tests SHALL be considered for critical components.

### 18.7 Documentation

- Every public API, complex component, and critical hook SHALL have JSDoc comments.
- The `README.md` and `CLAUDE.md` SHALL be kept up to date.
- Architecture decisions SHALL be documented in ADRs (Architecture Decision Records).

### 18.8 Maintainability

- Code complexity SHALL be kept in check: avoid deeply nested conditionals, large functions (>30 lines), and duplicate code.
- Use code‑quality tools (SonarQube or similar) to track maintainability metrics.

---

## 19. Definition of Done

A feature or user story is **done** only when all of the following conditions are met:

1. **Implementation Complete** — All code for the feature has been written and is committed.
2. **Backend Integration Verified** — All API calls have been verified against the actual backend, including error handling.
3. **Tests Passing** — All unit, integration, and component tests pass; coverage meets targets.
4. **Documentation Updated** — All affected documents (README, API docs, user guides) have been updated.
5. **Accessibility Reviewed** — The feature has been checked for WCAG compliance (automated and manual).
6. **Responsive Verified** — The feature works correctly on all supported breakpoints.
7. **No Duplicate Code** — No duplicate logic or components exist; any duplication has been refactored.
8. **No Architectural Violations** — The implementation does not violate any architecture principles (e.g., dependency direction, layer boundaries).
9. **Quality Gates Passed** — All quality gates (build, lint, typecheck, test, etc.) have passed.
10. **Production Ready** — The feature is deployable to production without additional work; all known issues are resolved or documented as acceptable limitations.

---

## 20. Engineering Rules

The following rules are **immutable** and SHALL be enforced at all times:

### 20.1 API Rules

- **Never invent APIs.** All API endpoints, DTOs, and validation rules SHALL be derived from actual backend inspection.
- **Never assume backend functionality.** If it’s not in the repository, it doesn’t exist.

### 20.2 Component Rules

- **Never duplicate components.** If a component is reused, extract it to a shared module.
- **Never duplicate business logic.** Extract logic to hooks or services.

### 20.3 TypeScript Rules

- **Never bypass TypeScript.** Avoid `any`; if unavoidable, document the justification and isolate it.
- **Never suppress compiler errors.** Fix them.

### 20.4 Routing Rules

- **Never hardcode routes.** Use route constants.
- **Never hardcode permissions.** Use permission constants and hooks.
- **Never hardcode roles.** Use role constants.

### 20.5 Linting Rules

- **Never disable lint rules to avoid fixing issues.** Only disable with a comment explaining why, and preferably file a follow‑up task to fix.

### 20.6 Error Handling Rules

- **Never skip loading, empty, success, or error states.** Every data‑fetching component SHALL handle all these states.

### 20.7 Architecture Rules

- **Never introduce undocumented architectural changes.** Any architectural change SHALL be documented in an ADR.
- **Prefer extension over duplication.** Use composition, inheritance, and hooks to extend behaviour.
- **Prefer composition over inheritance.** Use hooks and higher‑order components over deep inheritance hierarchies.

### 20.8 Source of Truth Rules

- **Repository is the implementation source of truth.** If documentation conflicts with the repository, the repository wins.

---

## 21. Long‑Term Maintainability

### 21.1 Refactoring

Refactoring SHALL be an ongoing activity, not a one‑time event. The codebase SHALL be continuously improved to reduce technical debt, improve readability, and accommodate new requirements. Refactoring SHALL be done in small, incremental steps with full test coverage.

### 21.2 Deprecation

When a library, API, or feature is deprecated:
- Announce the deprecation with a timeline.
- Provide migration guides.
- Remove deprecated code only after the migration period and after verifying no usage.

### 21.3 Technical Debt

Technical debt SHALL be tracked and prioritized. Each debt item SHALL have a clear description, impact, and estimated effort. Debt SHALL be reviewed regularly and scheduled for repayment.

### 21.4 Backward Compatibility

The frontend SHALL maintain backward compatibility with existing backend APIs and data formats. Breaking changes SHALL be coordinated with the backend team and deployed together.

### 21.5 Documentation Synchronization

Documentation SHALL be kept in sync with the implementation. Any code change SHALL trigger a review of relevant documentation to ensure accuracy. Outdated documentation is a source of confusion and errors.

### 21.6 Knowledge Sharing

- Regular tech talks and code reviews SHALL facilitate knowledge transfer.
- Onboarding materials SHALL be updated and comprehensive.
- Pair programming SHALL be encouraged for complex features.

### 21.7 Architecture Evolution

The architecture SHALL evolve to meet new challenges. Proposals for change SHALL follow the ADR process and be evaluated against the principles of this constitution. Evolution SHALL be deliberate and controlled.

---

## 22. References

### 22.1 Referenced Documents

The following documents are referenced by this constitution. Their contents are not duplicated but are complementary.

**Governance and Operational Documents**
- `MASTER_PROMPT.md` — Overall project vision and objectives.
- `CLAUDE.md` — Operational workflows, planning, and development processes.

**Architecture and Planning Documents**
- System Architecture Document
- Component Design Document
- API Integration Plan
- Data Model
- Routing Design Document
- Styling Strategy
- Testing Strategy
- Quality Standards
- Deployment Plan
- Risk Assessment

**Governance Documents**
- Coding Standards (ESLint/Prettier config)
- Testing Policy
- Security Guidelines
- Accessibility Guidelines

**Quality Documents**
- Project Health Report
- Phase Reports
- Quality Gate Reports

### 22.2 Document Management

- All referenced documents SHALL reside in the repository and be versioned.
- References SHALL be maintained as the documents evolve.
- Out‑of‑date references SHALL be updated during regular reviews.

---

*End of Constitution*