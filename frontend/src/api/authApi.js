import { http } from './http.js';

export const authApi = {
  login: (payload) => http.post('/auth/login', payload).then((res) => res.data),
  register: (payload) => http.post('/auth/register', payload).then((res) => res.data),
  me: () => http.get('/auth/me').then((res) => res.data),
};
