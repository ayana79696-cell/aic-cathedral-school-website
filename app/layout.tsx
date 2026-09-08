import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'AIC Cathedral Comprehensive School | Gilgil',
  description: 'AIC Cathedral Comprehensive School, Gilgil, Nakuru County, Kenya — nurturing excellence, character and faith.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><head>
    <link rel="preconnect" href="https://res.cloudinary.com" />
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
  </head><body>{children}</body></html>;
}
