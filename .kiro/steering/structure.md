# Project Structure

> This project is in early setup. Update this file as the structure evolves.

## Expected Top-Level Layout

```
attend-ai/
├── .kiro/                  # Kiro IDE configuration
│   └── steering/           # AI steering rules
├── src/                    # Application source code
│   ├── app/                # App entry points / routing (if Next.js)
│   ├── components/         # Reusable UI components
│   ├── features/           # Feature-based modules (attendance, reports, etc.)
│   ├── lib/                # Shared utilities and helpers
│   ├── services/           # External API integrations and AI services
│   └── types/              # Shared TypeScript types/interfaces
├── public/                 # Static assets
├── tests/                  # Test files (mirror src/ structure)
├── .env.example            # Environment variable template
└── package.json            # Project manifest
```

## Conventions

- Group code by **feature** inside `src/features/`, not by file type
- Keep AI/ML logic isolated in `src/services/` or a dedicated `src/ai/` module
- Co-locate component styles, tests, and types with the component when practical
- Use `index.ts` barrel exports within feature folders
- Environment secrets go in `.env.local` (never committed); document keys in `.env.example`

## Naming

- Files and folders: `kebab-case`
- React components: `PascalCase`
- Functions and variables: `camelCase`
- Types and interfaces: `PascalCase`, interfaces prefixed with `I` only if needed for clarity
