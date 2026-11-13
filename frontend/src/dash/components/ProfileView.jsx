import { useEffect, useState } from 'react';
import DietaryChecklist from './DietaryChecklist';
import { getProfile, saveDietaryRestrictions } from '../services/authService';

export default function ProfileView({ onSaved }) {
  const [profile, setProfile] = useState(null);
  const [selected, setSelected] = useState([]);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');

  useEffect(() => {
    let active = true;
    getProfile().then(data => {
      if (!active) return;
      setProfile(data);
      setSelected(data.dietaryRestrictions);
    }).catch(error => {
      if (active) setMessage(error.message);
    });
    return () => { active = false; };
  }, []);

  async function save(event) {
    event.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      const data = await saveDietaryRestrictions(selected);
      setSelected(data.dietaryRestrictions);
      onSaved();
      setMessage('Dietary preferences saved.');
    } catch (error) {
      setMessage(error.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <section>
      <h3>Profile</h3>
      {profile ? (
        <form onSubmit={save}>
          <p><strong>Name</strong> {profile.name}</p>
          <p><strong>Email</strong> {profile.email}</p>
          <DietaryChecklist selected={selected} onChange={setSelected} disabled={saving} />
          <button type="submit" disabled={saving}>{saving ? 'Saving…' : 'Save Preferences'}</button>
        </form>
      ) : !message && <p>Loading profile…</p>}
      {message && <p role="status">{message}</p>}
    </section>
  );
}
