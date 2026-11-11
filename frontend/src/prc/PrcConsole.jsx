import { useRef, useState } from 'react';
import { loadHistory, loadOrder, loadReceipt, refundOrder } from './prcApi.mjs';
import { formatHistory, formatReceipt } from './prcFormatters.mjs';
import './PrcConsole.css';

export default function PrcConsole({ user, logs, addLog, getCsrfToken }) {
  const [history, setHistory] = useState([]);
  const [selected, setSelected] = useState('');
  const [busy, setBusy] = useState(false);
  const inFlight = useRef(false);
  const financialAccess = user.role !== 'COURIER';
  const selectedOrder = history.find(item => String(item.orderId) === selected);

  async function run(label, work) {
    if (inFlight.current) return; // Since it is a normal habit to click repeatedly when something takes time to load, we decided to prevent repeated clicks from sending overlapping commands, especially for refunds.
    inFlight.current = true;
    setBusy(true);
    addLog(label + ' requested');
    try { await work(); }
    catch (error) { addLog(label + ' failed: ' + error.message); }
    finally { inFlight.current = false; setBusy(false); }
  }

  async function viewHistory() {
    await run('View History', async () => {
      const items = await loadHistory();
      setHistory(items);
      setSelected(previous => items.some(item => String(item.orderId) === previous)
        ? previous : String(items[0]?.orderId ?? ''));
      addLog(formatHistory(items, user.role));
    });
  }

  async function viewDocument(receiptOnly) {
    await run(receiptOnly ? 'View Receipt' : 'View Order', async () => {
      const document = await (receiptOnly ? loadReceipt(selected) : loadOrder(selected));
      addLog(formatReceipt(document));
    });
  }

  async function refund() {
    await run('Refund Order #' + selected, async () => {
      const entry = await refundOrder(selected, await getCsrfToken());
      setHistory(previous => previous.map(item => String(item.orderId) === selected
        ? { ...item, status: 'REFUNDED', refundable: false, refundedAmount: entry.amount, refundedAt: entry.recordedAt }
        : item));
      addLog('Refund recorded for Order #' + entry.orderId);
      try {
        setHistory(await loadHistory());
        addLog(formatReceipt(await loadReceipt(selected)));
      } catch (error) {
        addLog('Refund was recorded, but the display could not refresh: ' + error.message);
      }
    });
  }

  return (
    <section aria-busy={busy}>
      <h3>Output Console</h3>
      <p>History for {user.name} ({user.role.toLowerCase()})</p>
      <div className="prc-controls">
        <button type="button" disabled={busy} onClick={viewHistory}>View History</button>
        {history.length > 0 && <>
          <label>Order
            <select value={selected} disabled={busy} onChange={event => setSelected(event.target.value)}>
              {history.map(item => <option key={item.orderId} value={item.orderId}>
                #{item.orderId} — {item.restaurantName} — {item.status}
              </option>)}
            </select>
          </label>
          {financialAccess && <>
            <button type="button" disabled={busy || !selectedOrder} onClick={() => viewDocument(false)}>View Order</button>
            <button type="button" disabled={busy || !selectedOrder?.receiptAvailable} onClick={() => viewDocument(true)}>View Receipt</button>
          </>}
          {user.role === 'OPERATOR' && <button type="button" disabled={busy || !selectedOrder?.refundable}
            onClick={refund}>Refund</button>}
        </>}
      </div>
      {busy && <p role="status">Loading...</p>}
      <div className="activity-log prc-output" role="log" aria-label="Application output">
        {logs.length === 0 && <p>No commands yet.</p>}
        {logs.map((log, index) => <pre key={index}>{log}</pre>)}
      </div>
    </section>
  );
}
