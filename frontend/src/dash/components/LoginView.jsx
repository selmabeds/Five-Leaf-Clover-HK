
import DietaryChecklist from './DietaryChecklist';

/**
 * DASH UI layer: CARE Eats Login.
 *
 * Login interface for authenticated users (UC-REG-02 Log In), and the
 * customer sign-up form (UC-REG-01 Create an Account). There is no guest
 * access: nothing else is visible before login (R-REG-02).
 */
export default function LoginView({
  signingUp,
  name,
  email,
  password,
  registrationPending,
  dietaryRestrictions,
  onDietaryRestrictionsChange,
  message,
  onNameChange,
  onEmailChange,
  onPasswordChange,
  onSubmit,
  onToggleMode
}) {
  return (
    <div className="login-container">
      <form onSubmit={onSubmit} className="login-form">
        <h1>CARE Eats</h1>
        <h2>{signingUp ? 'Create a customer account' : 'Login'}</h2>

        {signingUp && (
          <input
            type="text"
            aria-label="Name"
            placeholder="Name"
            autoComplete="name"
            value={name}
            onChange={event => onNameChange(event.target.value)}
            disabled={registrationPending}
            required
          />
        )}

        <input
          type="email"
          aria-label="Email"
          placeholder="Email"
          autoComplete="username"
          value={email}
          onChange={event => onEmailChange(event.target.value)}
          disabled={registrationPending}
          required
        />

        <input
          type="password"
          aria-label="Password"
          placeholder="Password"
          autoComplete={signingUp ? 'new-password' : 'current-password'}
          value={password}
          onChange={event => onPasswordChange(event.target.value)}
          disabled={registrationPending}
          required
        />

        {signingUp && (
          <details>
            <summary>Dietary preferences (optional)</summary>
            <DietaryChecklist selected={dietaryRestrictions}
              onChange={onDietaryRestrictionsChange} disabled={registrationPending} />
          </details>
        )}

        <button type="submit" disabled={registrationPending}>
          {registrationPending ? 'Creating account…' : signingUp ? 'Sign Up' : 'Log In'}
        </button>
        <button
          type="button"
          onClick={onToggleMode}
          disabled={registrationPending}
        >
          {signingUp ? 'Back to Login' : 'Create a Customer Account'}
        </button>

        <p role="status">{message}</p>
      </form>
    </div>
  );
}
