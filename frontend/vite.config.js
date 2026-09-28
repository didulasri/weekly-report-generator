import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Same-origin in dev so the backend's httpOnly cookies (and the CSRF double-submit cookie)
      // work without any CORS configuration -- the browser sees every request as going to
      // localhost:5173, and Vite forwards it to the real backend server-side.
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
