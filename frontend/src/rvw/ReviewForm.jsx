import { useRef, useState } from 'react';
import { submitReview } from './reviewApi.mjs';
import StarRating from './StarRating';
import './reviews.css';

export default function ReviewForm({ orderId, review, disabled, onSubmitted, addLog }) {
  const [restaurantRating, setRestaurantRating] = useState('');
  const [courierRating, setCourierRating] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);
  const inFlight = useRef(false);

  async function submit(event) {
    event.preventDefault();
    if (inFlight.current) return;
    inFlight.current = true;
    setSaving(true);
    setMessage('');
    addLog('Rate Order #' + orderId + ' requested');
    try {
      const saved = await submitReview(orderId, restaurantRating, courierRating);
      onSubmitted(saved);
      addLog('Rating saved for Order #' + orderId);
      setMessage('Rating saved.');
    } catch (error) {
      setMessage(error.message);
      addLog('Rate Order #' + orderId + ' failed: ' + error.message);
    } finally {
      inFlight.current = false;
      setSaving(false);
    }
  }

  if (review) {
    return (
      <div className="review-result">
        <strong>Your rating</strong>
        <p>Restaurant <span aria-hidden="true">{'★'.repeat(review.restaurantRating)}</span> {review.restaurantRating}/5</p>
        <p>Courier <span aria-hidden="true">{'★'.repeat(review.courierRating)}</span> {review.courierRating}/5</p>
        <p>This order has already been rated</p>
      </div>
    );
  }

  return (
    <form className="review-form" onSubmit={submit} aria-label={'Rate Order #' + orderId}>
      <h4>How was your order?</h4>
      <StarRating
        label="How was the restaurant?"
        value={restaurantRating}
        onChange={setRestaurantRating}
        disabled={disabled || saving}
      />
      <StarRating
        label="How was your delivery?"
        value={courierRating}
        onChange={setCourierRating}
        disabled={disabled || saving}
      />
      <button type="submit" disabled={disabled || saving}>
        {saving ? 'Saving Rating…' : 'Submit Rating'}
      </button>
      {message && <p role="status">{message}</p>}
    </form>
  );
}
