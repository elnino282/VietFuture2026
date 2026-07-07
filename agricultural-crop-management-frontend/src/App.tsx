import { lazy, Suspense } from 'react';
import { I18nProvider } from '@/providers/I18nProvider';
import { ThemeProvider } from '@/providers/ThemeProvider';
import { ErrorBoundary, Toaster } from '@/shared/ui';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { AppRoutes } from './app/routes';
import { AuthProvider } from './features/auth/context/AuthContext';
import { AccountLockedModal } from './shared/components/AccountLockedModal';
import { SearchWindow } from './shared/components';
import { PreferencesProvider } from './shared/contexts';

/**
 * Lazy-loaded so the chat Firebase SDK is not bundled into the main chunk.
 * No Suspense fallback needed — the button simply doesn't render
 * until the chunk is ready (null is returned during loading).
 */
const FloatingChatButton = lazy(() =>
  import('@/features/chat').then((m) => ({ default: m.FloatingChatButton }))
);

// Initialize i18n
import '@/i18n';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2, // Retry failed queries up to 2 times
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000), // Exponential backoff
      refetchOnWindowFocus: false,
      refetchOnMount: true, // Enable fetching on mount to load data properly
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 30 * 60 * 1000, // 30 minutes garbage collection
    },
    mutations: {
      retry: 0, // Don't retry mutations - let user retry manually
    },
  },
});

function GlobalSearchTrigger() {
  const [searchOpen, setSearchOpen] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setSearchOpen(true);
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  return (
    <SearchWindow
      open={searchOpen}
      onOpenChange={setSearchOpen}
      onSelect={(result) => {
        navigate(result.route);
      }}
    />
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <I18nProvider>
        <ThemeProvider>
          <ErrorBoundary>
            <AuthProvider>
              <PreferencesProvider>
                <BrowserRouter>
                  <AppRoutes />
                  <Toaster />
                  <AccountLockedModal />
                  <GlobalSearchTrigger />
                  {/* Global floating chat button — shown on all authenticated pages except /chat */}
                  <Suspense fallback={null}>
                    <FloatingChatButton />
                  </Suspense>
                </BrowserRouter>
              </PreferencesProvider>
            </AuthProvider>
          </ErrorBoundary>
        </ThemeProvider>
      </I18nProvider>
    </QueryClientProvider>
  );
}
