// Design Tokens for SECUR-EU Dashboard
// Centralized design system following llm-security-scanner patterns

export const colors = {
  // Primary gradient (blue theme for SECUR-EU)
  primary: {
    start: '#1d4ed8',
    end: '#3b82f6',
    gradient: 'linear-gradient(135deg, #1d4ed8 0%, #3b82f6 100%)',
  },

  // Brand colors - Blue focused
  brand: {
    primary: '#2563eb',
    primaryLight: '#3b82f6',
    primaryDark: '#1d4ed8',
    blue: '#2563eb',
    blueLight: '#3b82f6',
    blueDark: '#1d4ed8',
  },

  // Neutral colors
  neutral: {
    white: '#ffffff',
    gray50: '#f9fafb',
    gray100: '#f3f4f6',
    gray200: '#e5e7eb',
    gray300: '#d1d5db',
    gray400: '#9ca3af',
    gray500: '#6b7280',
    gray600: '#4b5563',
    gray700: '#374151',
    gray800: '#1f2937',
    gray900: '#111827',
    black: '#000000',
  },

  // Semantic colors
  status: {
    success: '#10b981',
    successLight: '#34d399',
    successDark: '#059669',
    warning: '#f59e0b',
    warningLight: '#fbbf24',
    warningDark: '#d97706',
    error: '#ef4444',
    errorLight: '#f87171',
    errorDark: '#dc2626',
    info: '#3b82f6',
    infoLight: '#60a5fa',
    infoDark: '#2563eb',
  },

  // Severity colors (for vulnerabilities)
  severity: {
    critical: '#7c2d12',
    high: '#ef4444',
    medium: '#f59e0b',
    low: '#3b82f6',
    info: '#6b7280',
  },

  // Background colors
  background: {
    primary: '#ffffff',
    secondary: '#f9fafb',
    tertiary: '#f3f4f6',
    dark: '#111827',
    darkSecondary: '#1f2937',
  },

  // Text colors
  text: {
    primary: '#111827',
    secondary: '#6b7280',
    tertiary: '#9ca3af',
    inverse: '#ffffff',
    accent: '#2563eb',
  },

  // Border colors
  border: {
    light: '#e5e7eb',
    medium: '#d1d5db',
    dark: '#374151',
  },
};

export const spacing = {
  0: '0',
  1: '0.25rem',   // 4px
  2: '0.5rem',    // 8px
  3: '0.75rem',   // 12px
  4: '1rem',      // 16px
  5: '1.25rem',   // 20px
  6: '1.5rem',    // 24px
  8: '2rem',      // 32px
  10: '2.5rem',   // 40px
  12: '3rem',     // 48px
  16: '4rem',     // 64px
  20: '5rem',     // 80px
  24: '6rem',     // 96px
};

export const typography = {
  fontFamily: {
    sans: '-apple-system, BlinkMacSystemFont, "Segoe UI", "Helvetica Neue", Arial, sans-serif',
    mono: 'ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace',
  },
  fontSize: {
    xs: '0.75rem',     // 12px
    sm: '0.875rem',    // 14px
    base: '1rem',      // 16px
    lg: '1.125rem',    // 18px
    xl: '1.25rem',     // 20px
    '2xl': '1.5rem',   // 24px
    '3xl': '1.875rem', // 30px
    '4xl': '2.25rem',  // 36px
    '5xl': '3rem',     // 48px
  },
  fontWeight: {
    normal: '400',
    medium: '500',
    semibold: '600',
    bold: '700',
  },
  lineHeight: {
    tight: '1.25',
    normal: '1.5',
    relaxed: '1.75',
  },
};

export const shadows = {
  none: 'none',
  sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
  base: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
  md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
  lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
  xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
  '2xl': '0 25px 50px -12px rgb(0 0 0 / 0.25)',
  inner: 'inset 0 2px 4px 0 rgb(0 0 0 / 0.05)',
};

export const borderRadius = {
  none: '0',
  sm: '0.125rem',   // 2px
  base: '0.25rem',  // 4px
  md: '0.375rem',   // 6px
  lg: '0.5rem',     // 8px
  xl: '0.75rem',    // 12px
  '2xl': '1rem',    // 16px
  '3xl': '1.5rem',  // 24px
  full: '9999px',
};

export const transitions = {
  fast: '150ms cubic-bezier(0.4, 0, 0.2, 1)',
  base: '200ms cubic-bezier(0.4, 0, 0.2, 1)',
  slow: '300ms cubic-bezier(0.4, 0, 0.2, 1)',
  slower: '500ms cubic-bezier(0.4, 0, 0.2, 1)',
};

export const zIndex = {
  hide: -1,
  auto: 'auto',
  base: 0,
  dropdown: 1000,
  sticky: 1020,
  fixed: 1030,
  modalBackdrop: 1040,
  modal: 1050,
  popover: 1060,
  tooltip: 1070,
};

// Utility function to get CSS variable reference
export const cssVar = (name) => `var(--${name})`;

// Export all tokens as a single object
const designTokens = {
  colors,
  spacing,
  typography,
  shadows,
  borderRadius,
  transitions,
  zIndex,
};

export default designTokens;
