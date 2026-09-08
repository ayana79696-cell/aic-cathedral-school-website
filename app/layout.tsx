import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'AIC Cathedral Comprehensive School | Gilgil',
  description: 'AIC Cathedral Comprehensive School, Gilgil, Nakuru County, Kenya — nurturing excellence, character and faith.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return <html lang="en"><body>{children}</body></html>;
}
