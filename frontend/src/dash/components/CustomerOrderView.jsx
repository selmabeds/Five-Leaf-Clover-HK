
import { useEffect, useState } from 'react';
import ReviewForm from '../../rvw/ReviewForm';
import { loadMyReviews } from '../../rvw/reviewApi.mjs';

/**
 * DASH UI layer: My Orders (customer).
 *
 * The logged-in customer follows the progress of their own orders
 * (UC-ORD-07 View Order Status) and may cancel an order while it is
 * still PLACED (UC-ORD-03 Cancel Order, R-DASH-01). After that,
 * cancellation is an operator action, so no Cancel button is shown.
 * After delivery the customer can view the order summary with its computed
 * cost (R-PRC-02, UC-PRC-02, sequence diagram displayOrderSummary).
 */
export default function CustomerOrderView({
  orders, loading, busy, onRefresh, onCancel, onViewSummary, onReviewSubmitted, addLog,
}) {
  const [reviews, setReviews] = useState([]);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewError, setReviewError] = useState('');

  useEffect(() => {
    let active = true;
    setReviewsLoading(true);
    setReviewError('');
    loadMyReviews().then(data => {
      if (active) setReviews(data);
    }).catch(error => {
      if (active) setReviewError(error.message);
    }).finally(() => {
      if (active) setReviewsLoading(false);
    });
    return () => { active = false; };
  }, [orders]);

  function reviewSubmitted(review) {
    setReviews(previous => [...previous.filter(item => item.orderId !== review.orderId), review]);
    onReviewSubmitted();
  }

  return (
    <section aria-busy={loading || busy}>
      <h3>My Orders</h3>

      <button type="button" disabled={loading || busy} onClick={onRefresh}>
        {loading ? 'Loading orders...' : 'Refresh Orders'}
      </button>

      {!loading && orders.length === 0 && (
        <p className="orders-empty">You have no orders yet.</p>
      )}
      {reviewError && <p role="alert">Could not load your ratings. Refresh Orders to retry. {reviewError}</p>}

      <div className="order-list">
        {orders.map(order => (
          <div className="order-card" key={order.orderId}>
            <div className="order-card-header">
              <span className="order-number">Order #{order.orderId}</span>
              <span className="order-status">{order.status}</span>
            </div>

            <div className="order-actions">
              {order.status === 'PLACED' && (
                <button type="button" disabled={busy} onClick={() => onCancel(order.orderId)}>
                  Cancel Order
                </button>
              )}

              {/* After delivery (a refunded order was delivered first) */}
              {(order.status === 'DELIVERED' || order.status === 'REFUNDED') && (
                <button type="button" disabled={busy} onClick={() => onViewSummary(order.orderId)}>
                  View Summary
                </button>
              )}
            </div>
            {(order.status === 'DELIVERED' || order.status === 'REFUNDED') && (
              <ReviewForm
                orderId={order.orderId}
                review={reviews.find(review => review.orderId === order.orderId)}
                disabled={busy || reviewsLoading || Boolean(reviewError)}
                onSubmitted={reviewSubmitted}
                addLog={addLog}
              />
            )}
          </div>
        ))}
      </div>
    </section>
  );
}
