import { http } from './http.js';

export const userApi = {
  list: () => http.get('/api/users').then((res) => res.data),
  get: (id) => http.get(`/api/users/${id}`).then((res) => res.data),
  create: (payload) => http.post('/api/users', payload).then((res) => res.data),
  update: (id, payload) => http.put(`/api/users/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/api/users/${id}`).then((res) => res.data),
};
