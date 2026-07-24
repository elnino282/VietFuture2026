import type { Config } from "tailwindcss";
import defaultTheme from "tailwindcss/defaultTheme";

const config: Config = {
  darkMode: ["class"],
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    // Override container mặc định để giới hạn layout tránh bị dàn trải
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      // 1. TYPOGRAPHY: Font Display organic/serif cho heading và Geometric sans-serif cho body
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', ...defaultTheme.fontFamily.sans],
        display: ['"Fraunces"', ...defaultTheme.fontFamily.serif],
        heading: ['"Fraunces"', ...defaultTheme.fontFamily.serif],
      },
      
      // 2. COLORS: Thêm dải màu warm earthy neutrals và terracotta để tạo độ sâu
      colors: {
        background: "var(--background)",
        foreground: "var(--foreground)",
        
        // Earthy Neutrals - Tạo độ sâu tự nhiên, ấm áp hơn màu gray thông thường
        earth: {
          50: "#faf9f7",
          100: "#f3f0ea",
          200: "#e4ded4",
          300: "#cfc5b6",
          400: "#b5a691",
          500: "#a18d75",
          600: "#86725c",
          700: "#6a5a4a",
          800: "#574b3f", // Soil brown
          900: "#463d34",
          950: "#26201b",
        },
        
        // Terracotta - Điểm nhấn ấm áp (Warm accent) chuyển sang tông cam/vàng (Apricot/Gold)
        terracotta: {
          50: "#fff8f1",
          100: "#ffedd5",
          200: "#fed7aa",
          300: "#fdba74",
          400: "#fb923c",
          500: "#f97316", // Warm apricot orange
          600: "#ea580c",
          700: "#c2410c",
          800: "#9a3412",
          900: "#7c2d12",
        },
        
        // Semantic Colors
        card: "var(--card)",
        "card-foreground": "var(--card-foreground)",
        popover: "var(--popover)",
        "popover-foreground": "var(--popover-foreground)",
        primary: "var(--primary)", // Mặc định là Emerald trong index.css
        "primary-foreground": "var(--primary-foreground)",
        secondary: "var(--secondary)",
        "secondary-foreground": "var(--secondary-foreground)",
        muted: "var(--muted)",
        "muted-foreground": "var(--muted-foreground)",
        accent: "var(--accent)",
        "accent-foreground": "var(--accent-foreground)",
        destructive: "var(--destructive)",
        "destructive-foreground": "var(--destructive-foreground)",
        border: "var(--border)",
        input: "var(--input)",
        ring: "var(--ring)",
        success: "var(--success)",
        "success-foreground": "var(--success-foreground)",
        warning: "var(--warning)",
        "warning-foreground": "var(--warning-foreground)",
        info: "var(--info)",
        "info-foreground": "var(--info-foreground)",
        status: {
          success: { DEFAULT: "var(--status-success-bg)", foreground: "var(--status-success-fg)" },
          warning: { DEFAULT: "var(--status-warning-bg)", foreground: "var(--status-warning-fg)" },
          error: { DEFAULT: "var(--status-error-bg)", foreground: "var(--status-error-fg)" },
          neutral: { DEFAULT: "var(--status-neutral-bg)", foreground: "var(--status-neutral-fg)" }
        },
        chart: {
          1: "var(--chart-1)",
          2: "var(--chart-2)",
          3: "var(--chart-3)",
          4: "var(--chart-4)",
          5: "var(--chart-5)",
        },
        sidebar: {
          DEFAULT: "var(--sidebar)",
          foreground: "var(--sidebar-foreground)",
          primary: "var(--sidebar-primary)",
          "primary-foreground": "var(--sidebar-primary-foreground)",
          accent: "var(--sidebar-accent)",
          "accent-foreground": "var(--sidebar-accent-foreground)",
          border: "var(--sidebar-border)",
          ring: "var(--sidebar-ring)",
        },
      },
      
      // 3. LAYOUT & SPACING: Cấu hình giới hạn layout chuẩn Enterprise
      maxWidth: {
        "8xl": "88rem", // 1408px
        "7xl": "80rem", // 1280px
        "enterprise": "1400px", // Custom max-width cho layout không bị dàn trải
      },
      
      spacing: {
        18: "4.5rem",
        22: "5.5rem",
        30: "7.5rem",
      },
      
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
    },
  },
  plugins: [],
};

export default config;
