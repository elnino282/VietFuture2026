# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ACM Web Platform** - Agricultural Crop Management system with three user roles (Admin, Farmer, Buyer) and a public marketplace. Built with React 18, TypeScript, Vite, and Feature-Sliced Design architecture.

## Common Commands

### Development
```bash
npm run dev              # Start dev server (default port 3000, configurable via PORT/VITE_PORT)
npm run build            # Production build
npm run preview          # Preview production build
```

### Testing
```bash
npm test                 # Run all tests in watch mode
npm run test:ui          # Run tests with Vitest UI
npm run test:coverage    # Generate coverage report
npm run test:fdn         # Run FDN-specific contract + flow tests
npm run test:fdn:contracts  # Run contract tests only
npm run test:fdn:flow    # Run flow tests only
```

### Code Quality
```bash
npm run lint             # Lint source files
npm run lint:fix         # Auto-fix linting issues
npm run typecheck        # TypeScript type checking
npm run check:fsd        # Validate Feature-Sliced Design structure
npm run check:legacy     # Count legacy imports (non-FSD)
npm run check:legacy:baseline  # Check against baseline
npm run check:legacy:update    # Update baseline
```

### Running Single Tests
```bash
npm test -- path/to/test.test.tsx           # Run specific test file
npm test -- --run path/to/test.test.tsx     # Run once without watch mode
```

## Architecture

### Feature-Sliced Design (FSD)

The codebase follows **Feature-Sliced Design** with strict layer hierarchy:

```
src/
├── app/          # Application initialization and routing
├── pages/        # DEPRECATED - being migrated to features/
├── widgets/      # Complex UI blocks composed of features/entities
├── features/     # User-facing features (auth, marketplace, farmer portal, etc.)
├── entities/     # Business entities (crop, farm, order, product, etc.)
└── shared/       # Reusable utilities, UI components, API clients
```

**Import Rules:**
- Layers can only import from layers below them
- `app` → can import from all layers
- `features` → can import from `entities`, `shared`
- `entities` → can import from `shared` only
- `shared` → no cross-imports, only external libraries

**Path Aliases:**
```typescript
@app/*       → src/app/*
@pages/*     → src/pages/*
@widgets/*   → src/widgets/*
@features/*  → src/features/*
@entities/*  → src/entities/*
@shared/*    → src/shared/*
@/*          → src/*
```

### Key Architectural Patterns

**1. Role-Based Routing**
- Three main portals: `/admin/*`, `/farmer/*`, `/buyer/*`
- Public marketplace: `/market/*`
- Route guards via `ProtectedRoute` component with `requiredRole` or `allowedRoles`
- Role types: `admin`, `farmer`, `buyer`, `employee` (lowercase canonical form)

**2. State Management**
- **React Context** for auth/session state (`src/features/auth/context/AuthContext.tsx`)
- **TanStack Query** for server state (entities layer)
- **React Context** for feature-scoped state (e.g., `SeasonContext`)

**3. API Layer**
- Mock adapter pattern: `src/shared/api/marketplace/mock-adapter.ts`
- Contract-based testing: `*.contract.test.ts` files
- Flow testing: `*.flow.test.tsx` files for integration tests
- API proxy configured in `vite.config.ts` (`/api` → `VITE_API_PROXY_TARGET`)

**4. Internationalization (i18n)**
- English (en-US) and Vietnamese (vi-VN)
- Translation files: `public/locales/{en,vi}.json`
- Hook: `useI18n()` from `@/hooks/useI18n`
- Formatters: `useFormatters()` for currency, dates, weights
- See `docs/i18n.md` for detailed guidelines

**5. UI Components**
- Design system based on **Radix UI** + **Tailwind CSS**
- Shared components in `src/shared/ui/`
- Component naming: PascalCase with descriptive names
- Styling: Tailwind utility classes with `cn()` helper

### Testing Strategy

