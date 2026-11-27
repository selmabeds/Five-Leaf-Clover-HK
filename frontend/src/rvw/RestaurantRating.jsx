import { useEffect, useState } from 'react';
import { loadRestaurantRating } from './reviewApi.mjs';
import './reviews.css';

export default function RestaurantRating({ restaurantId, revision = 0 }) {
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setResult(null);
    setError('');
    loadRestaurantRating(restaurantId).then(data => {
      if (active) setResult(data);
    }).catch(problem => {
      if (active) setError(problem.message);
    });
    return () => { active = false; };
  }, [restaurantId, revision]);

  if (error) return <p role="status">Restaurant rating unavailable. {error}</p>;
  if (!result) return <p role="status">Loading restaurant rating…</p>;
  if (result.reviewCount === 0) {
    return (
      <p className="restaurant-rating">
        <span aria-hidden="true" className="empty-rating">☆☆☆☆☆</span>
        {' '}No ratings
      </p>
    );
  }

  return (
    <p className="restaurant-rating">
      <span aria-hidden="true">★ </span>
      Restaurant rating <strong>{Number(result.averageRating).toFixed(2)}/5</strong>
      {' · '}{result.reviewCount} {result.reviewCount === 1 ? 'review' : 'reviews'}
    </p>
  );
}
