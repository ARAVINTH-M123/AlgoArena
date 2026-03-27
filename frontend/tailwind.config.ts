import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  theme: {
    extend: {
      colors: {
        platform: {
          bg: "#000000",
          panel: "#0a0a0a",
          border: "#16320f",
          accent: "#22c55e",
          accentSoft: "#14532d",
          text: "#f3f4f6"
        }
      },
      boxShadow: {
        glow: "0 0 18px rgba(34, 197, 94, 0.28)"
      }
    }
  },
  plugins: []
};

export default config;
