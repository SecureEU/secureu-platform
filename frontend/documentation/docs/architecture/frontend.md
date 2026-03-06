---
sidebar_position: 3
---

# Frontend Architecture

The SECUR-EU frontend is built with Next.js 15 and React 19, providing a modern, responsive user interface.

## Project Structure

```
secur-eu-dashboard/
├── app/
│   ├── layout.js          # Root layout
│   ├── page.js            # Dashboard page
│   ├── globals.css        # Global styles
│   ├── scans/
│   │   └── page.js        # Scans page
│   ├── hosts/
│   │   └── page.js        # Assets page
│   ├── exploitation/
│   │   └── page.js        # Exploitation page
│   ├── assistant/
│   │   └── page.js        # AI assistant page
│   └── compliance/
│       └── page.js        # Compliance page
├── components/
│   ├── Layout.js          # Main layout component
│   ├── dashboard/
│   │   └── Overview.js    # Dashboard components
│   ├── scans/
│   │   └── Scans.js       # Scan components
│   ├── exploitation/
│   │   └── ...            # Exploitation components
│   └── ui/
│       ├── Button.js      # UI components
│       ├── Card.js
│       ├── Badge.js
│       └── ...
├── lib/
│   ├── designTokens.js    # Design system tokens
│   └── api.js             # API client
├── public/
│   └── ...                # Static assets
└── package.json
```

## Core Concepts

### App Router

Next.js 15 App Router structure:

```jsx
// app/layout.js
import Layout from '@/components/Layout';
import './globals.css';

export const metadata = {
  title: 'SECUR-EU',
  description: 'Security Operations Platform',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <Layout>{children}</Layout>
      </body>
    </html>
  );
}
```

### Client Components

Interactive components use the `'use client'` directive:

```jsx
'use client';

import { useState, useEffect } from 'react';

export default function DashboardOverview() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const response = await fetch('http://localhost:3001/overview');
      const data = await response.json();
      setData(data);
    } catch (error) {
      console.error('Error fetching data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <Loading />;

  return (
    <div>
      {/* Dashboard content */}
    </div>
  );
}
```

## Component Architecture

### Component Hierarchy

```
Layout
├── Header
│   ├── Logo
│   ├── Navigation
│   └── UserMenu
├── Main Content
│   └── Page Components
└── Footer
```

### Reusable UI Components

```jsx
// components/ui/Button.js
'use client';

const Button = ({
  children,
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  onClick,
  ...props
}) => {
  const variants = {
    primary: 'btn-primary',
    secondary: 'btn-secondary',
    danger: 'btn-danger',
    ghost: 'btn-ghost',
  };

  const sizes = {
    sm: 'btn-sm',
    md: 'btn-md',
    lg: 'btn-lg',
  };

  return (
    <button
      className={`btn ${variants[variant]} ${sizes[size]}`}
      disabled={disabled || loading}
      onClick={onClick}
      {...props}
    >
      {loading ? <Spinner /> : children}
    </button>
  );
};
```

### Page Components

```jsx
// app/scans/page.js
import Scans from '@/components/scans/Scans';

export const metadata = {
  title: 'Scans | SECUR-EU',
};

export default function ScansPage() {
  return <Scans />;
}
```

## State Management

### Local State

Component-level state with React hooks:

```jsx
const [scans, setScans] = useState([]);
const [filter, setFilter] = useState('all');
const [loading, setLoading] = useState(true);
```

### Data Fetching

```jsx
// Fetch with error handling
const fetchScans = async () => {
  setLoading(true);
  try {
    const response = await fetch('http://localhost:3001/scans');
    const data = await response.json();
    setScans(data || []);
  } catch (error) {
    console.error('Error:', error);
    setScans([]);
  } finally {
    setLoading(false);
  }
};

// Polling for updates
useEffect(() => {
  fetchScans();
  const interval = setInterval(fetchScans, 30000);
  return () => clearInterval(interval);
}, []);
```

### Null Safety

Handle null/undefined API responses:

```jsx
// Safe data access
setScanData(data && data.length > 0 ? data[0] : null);
setScans(data || []);
const filtered = (data || []).filter(item => item.status === 'active');
```

## Styling System

### Design Tokens

```javascript
// lib/designTokens.js
export const colors = {
  primary: {
    start: '#2563eb',
    end: '#dc2626',
    gradient: 'linear-gradient(135deg, #2563eb 0%, #dc2626 100%)',
  },
  severity: {
    critical: '#7c2d12',
    high: '#ef4444',
    medium: '#f59e0b',
    low: '#3b82f6',
  },
};

export const spacing = {
  xs: '0.25rem',
  sm: '0.5rem',
  md: '1rem',
  lg: '1.5rem',
  xl: '2rem',
};
```

### Tailwind CSS

Utility-first styling:

```jsx
<div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
  <h2 className="text-xl font-bold text-gray-900 mb-4">
    Scan Results
  </h2>
  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
    {/* Grid items */}
  </div>
</div>
```

### Custom CSS Classes

```css
/* globals.css */
.card {
  @apply bg-white rounded-2xl border border-gray-200 shadow-sm;
}

.btn-primary {
  @apply bg-gradient-to-r from-blue-600 to-red-600 text-white
         font-medium rounded-lg px-4 py-2
         hover:shadow-lg transition-all duration-200;
}

.severity-critical {
  @apply bg-orange-900 text-white;
}
```

## API Integration

### API Client

```javascript
// lib/api.js
const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3001';

export const api = {
  async get(endpoint) {
    const response = await fetch(`${API_BASE}${endpoint}`);
    if (!response.ok) throw new Error('API Error');
    return response.json();
  },

  async post(endpoint, data) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (!response.ok) throw new Error('API Error');
    return response.json();
  },

  async delete(endpoint) {
    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error('API Error');
    return response.json();
  },
};
```

### Usage in Components

```jsx
import { api } from '@/lib/api';

const startScan = async () => {
  try {
    const result = await api.post('/scan/nmap', {
      target: targetInput,
      scanType: 'standard',
    });
    console.log('Scan started:', result.scanId);
  } catch (error) {
    console.error('Failed to start scan:', error);
  }
};
```

## Responsive Design

### Breakpoints

```jsx
<div className="
  grid
  grid-cols-1
  sm:grid-cols-2
  lg:grid-cols-4
  gap-4
">
  {/* Responsive grid */}
</div>
```

### Mobile Navigation

```jsx
const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

return (
  <>
    {/* Desktop nav - hidden on mobile */}
    <nav className="hidden lg:flex">
      {/* Navigation items */}
    </nav>

    {/* Mobile menu button */}
    <button
      className="lg:hidden"
      onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
    >
      {mobileMenuOpen ? <X /> : <Menu />}
    </button>

    {/* Mobile nav - shown when open */}
    {mobileMenuOpen && (
      <nav className="lg:hidden">
        {/* Mobile navigation items */}
      </nav>
    )}
  </>
);
```

## Performance

### Code Splitting

Next.js automatic code splitting by route.

### Image Optimization

```jsx
import Image from 'next/image';

<Image
  src="/logo.png"
  alt="Logo"
  width={40}
  height={40}
  priority
/>
```

### Memoization

```jsx
import { useMemo, useCallback } from 'react';

const filteredScans = useMemo(() => {
  return scans.filter(scan => scan.status === filter);
}, [scans, filter]);

const handleRefresh = useCallback(() => {
  fetchScans();
}, []);
```

## Related

- [Architecture Overview](/architecture/overview)
- [Backend Architecture](/architecture/backend)
- [UI Components](/api/overview)