**Test Types:**
1. **Contract tests** (`*.contract.test.ts`) - API contract validation
2. **Flow tests** (`*.flow.test.tsx`) - Integration tests for user flows
3. **Unit tests** (`*.test.tsx`) - Component and utility tests
4. **E2E tests** (`tests/e2e/*.spec.ts`) - End-to-end with Playwright

**Test Patterns:**
- Use `@testing-library/react` for component tests
- Mock data helpers: `createMock*()` functions
- Common mocks in `beforeEach` blocks
- Semantic selectors: `getByRole`, `getByLabel`, `getByText`
- Helper functions for repeated patterns (login, route verification)

### Feature Areas

**Admin Portal** (`src/features/admin/`)
- Buyer management, farmer management
- Marketplace moderation (products, orders)
- System monitoring, reports & analytics

**Farmer Portal** (`src/features/farmer/`)
- Dashboard, season workspace
- Crop management, field logs
- Sales management (marketplace products)

**Buyer Portal** (`src/features/buyer/`)
- Profile management (personal info, addresses, security)
- Order history
- Cart and checkout

**Marketplace** (`src/features/marketplace/`)
- Public product/farm browsing
- Cart, checkout, order tracking
- Traceability features
- Admin moderation UI

**Entities** (`src/entities/`)
- Business logic for: crop, farm, field-log, harvest, incident, inventory
- Nutrient input, irrigation water analysis, soil test
- Dashboard, document, expense, labor, location, notification
- Plot, plot-status, preferences

## Environment Variables

Create `.env.development` or `.env.production` based on `.env.example`:

```bash
PORT=3000                           # Dev server port
VITE_API_PROXY_TARGET=http://localhost:8080  # Backend API URL
```

## Important Notes

### Current Migration Status
- **Legacy `pages/` directory** is being migrated to `features/`
- Use `npm run check:legacy` to track migration progress
- New features should go directly into `features/` layer

### Modal Patterns
- **Destructive actions** (reject, hide, cancel) require modal with reason input
- **Non-destructive actions** (approve, verify) work without modal
- Reason validation: min 10 chars, max 500 chars
- Use `RejectWithReasonModal` component for consistency

### Pagination Pattern
- 0-indexed pages
- Default page size: 25
- Options: 10, 25, 50, 100
- Use `PaginationControls` component
- Sync state with query params

### Authentication
- Firebase Auth integration
- Auth context: `useAuth()` hook
- User roles stored in auth state
- Protected routes redirect unauthenticated users to `/sign-in`

### Code Style
- TypeScript strict mode enabled
- ESLint with import plugin for FSD validation
- Prefer functional components with hooks
- Use `type` over `interface` for consistency
- Avoid `any` - use proper types or `unknown`

## Development Workflow

1. **Starting new features:**
   - Identify correct FSD layer (usually `features/`)
   - Create feature directory with clear naming
   - Add types, components, hooks, pages as needed
   - Export via barrel file (`index.ts`)

2. **Adding tests:**
   - Contract tests for API boundaries
   - Flow tests for user journeys
   - Unit tests for complex logic
   - E2E tests for critical paths

3. **Before committing:**
   - Run `npm run lint:fix`
   - Run `npm run typecheck`
   - Run `npm test` (relevant tests)
   - Check `npm run check:fsd` passes

4. **Marketplace changes:**
   - Update mock adapter if API changes
   - Add contract tests for new endpoints
   - Update types in `src/shared/api/marketplace/types.ts`
   - Test both admin and user-facing flows

## Tech Stack

- **Framework:** React 18 + TypeScript
- **Build:** Vite 6
- **Routing:** React Router 6
- **State:** React Context + TanStack Query
- **UI:** Radix UI + Tailwind CSS
- **Forms:** React Hook Form + Zod
- **i18n:** i18next + react-i18next
- **Auth:** Firebase Auth
- **Testing:** Vitest + Testing Library + Playwright
- **Charts:** Recharts
- **Icons:** Lucide React
