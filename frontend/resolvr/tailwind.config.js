/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}"
  ],
  theme: {
    extend: {}
  },
  plugins: [require("daisyui")],
  daisyui: {
    themes: [
      {
        resolvr: {
          "primary":         "#1d4ed8",   // Blue 700
          "primary-content": "#ffffff",
          "secondary":       "#0f766e",   // Teal 700
          "secondary-content": "#ffffff",
          "accent":          "#d97706",   // Amber 600
          "accent-content":  "#ffffff",
          "neutral":         "#1e293b",   // Slate 800
          "neutral-content": "#f1f5f9",
          "base-100":        "#f8fafc",   // Slate 50
          "base-200":        "#f1f5f9",   // Slate 100
          "base-300":        "#e2e8f0",   // Slate 200
          "base-content":    "#0f172a",   // Slate 900
          "info":            "#0ea5e9",
          "success":         "#16a34a",
          "warning":         "#d97706",
          "error":           "#dc2626",
        }
      },
      "light",
      "dark"
    ],
    defaultTheme: "resolvr",
    logs: false
  }
}
