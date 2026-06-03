import { useState } from 'react';
import AnimatedPage from '../../components/AnimatedPage.jsx';
import Button from '../../components/Button.jsx';
import LoadingSkeleton from '../../components/LoadingSkeleton.jsx';
import PageHeader from '../../components/PageHeader.jsx';
import { userApi } from '../../api/userApi.js';
import { useAsync } from '../../hooks/useAsync.js';
import { useToastStore } from '../../store/toastStore.js';

const emptyUser = { fullName: '', email: '', phone: '', password: '', roles: 'ROLE_USER' };

export default function AdminUsersPage() {
  const toast = useToastStore((state) => state.push);
  const { data: users = [], loading, reload } = useAsync(() => userApi.list(), []);
  const [form, setForm] = useState(emptyUser);
  const [editingId, setEditingId] = useState(null);

  function payload() {
    return { ...form, phone: form.phone || null, password: form.password || null, roles: form.roles.split(',').map((role) => role.trim()).filter(Boolean) };
  }

  function edit(user) {
    setEditingId(user.id);
    setForm({ fullName: user.fullName, email: user.email, phone: user.phone || '', password: '', roles: (user.roles || []).join(', ') });
  }

  async function save(event) {
    event.preventDefault();
    if (editingId) {
      await userApi.update(editingId, payload());
      toast({ type: 'success', message: 'User updated' });
    } else {
      await userApi.create(payload());
      toast({ type: 'success', message: 'User created' });
    }
    setForm(emptyUser);
    setEditingId(null);
    await reload();
  }

  return (
    <AnimatedPage className="grid gap-6">
      <PageHeader title="Manage users" eyebrow="Admin" description="Admin-only user creation and update through `/api/users`." />
      <form onSubmit={save} className="panel grid gap-4 p-5 md:grid-cols-2">
        <input className="field" placeholder="Full name" value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
        <input className="field" type="email" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
        <input className="field" placeholder="Phone" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        <input className="field" type="password" placeholder={editingId ? 'New password optional' : 'Password'} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required={!editingId} />
        <input className="field md:col-span-2" placeholder="Roles" value={form.roles} onChange={(e) => setForm({ ...form, roles: e.target.value })} />
        <div className="flex gap-3 md:col-span-2">
          <Button type="submit">{editingId ? 'Update user' : 'Create user'}</Button>
          {editingId && <Button type="button" variant="secondary" onClick={() => { setEditingId(null); setForm(emptyUser); }}>Cancel</Button>}
        </div>
      </form>
      {loading ? <LoadingSkeleton rows={5} /> : (
        <div className="grid gap-3">
          {users.map((user) => (
            <div key={user.id} className="panel flex flex-col gap-4 p-4 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="font-black text-white">{user.fullName}</p>
                <p className="text-sm text-slate-400">{user.email} · {(user.roles || []).join(', ')}</p>
              </div>
              <Button variant="secondary" onClick={() => edit(user)}>Edit</Button>
            </div>
          ))}
        </div>
      )}
    </AnimatedPage>
  );
}
