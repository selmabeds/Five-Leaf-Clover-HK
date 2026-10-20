
import { useState } from 'react';
import './App.css';

function App() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState('');
  const [loggedIn, setLoggedIn] = useState(false);
  const [logs, setLogs] = useState([]);

  function addLog(message) {
    setLogs(previous => [...previous, message]);
  }

  async function getCsrfToken() {
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

  async function handleLogin(event) {
    event.preventDefault();
    setMessage('');

    try {
      const csrfToken = await getCsrfToken();

      const response = await fetch('/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded'
        },
        credentials: 'include',
        body: new URLSearchParams({
          username: email,
          password: password,
          _csrf: csrfToken
        })
      });

      if (response.ok) {
        setLoggedIn(true);
        addLog('Login successful: ' + email);
        setMessage('Login successful');
      } else {
        addLog('Login failed: ' + email);
        setMessage('Invalid email or password');
      }
    } catch (error) {
      setMessage('Login request failed');
      console.error(error);
    }
  }

  async function handleLogout() {
    try {
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

      if (response.ok) {
        setLoggedIn(false);
        addLog('Logout successful: ' + email);
        setPassword('');
        setMessage('Logged out successfully');
      } else {
        setMessage('Logout failed');
      }
    } catch (error) {
      setMessage('Logout request failed');
      console.error(error);
    }
  }

  if (loggedIn) {
    return (
      <div className="login-container">
        <div className="login-form">
          <h1>CARE Eats</h1>
          <h2>Dashboard</h2>
          <p>Logged in as {email}</p>
          <p>Authentication successful.</p>
          <button onClick={handleLogout}>Log Out</button>
          <h3>Activity Log</h3>
          <div className="activity-log">
            {logs.map((log, index) => (
              <p key={index}>{log}</p>
            ))}
          </div>
          <p role="status">{message}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="login-container">
      <form onSubmit={handleLogin} className="login-form">
        <h1>CARE Eats</h1>
        <h2>Login</h2>

        <input
          type="email"
          placeholder="Email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          required
        />

        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
        />

        <button type="submit">Log In</button>
        <p role="status">{message}</p>
      </form>
    </div>
  );
}

export default App;
