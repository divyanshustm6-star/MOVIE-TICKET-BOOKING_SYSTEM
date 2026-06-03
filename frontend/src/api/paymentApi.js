import { http } from './http.js';

export const paymentApi = {
  create: (payload) => http.post('/api/payments', payload).then((res) => res.data),
  list: () => http.get('/api/payments').then((res) => res.data),
  get: (id) => http.get(`/api/payments/${id}`).then((res) => res.data),
  byBooking: (bookingId) => http.get(`/api/payments/booking/${bookingId}`).then((res) => res.data),
  update: (id, payload) => http.put(`/api/payments/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/api/payments/${id}`).then((res) => res.data),
};
