import { http } from './http.js';

export const theaterApi = {
  list: (params = {}) => http.get('/api/theaters', { params }).then((res) => res.data),
  get: (id) => http.get(`/api/theaters/${id}`).then((res) => res.data),
  create: (payload) => http.post('/api/theaters', payload).then((res) => res.data),
  update: (id, payload) => http.put(`/api/theaters/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/api/theaters/${id}`).then((res) => res.data),
  screens: (theaterId) => http.get(`/api/theaters/${theaterId}/screens`).then((res) => res.data),
  createScreen: (payload) => http.post('/api/theaters/screens', payload).then((res) => res.data),
  updateScreen: (id, payload) => http.put(`/api/theaters/screens/${id}`, payload).then((res) => res.data),
  removeScreen: (id) => http.delete(`/api/theaters/screens/${id}`).then((res) => res.data),
  seats: (screenId) => http.get(`/api/theaters/screens/${screenId}/seats`).then((res) => res.data),
  createSeat: (payload) => http.post('/api/theaters/seats', payload).then((res) => res.data),
  updateSeat: (id, payload) => http.put(`/api/theaters/seats/${id}`, payload).then((res) => res.data),
  removeSeat: (id) => http.delete(`/api/theaters/seats/${id}`).then((res) => res.data),
};
