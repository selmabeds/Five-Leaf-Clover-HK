const money = value => '$' + Number(value).toFixed(2);

export function formatHistory(items, role) {
  if (!items.length) return 'History: no orders are available for your account.';
  const title = role === 'COURIER' ? 'Delivery history' : 'Order history';
  return title + '\n' + items.map(item => {
    let text = 'Order #' + item.orderId + ' — ' + item.status + '\nRestaurant: ' + item.restaurantName
      + '\nPlaced: ' + item.placedAt;
    if (item.deliveredAt) text += '\nDelivered: ' + item.deliveredAt;
    if (role !== 'COURIER') {
      text += '\nCharge: ' + (item.totalCharge == null ? 'Not charged' : money(item.totalCharge));
      if (Number(item.refundedAmount) > 0) text += '\nRefund: ' + money(item.refundedAmount);
    }
    return text;
  }).join('\n\n');
}

export function formatReceipt(receipt) {
  const text = ['Order #' + receipt.orderId + ' — ' + receipt.status,
    'Restaurant: ' + receipt.restaurantName, 'Placed: ' + receipt.placedAt, ''];
  for (const line of receipt.lines) text.push(line.description + ' × ' + line.quantity + ' @ '
    + money(line.unitPrice) + ' = ' + money(line.lineTotal));
  text.push('', 'Items subtotal: ' + money(receipt.itemSubtotal));
  if (receipt.rejectionReason) text.push('Reason: ' + receipt.rejectionReason);
  if (!receipt.fare) {
    text.push('Not billed. A receipt becomes available after confirmed delivery and billing.');
  } else {
    text.push('Delivery fee: ' + money(receipt.fare.deliveryFee), 'Service fee: ' + money(receipt.fare.serviceFee),
      'Small-order surcharge: ' + money(receipt.fare.smallOrderSurcharge),
      'Original total charged: ' + money(receipt.fare.totalCharge), 'Delivered: ' + receipt.deliveredAt, '');
    for (const entry of receipt.entries) text.push(entry.type + ': '
      + (entry.type === 'REFUND' ? '-' : '') + money(entry.amount) + ' — ' + entry.recordedAt);
    text.push('Net charge: ' + money(receipt.netCharge));
  }
  return text.join('\n');
}
