
/**
 * DASH authentication service.
 *
 * Communicates with the REG backend.
 * REG owns authentication and session validation.
 */

// CSRF token of the current session, read from the login page (needed for every POST command)
export async function getCsrfToken() {
  const response = await fetch('/login', {
    credentials: 'include'
  });

  if (!response.ok) {
    throw new Error('Cannot load login page');
  }

  const html = await response.text();
  const doc = new DOMParser().parseFromString(html, 'text/html');
  const token = doc.querySelector('input[name="_csrf"]')?.value;

  if (!token) {
    throw new Error('CSRF token not found');
  }

  return token;
}

// UC-REG-02 Log In: returns true when REG accepted the credentials
export async function login(credentials) {
  const csrfToken = await getCsrfToken();

  const response = await fetch('/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    credentials: 'include',
    body: new URLSearchParams({
      username: credentials.email,
      password: credentials.password,
      _csrf: csrfToken
    })
  });

  return response.ok;
}

// UC-REG-03: identity and role of the authenticated session
// (served by /api/prc/context, which also gives a restaurant account its restaurantId)
export async function getCurrentUser() {
  const response = await fetch('/api/prc/context', { credentials: 'include' });
  if (!response.ok) throw new Error('Cannot load the authenticated user');
  return response.json();
}

// Ends the current session: returns true when REG logged the user out
export async function logout() {
  const csrfToken = await getCsrfToken();

  const response = await fetch('/logout', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded'
    },
    credentials: 'include',
    body: new URLSearchParams({
      _csrf: csrfToken
    })
  });

  return response.ok;
}

// UC-REG-01 Create an Account (customer only): returns the response so the caller can read errors
export async function register({ name, email, password, dietaryRestrictions }) {
  const csrfToken = await getCsrfToken();

  return fetch('/api/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': csrfToken,
    },
    credentials: 'include',
    body: JSON.stringify({ name, email, password, dietaryRestrictions }),
  });
}

export async function getProfile() {
  const response = await fetch('/api/profile', { credentials: 'include' });
  if (!response.ok) throw new Error('Could not load your profile.');
  return response.json();
}

export async function saveDietaryRestrictions(dietaryRestrictions) {
  const csrfToken = await getCsrfToken();
  const response = await fetch('/api/profile/dietary-restrictions', {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrfToken },
    body: JSON.stringify({ dietaryRestrictions }),
  });
  if (!response.ok) throw new Error('Could not save your dietary preferences.');
  return response.json();
}
