async function request(url, options = {}) {
  const response = await fetch(url, { credentials: 'include', ...options });
  if (!response.ok) {
    if (response.status === 401) throw new Error('Your session expired. Please log in again.');
    const text = await response.text();
    let message = text || 'Request failed (' + response.status + ')';
    try { message = JSON.parse(text).error || message; } catch { message = text || message; }
    throw new Error(message);
  }
  return response.json();
}

export const loadHistory = () => request('/api/prc/history');
export const loadOrder = id => request('/api/prc/orders/' + encodeURIComponent(id));
export const loadReceipt = id => request('/api/prc/orders/' + encodeURIComponent(id) + '/receipt');
export const refundOrder = (id, csrfToken) => request('/api/prc/orders/' + encodeURIComponent(id) + '/refund', {
  method: 'POST', headers: { 'X-CSRF-TOKEN': csrfToken }
});
