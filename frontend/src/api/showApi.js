import { http } from './http.js';

export const showApi = {
  list: (params = {}) => http.get('/api/shows', { params }).then((res) => res.data),
  get: (id) => http.get(`/api/shows/${id}`).then((res) => res.data),
  seats: (id) => http.get(`/api/shows/${id}/seats`).then((res) => res.data),
  create: (payload) => http.post('/api/shows', payload).then((res) => res.data),
  update: (id, payload) => http.put(`/api/shows/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/api/shows/${id}`).then((res) => res.data),
};
