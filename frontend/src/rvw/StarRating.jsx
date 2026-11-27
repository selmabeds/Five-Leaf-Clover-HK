import { useId } from 'react';

export default function StarRating({ label, value, onChange, disabled }) {
  const group = useId();
  return (
    <fieldset className="review-score" disabled={disabled}>
      <legend>{label}</legend>
      <div className="review-stars">
        {[1, 2, 3, 4, 5].map(score => (
          <label key={score} title={score + ' out of 5'}>
            <input
              type="radio"
              name={group}
              value={score}
              checked={Number(value) === score}
              onChange={() => onChange(String(score))}
              aria-label={score + ' out of 5'}
              required
            />
            <span aria-hidden="true" className={score <= Number(value) ? 'selected' : ''}>★</span>
          </label>
        ))}
      </div>
      <span className="review-score-value">{value ? value + ' out of 5' : 'Choose a rating'}</span>
    </fieldset>
  );
}
