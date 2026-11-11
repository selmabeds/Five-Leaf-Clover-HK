import { useRef, useState } from 'react';

export default function DeliveryConsole({ user, addLog, getCsrfToken }) {
  const [data, setData] = useState(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);

  const isCourier = user.role === 'COURIER';

  async function request(path, label, method = 'POST') {
    if (inFlight.current) return;

    inFlight.current = true;
    setPending(true);
    setError('');
    addLog(label + ' requested');

    try {
      const headers = method === 'POST'
        ? { 'X-CSRF-TOKEN': await getCsrfToken() }
        : {};

      const response = await fetch('/api/delivery' + path, {
        method,
        headers,
        credentials: 'include'
      });

      if (response.status === 401) {
        throw new Error('Your session expired. Please log in again.');
      }

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.error || 'Delivery request failed');
      }

      setData(result);
      addLog(label + ' succeeded');
    } catch (failure) {
      setError(failure.message);
      addLog(label + ' failed: ' + failure.message);
    } finally {
      inFlight.current = false;
      setPending(false);
    }
  }

  const state = data?.courier?.state;
  const assignment = data?.assignment;
  const orderPath = '/orders/' + assignment?.orderId;

  const hasOffer =
    state === 'IDLE' && assignment?.status === 'OFFERED';

  const canPickUp =
    state === 'ASSIGNED'
    && assignment?.status === 'ACCEPTED'
    && data?.orderStatus === 'READY';

  const canDeliver =
    state === 'CARRYING'
    && assignment?.status === 'ACCEPTED'
    && data?.orderStatus === 'PICKED_UP';

  return (
    <section aria-busy={pending}>
      <h3>{isCourier ? 'Courier Delivery' : 'Dispatch Queue'}</h3>

      <button
        type="button"
        disabled={pending}
        onClick={() => request(
          isCourier ? '/me' : '/dispatch',
          'Refresh delivery',
          'GET'
        )}
      >
        Refresh Delivery
      </button>

      {!data && <p>Click Refresh Delivery to load the current state.</p>}

      {isCourier && data && (
        <>
          <p>Courier #{data.courier.id} — {state}</p>

          {assignment ? (
            <p>
              Order #{assignment.orderId}
              {' — '}{assignment.status}
              {' — '}{data.orderStatus}
            </p>
          ) : (
            <p>No current assignment.</p>
          )}

          {assignment?.status === 'STALLED' && (
            <p>An operator must release your stalled order.</p>
          )}

          <button
            type="button"
            disabled={
              pending
              || state !== 'OFFLINE'
              || assignment?.status === 'STALLED'
            }
            onClick={() => request('/on-duty', 'Go on duty')}
          >
            Go On Duty
          </button>

          <button
            type="button"
            disabled={pending || state === 'OFFLINE' || state === 'CARRYING'}
            onClick={() => request('/off-duty', 'Go off duty')}
          >
            Go Off Duty
          </button>

          {hasOffer && (
            <>
              <button
                type="button"
                disabled={pending}
                onClick={() => request(orderPath + '/accept', 'Accept offer')}
              >
                Accept Offer
              </button>

              <button
                type="button"
                disabled={pending}
                onClick={() => request(
                  orderPath + '/decline',
                  'Decline offer and go off duty'
                )}
              >
                Decline and Go Off Duty
              </button>
            </>
          )}

          <button
            type="button"
            disabled={pending || !canPickUp}
            onClick={() => request(orderPath + '/pickup', 'Confirm pickup')}
          >
            Confirm Pickup
          </button>

          <button
            type="button"
            disabled={pending || !canDeliver}
            onClick={() => request(orderPath + '/deliver', 'Confirm delivery')}
          >
            Confirm Delivery
          </button>
        </>
      )}

      {!isCourier && data && (
        <>
          <h4>Waiting Orders</h4>

          <p>{data.queuedOrderIds.join(', ') || 'None'}</p>

          <h4>Stalled Orders</h4>

          {data.stalledAssignments.length === 0 && <p>None</p>}

          {data.stalledAssignments.map(item => (
            <div key={item.orderId}>
              <p>
                Order #{item.orderId} — Courier #{item.courierId}
              </p>

              <button
                type="button"
                disabled={pending}
                onClick={() => request(
                  '/orders/' + item.orderId + '/reassign',
                  'Reassign order #' + item.orderId
                )}
              >
                Reassign
              </button>
            </div>
          ))}
        </>
      )}

      {pending && <p role="status">Request in progress…</p>}
      {error && <p role="alert">{error}</p>}
    </section>
  );
}