import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import keycloak from './keycloak';

interface AuthState {
  token: string | undefined;
  username: string | undefined;
  subject: string | undefined;
  roles: string[];
  hasRole: (role: string) => boolean;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

// keycloak-js's own instance already guards against a real double-init, but React 18
// StrictMode invokes effects twice in dev *before* the first one's cleanup can run,
// which is enough to make init() throw "already initialized". A module-level flag
// (surviving across the two mounts) avoids that without disabling StrictMode.
let initStarted = false;

export function AuthProvider({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false);
  const [token, setToken] = useState<string | undefined>(undefined);

  useEffect(() => {
    if (initStarted) return;
    initStarted = true;

    keycloak
      .init({ onLoad: 'login-required', pkceMethod: 'S256' })
      .then(() => {
        setToken(keycloak.token);
        setReady(true);
      })
      .catch((err) => {
        console.error('Keycloak init failed', err);
      });

    keycloak.onTokenExpired = () => {
      keycloak.updateToken(30).then(() => setToken(keycloak.token));
    };
  }, []);

  if (!ready) {
    return (
      <div className="min-h-screen bg-slate-50 flex items-center justify-center text-slate-500">
        Signing in...
      </div>
    );
  }

  const roles = keycloak.realmAccess?.roles ?? [];
  const value: AuthState = {
    token,
    username: keycloak.tokenParsed?.preferred_username,
    subject: keycloak.subject,
    roles,
    hasRole: (role: string) => roles.includes(role),
    logout: () => keycloak.logout({ redirectUri: window.location.origin }),
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
