import { getCsrfToken } from '../dash/services/authService.js';

async function readResponse(response) {
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || 'Could not complete the review request.');
  return data;
}

export async function loadMyReviews() {
  return readResponse(await fetch('/api/reviews/mine', { credentials: 'include' }));
}

export async function loadRestaurantRating(restaurantId) {
  return readResponse(await fetch('/api/reviews/restaurants/' + restaurantId, { credentials: 'include' }));
}

export async function submitReview(orderId, restaurantRating, courierRating) {
  return readResponse(await fetch('/api/reviews/orders/' + orderId, {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': await getCsrfToken(),
    },
    body: JSON.stringify({ restaurantRating: Number(restaurantRating), courierRating: Number(courierRating) }),
  }));
}
