import test from 'node:test';
import assert from 'node:assert/strict';
import { formatHistory, formatReceipt } from './prcFormatters.mjs';

test('courier output never prints financial values', () => {
  const text = formatHistory([{ orderId: 1, restaurantName: 'Restaurant', status: 'DELIVERED', placedAt: '2026-11-01',
    totalCharge: 999, refundedAmount: 999 }], 'COURIER');
  assert.ok(!text.includes('$'));
  assert.ok(!text.includes('999'));
  assert.ok(text.includes('\nRestaurant: Restaurant'));
});
test('receipt shows original charge and separate refund', () => {
  const text = formatReceipt({ orderId: 1, restaurantName: 'Restaurant', status: 'REFUNDED', placedAt: '2026-11-01',
    deliveredAt: '2026-11-01', lines: [{ description: 'Meal', quantity: 2, unitPrice: 10, lineTotal: 20 }],
    itemSubtotal: 20, fare: { deliveryFee: 3.99, serviceFee: 2.4, smallOrderSurcharge: 0, totalCharge: 26.39 },
    entries: [{ type: 'CHARGE', amount: 26.39, recordedAt: '2026-11-01' },
      { type: 'REFUND', amount: 26.39, recordedAt: '2026-11-02' }], netCharge: 0 });
  assert.ok(text.includes('CHARGE: $26.39'));
  assert.ok(text.includes('REFUND: -$26.39'));
  assert.ok(text.includes('Total after refund: $0.00'));
});
test('uncharged order details do not invent a fare', () => {
  const text = formatReceipt({ orderId: 2, restaurantName: 'Restaurant', status: 'READY', placedAt: '2026-11-01',
    lines: [], itemSubtotal: 0, fare: null });
  assert.ok(text.includes('Not billed'));
  assert.ok(!text.includes('Original total charged'));
});
test('empty history is explicit', () => assert.ok(formatHistory([], 'CUSTOMER').includes('no orders')));
