import { http } from './http.js';

export const paymentApi = {
  create: (payload) => http.post('/api/payments', payload).then((res) => res.data),
  list: () => http.get('/api/payments').then((res) => res.data),
  get: (id) => http.get(`/api/payments/${id}`).then((res) => res.data),
  byBooking: (bookingId) => http.get(`/api/payments/booking/${bookingId}`).then((res) => res.data),
  update: (id, payload) => http.put(`/api/payments/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/api/payments/${id}`).then((res) => res.data),

  // Razorpay order creation
  createOrder: (payload) => http.post('/api/payments/create-order', payload).then((res) => res.data),
  // verify Razorpay payment
  verifyPayment: (payload) => http.post('/api/payments/verify', payload).then((res) => res.data),

  // download ticket for a booking
  downloadTicket: (bookingId) => http.get(`/api/bookings/${bookingId}/ticket`, { responseType: 'arraybuffer' }).then((res) => res.data),
};
