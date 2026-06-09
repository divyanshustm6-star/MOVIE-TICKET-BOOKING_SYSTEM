import { http } from './http.js';

export const movieApi = {
  list: () => http.get('/movies').then((res) => res.data),
  publicList: () => http.get('/api/movies/public').then((res) => res.data),
  get: (id) => http.get(`/movies/${id}`).then((res) => res.data),
  create: (payload) => http.post('/movies', payload).then((res) => res.data),
  update: (id, payload) => http.put(`/movies/${id}`, payload).then((res) => res.data),
  remove: (id) => http.delete(`/movies/${id}`).then((res) => res.data),
};
