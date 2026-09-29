---
name: frontend-conventions
description: Conventions for the React frontend. Use when creating or editing any file under frontend/src.
---

# Frontend Conventions

## Stack

Plain JavaScript (.jsx), never TypeScript. React + Vite + Tailwind +
shadcn/ui + react-hook-form.

## Auth — non-negotiable

The backend uses httpOnly cookies. The frontend stores no token.

- No localStorage or sessionStorage for auth, ever.
- No Authorization header.
- All API calls go through src/services/\*.js. Never import axios in a
  component.
- Auth state comes from useCurrentUser(), never from local state.

## Responsive

Mobile-first, works from 320px up. Base classes target small screens;
sm: md: lg: scale upward. No horizontal scroll at any width. Tap targets
44px minimum. Mobile inputs at 16px font-size or larger to prevent iOS
Safari zoom.

## Structure

- Reusable UI in src/components/common/ — check there before creating a
  new button, input, card or badge.
- Pages in src/pages/<area>/.
- Tailwind utilities only. No CSS modules, no inline style objects.

## UX baseline for every screen

- Loading states use skeletons, not spinners, for content areas.
- Mutations disable their trigger while in flight.
- Errors show the backend message via src/utils/apiError.js.
- Empty states are designed, not blank.
- Labels tied to inputs; visible focus states everywhere.
