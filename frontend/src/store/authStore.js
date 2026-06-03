import { create } from 'zustand';
import { authApi } from '../api/authApi.js';
import { roles, STORAGE_KEYS } from '../utils/constants.js';

function readUser() {
  try {
    const stored = localStorage.getItem(STORAGE_KEYS.user);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

function normalizeUser(authResponse) {
  return {
    id: authResponse.userId ?? authResponse.id,
    fullName: authResponse.fullName,
    email: authResponse.email,
    phone: authResponse.phone,
    roles: authResponse.roles || [],
    tokenType: authResponse.tokenType || 'Bearer',
    expiresInSeconds: authResponse.expiresInSeconds,
  };
}

export const useAuthStore = create((set, get) => ({
  token: localStorage.getItem(STORAGE_KEYS.token),
  user: readUser(),
  bootstrapped: false,
  loading: false,

  isAuthenticated: () => Boolean(get().token),
  hasRole: (role) => get().user?.roles?.includes(role),
  isAdmin: () => get().user?.roles?.includes(roles.admin),
  isUser: () => get().user?.roles?.includes(roles.user),

  persistSession(authResponse) {
    const user = normalizeUser(authResponse);
    localStorage.setItem(STORAGE_KEYS.token, authResponse.token);
    localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
    set({ token: authResponse.token, user });
    return user;
  },

  async login(payload) {
    set({ loading: true });
    try {
      const authResponse = await authApi.login(payload);
      const user = get().persistSession(authResponse);
      // Fetch authoritative profile immediately and merge into state/localStorage
      try {
        const me = await authApi.me();
        const current = readUser();
        const merged = { ...current, ...me };
        localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(merged));
        set({ user: merged });
        return merged;
      } catch (e) {
        // If /me fails, still return the persisted user from token
        return user;
      }
    } finally {
      set({ loading: false, bootstrapped: true });
    }
  },

  async register(payload) {
    set({ loading: true });
    try {
      const authResponse = await authApi.register(payload);
      const user = get().persistSession(authResponse);
      // After register, fetch profile to ensure all fields are present
      try {
        const me = await authApi.me();
        const current = readUser();
        const merged = { ...current, ...me };
        localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(merged));
        set({ user: merged });
        return merged;
      } catch (e) {
        return user;
      }
    } finally {
      set({ loading: false, bootstrapped: true });
    }
  },

  async bootstrap() {
    const token = localStorage.getItem(STORAGE_KEYS.token);
    if (!token) {
      set({ bootstrapped: true, user: null, token: null });
      return;
    }
    set({ loading: true });
    try {
      const me = await authApi.me();
      const current = readUser();
      const user = { ...current, ...me };
      localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
      set({ user, token, bootstrapped: true });
    } catch {
      get().logout(false);
      set({ bootstrapped: true });
    } finally {
      set({ loading: false });
    }
  },

  logout(redirect = true) {
    localStorage.removeItem(STORAGE_KEYS.token);
    localStorage.removeItem(STORAGE_KEYS.user);
    set({ token: null, user: null });
    if (redirect && window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
  },
}));
