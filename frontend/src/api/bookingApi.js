import { http } from './http.js';

export const bookingApi = {
  create: (payload) => http.post('/api/bookings', payload).then((res) => res.data),
  history: () => http.get('/api/bookings/history').then((res) => res.data),
  list: () => http.get('/api/bookings').then((res) => res.data),
  get: (id) => http.get(`/api/bookings/${id}`).then((res) => res.data),
  update: (id, payload) => http.put(`/api/bookings/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/api/bookings/${id}`).then((res) => res.data),
};
