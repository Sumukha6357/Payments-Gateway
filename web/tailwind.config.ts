import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}"
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ["'IBM Plex Sans'", "ui-sans-serif", "system-ui"]
      },
      colors: {
        brand: {
          50: "#f0f6ff",
          100: "#d9e8ff",
          500: "#155dfc",
          700: "#123ea3"
        }
      }
    }
  },
  plugins: []
};

export default config;
