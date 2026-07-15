# HealthFlow Frontend

React and TypeScript single-page application for the HealthFlow learning project.

## Stack

- React with Vite
- TypeScript
- React Router
- TanStack Query
- React Hook Form and Zod
- Tailwind CSS

## Local development

Start the backend Gateway on port `8084`, then run:

```bash
npm install
npm run dev
```

The frontend runs at `http://localhost:3000`. Copy `.env.example` to
`.env.local` only when the Gateway URL needs to be changed.

The public landing page is available at `/`, account creation at `/signup`,
sign-in at `/login`, and the authenticated application begins at `/dashboard`.

Development accounts are seeded by the Auth Service `dev` profile. Their
password is `admin123`:

- `admin@pm.com`
- `receptionist@pm.com`
- `doctor@pm.com`

## Checks

```bash
npm run lint
npm run build
```

## Docker

From the repository root:

```bash
docker compose up --build
```

The Nginx container serves the SPA at `http://localhost:3000` and supports
React Router fallback navigation.
