import axios from 'axios';
import { API_BASE_URL, STORAGE_KEYS } from '../utils/constants.js';
import { useAuthStore } from '../store/authStore.js';
import { useToastStore } from '../store/toastStore.js';

export const http = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Debug: show effective base URL in dev tools
if (typeof window !== 'undefined') {
  // eslint-disable-next-line no-console
  console.debug('[http] API_BASE_URL =', API_BASE_URL);
}

http.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token || localStorage.getItem(STORAGE_KEYS.token);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const message = error.response?.data?.message || error.message || 'Request failed';
    if (status === 401) {
      useAuthStore.getState().logout(false);
    }
    if (status !== 401) {
      useToastStore.getState().push({ type: 'error', message });
    }
    return Promise.reject(error);
  },
);

export function apiMessage(error, fallback = 'Something went wrong') {
  return error?.response?.data?.message || error?.message || fallback;
}
