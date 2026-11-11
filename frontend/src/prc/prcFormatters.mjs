const money = value => `$${Number(value).toFixed(2)}`;

export function formatHistory(items, role) {
  if (items.length === 0) {
    return 'History: no orders are available for your account.';
  }

  const isCourier = role === 'COURIER';
  const title = isCourier ? 'Delivery history' : 'Order history';
  const orders = items.map(item => {
    const lines = [
      `Order #${item.orderId} — ${item.status}`,
      `Restaurant: ${item.restaurantName}`,
      `Placed: ${item.placedAt}`,
    ];

    if (item.deliveredAt) {
      lines.push(`Delivered: ${item.deliveredAt}`);
    }

    if (!isCourier) {
      const charge = item.totalCharge == null ? 'Not charged' : money(item.totalCharge);
      lines.push(`Charge: ${charge}`);

      if (Number(item.refundedAmount) > 0) {
        lines.push(`Refund: ${money(item.refundedAmount)}`);
      }
    }

    return lines.join('\n');
  });

  return `${title}\n${orders.join('\n\n')}`;
}

export function formatReceipt(receipt) {
  const { fare } = receipt;
  const lines = [
    `Order #${receipt.orderId} — ${receipt.status}`,
    `Restaurant: ${receipt.restaurantName}`,
    `Placed: ${receipt.placedAt}`,
    '',
  ];

  for (const item of receipt.lines) {
    const unitPrice = money(item.unitPrice);
    const lineTotal = money(item.lineTotal);
    lines.push(`${item.description} × ${item.quantity} @ ${unitPrice} = ${lineTotal}`);
  }

  lines.push('', `Items subtotal: ${money(receipt.itemSubtotal)}`);
  if (receipt.rejectionReason) {
    lines.push(`Reason: ${receipt.rejectionReason}`);
  }

  if (!fare) {
    lines.push('Not billed. A receipt becomes available after confirmed delivery and billing.');
    return lines.join('\n');
  }

  lines.push(
    `Delivery fee: ${money(fare.deliveryFee)}`,
    `Service fee: ${money(fare.serviceFee)}`,
    `Small-order surcharge: ${money(fare.smallOrderSurcharge)}`,
    `Original total charged: ${money(fare.totalCharge)}`,
    `Delivered: ${receipt.deliveredAt}`,
    '',
  );

  for (const entry of receipt.entries) {
    const sign = entry.type === 'REFUND' ? '-' : '';
    lines.push(`${entry.type}: ${sign}${money(entry.amount)} — ${entry.recordedAt}`);
  }

  if (receipt.entries.some(entry => entry.type === 'REFUND')) {
    lines.push(`Total after refund: ${money(receipt.netCharge)}`);
  }

  return lines.join('\n');
}
