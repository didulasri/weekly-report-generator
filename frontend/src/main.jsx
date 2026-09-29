import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import './index.css'
import App from './App.jsx'
import { Toaster } from './components/ui/sonner.jsx'
import { queryClient } from './services/queryClient.js'
import { setQueryClient } from './services/api.js'
import { getCsrfToken } from './services/authApi.js'

// Wire the QueryClient into the axios interceptor so a failed token refresh can clear every
// cached query -- done here, once, rather than inside a component, since api.js is a plain module
// with no access to React context.
setQueryClient(queryClient)

// Spring Security's CSRF token is deferred: it isn't generated or written to the XSRF-TOKEN cookie
// until something reads it. Fetched once here, outside any component and before first render, so
// the cookie exists before the user can trigger any state-changing request. Fire-and-forget --
// a network hiccup here shouldn't block the app from rendering; the first real state-changing
// request will just fail with a clear error instead.
getCsrfToken().catch(() => {})

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
        <Toaster position="top-right" />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
