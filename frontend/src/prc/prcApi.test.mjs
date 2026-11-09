import test from 'node:test';
import assert from 'node:assert/strict';
import { loadHistory, refundOrder } from './prcApi.mjs';

test('history uses the authenticated session', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/prc/history');
      assert.equal(options.credentials, 'include');
      return { ok: true, json: async () => [] };
    };
    assert.deepEqual(await loadHistory(), []);
  } finally { globalThis.fetch = original; }
});
test('refund carries CSRF token and reports server validation errors', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async (url, options) => {
      assert.equal(url, '/api/prc/orders/10/refund');
      assert.equal(options.method, 'POST');
      assert.equal(options.headers['X-CSRF-TOKEN'], 'token');
      return { ok: false, status: 400, text: async () => '{"error":"Already refunded"}' };
    };
    await assert.rejects(refundOrder(10, 'token'), /Already refunded/);
  } finally { globalThis.fetch = original; }
});
test('expired sessions produce a clear message', async () => {
  const original = globalThis.fetch;
  try {
    globalThis.fetch = async () => ({ ok: false, status: 401 });
    await assert.rejects(loadHistory(), /session expired/);
  } finally { globalThis.fetch = original; }
});
