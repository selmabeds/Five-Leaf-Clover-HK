package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.prc.infrastructure.*;
import com.careeats.backend.reg.domain.*;
import com.careeats.backend.ord.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.math.BigDecimal;
import java.util.*;

@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ) // tells Spring how to handle database reads of the methods of this class (class OrderHistoryService is a Spring bean, so Spring will manage it and apply the transaction settings to all public methods))
public class OrderHistoryService {
    private final OrderHistoryRepository orders;
    private final LedgerRepository ledger;
    private final Map<Long,Long> assignments;
    public OrderHistoryService(OrderHistoryRepository orders,LedgerRepository ledger,Map<Long,Long> assignments) {
        this.orders=orders; this.ledger=ledger; this.assignments=assignments;
    }

    public record UserContext(Long userId,String name,Role role,Long restaurantId) { }
    public UserContext context(User user) {
        requireUser(user);
        return new UserContext(user.getId(),user.getName(),user.getRole(),
            user.getRole()==Role.RESTAURANT?assignments.get(user.getId()):null);
    }

    public List<OrderHistoryItem> historyFor(User user) {
        requireUser(user);
        return orders.findVisibleTo(user,assignments.get(user.getId())).stream().map(row -> {
            if(user.getRole()==Role.COURIER) return (OrderHistoryItem)new OrderHistoryItem.Courier(
                row.orderId(),row.restaurantId(),row.restaurantName(),row.financialState(),row.placedAt(),row.deliveredAt());
            BigDecimal total=row.totalCharge();
            if(total==null && (row.orderStatus()==OrderStatus.REJECTED || row.orderStatus()==OrderStatus.CANCELLED)) total=new BigDecimal("0.00");
            return (OrderHistoryItem)new OrderHistoryItem.Financial(row.orderId(),row.customerId(),row.restaurantId(),
                row.restaurantName(),row.financialState(),row.placedAt(),row.deliveredAt(),row.chargedAt(),row.refundedAt(),
                total,zero(row.refundedAmount()),row.chargedAt()!=null,
                user.getRole()==Role.OPERATOR && row.chargedAt()!=null && row.refundedAt()==null);
        }).toList();
    }

    public OrderReceipt detailsFor(User user,Long orderId) {
        requireUser(user);

        //Personal design choice was ot  omi torder items, prices and fee breakdown from Couriers (they don't need to know this information, and it is sensitive financial data)
       //Of course courier can still see the orders they deliver, but they don't need to know the financial details of the order
        if(user.getRole()==Role.COURIER) throw new SecurityException("Courier history does not include financial details about customers or restaurants");

        if(orderId==null || orderId<=0) throw new IllegalArgumentException("A positive order ID is required");
        OrderHistoryRecord row=orders.findById(orderId);

        if(row==null) throw new PrcNotFoundException("Order not found");

        boolean visible=switch(user.getRole()) {
            case CUSTOMER -> user.getId().equals(row.customerId());
            case RESTAURANT -> row.restaurantId().equals(assignments.get(user.getId()));
            case OPERATOR -> true;
            case COURIER -> false;
        };


        if(!visible) throw new SecurityException("This order is outside of your visibility scope");

        List<ReceiptLine> lines=orders.findLines(orderId);
        BigDecimal subtotal=lines.stream().map(ReceiptLine::lineTotal).reduce(new BigDecimal("0.00"),BigDecimal::add);
        FareDecomposition fare=ledger.findFare(orderId);
        BigDecimal refunded=zero(row.refundedAmount());
        return new OrderReceipt(row.orderId(),row.customerId(),row.restaurantId(),row.restaurantName(),row.financialState(),
            row.rejectionReason(),row.placedAt(),row.deliveredAt(),lines,subtotal,fare,ledger.findByOrderId(orderId),
            refunded,fare==null?null:fare.totalCharge().subtract(refunded),
            user.getRole()==Role.OPERATOR && row.chargedAt()!=null && row.refundedAt()==null);
    }

    // Receipts with order charge details become available only after the order changes to DELIVERED and its charge has been recorded.
    public OrderReceipt receiptFor(User user,Long orderId) {
        OrderReceipt receipt=detailsFor(user,orderId);
        if(receipt.fare()==null) throw new ReceiptUnavailableException("A receipt is available only after confirmed delivery and billing");
        return receipt;
    }
    private BigDecimal zero(BigDecimal value) { return value==null?new BigDecimal("0.00"):value; }
    private void requireUser(User user) {
        if(user==null || user.getId()==null || user.getRole()==null) throw new SecurityException("Authentication is  required");
    }
}
