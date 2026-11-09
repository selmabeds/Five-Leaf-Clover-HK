package com.careeats.backend.persistence;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.prc.infrastructure.LedgerRepository;
import com.careeats.backend.ord.domain.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Transactional
public class JdbcLedgerRepository extends LedgerRepository {
    private final JdbcTemplate jdbc;
    public JdbcLedgerRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void lockForWrite() { DatabaseLock.acquire(jdbc); }
    @Override public void save(LedgerEntry entry) {
        if(entry==null) throw new IllegalArgumentException("Ledger entry cannot be null");
        lockForWrite();
        if(entry.type()==LedgerEntryType.CHARGE) {
            // Cross-check existing order participants before retaining financial completion facts.
            Long count=jdbc.queryForObject("SELECT COUNT(*) FROM careeats.orders WHERE id=? AND customer_id=? AND restaurant_id=?",
                Long.class,entry.orderId(),entry.customerId(),entry.restaurantId());
            if(count==0) throw new IllegalArgumentException("Charge must reference an existing order and its participants");
            jdbc.update("INSERT INTO careeats.delivered_orders(order_id,customer_id,restaurant_id,courier_id,placed_at,delivered_at) VALUES (?,?,?,?,?,?)",
                entry.orderId(),entry.customerId(),entry.restaurantId(),entry.courierId(),
                entry.orderPlacedAt().atOffset(ZoneOffset.UTC),entry.deliveredAt().atOffset(ZoneOffset.UTC));
        } else {
            LedgerEntry charge=findByOrderId(entry.orderId()).stream().filter(e->e.type()==LedgerEntryType.CHARGE)
                .findFirst().orElseThrow(()->new IllegalStateException("A delivered charge is required before refunding"));
            if(charge.amount().compareTo(entry.amount())!=0) throw new IllegalArgumentException("Refund must match charge amount");
        }
        jdbc.update("INSERT INTO careeats.ledger_entries(order_id,entry_type,order_state,amount,recorded_at) VALUES (?,?,?,?,?)",
            entry.orderId(),entry.type().name(),entry.orderState().name(),entry.amount(),entry.recordedAt().atOffset(ZoneOffset.UTC));
    }
    @Override public void saveFare(DeliveredOrder order,FareDecomposition fare) {
        lockForWrite();
        var savedLines = jdbc.query("SELECT item_id,quantity,unit_price FROM careeats.order_lines WHERE order_id=? ORDER BY position",
            (rs,n) -> new PricedOrderLine(rs.getLong(1),rs.getInt(2),rs.getBigDecimal(3)),order.orderId());
        if (savedLines.size() != order.orderLines().size()) {
            throw new IllegalArgumentException("Financial lines must match the placed order");
        }
        for (int i=0; i<savedLines.size(); i++) {
            PricedOrderLine saved=savedLines.get(i), submitted=order.orderLines().get(i);
            if (!saved.menuItemId().equals(submitted.menuItemId()) || saved.quantity()!=submitted.quantity()
                    || saved.unitPrice().compareTo(submitted.unitPrice())!=0) {
                throw new IllegalArgumentException("Financial lines must use the order's historical price snapshots");
            }
        }
        jdbc.update("INSERT INTO careeats.fare_snapshots(order_id,item_subtotal,delivery_fee,service_fee,small_order_surcharge,total_charge) VALUES (?,?,?,?,?,?)",
            order.orderId(),fare.itemSubtotal(),fare.deliveryFee(),fare.serviceFee(),fare.smallOrderSurcharge(),fare.totalCharge());
        int position=0;
        for(PricedOrderLine line:order.orderLines()) jdbc.update(
            "INSERT INTO careeats.priced_lines(order_id,position,item_id,quantity,unit_price) VALUES (?,?,?,?,?)",
            order.orderId(),position++,line.menuItemId(),line.quantity(),line.unitPrice());
    }
    private List<LedgerEntry> query(String where,Object... args) {
        return jdbc.query("SELECT l.*,d.customer_id,d.restaurant_id,d.courier_id,d.placed_at,d.delivered_at FROM careeats.ledger_entries l JOIN careeats.delivered_orders d ON d.order_id=l.order_id "
            +where+" ORDER BY l.order_id, CASE WHEN l.entry_type='CHARGE' THEN 0 ELSE 1 END",(rs,n)->new LedgerEntry(
                rs.getLong("order_id"),LedgerEntryType.valueOf(rs.getString("entry_type")),OrderStatus.valueOf(rs.getString("order_state")),
                rs.getLong("customer_id"),rs.getLong("restaurant_id"),rs.getObject("courier_id",Long.class),rs.getBigDecimal("amount"),
                rs.getObject("placed_at",OffsetDateTime.class).toInstant(),rs.getObject("delivered_at",OffsetDateTime.class).toInstant(),
                rs.getObject("recorded_at",OffsetDateTime.class).toInstant()),args);
    }
    @Override public List<LedgerEntry> findByOrderId(Long id) {
        if(id==null) throw new IllegalArgumentException("Order ID is needed");
        return List.copyOf(query("WHERE l.order_id=?",id));
    }
    @Override public List<LedgerEntry> findAll() { return List.copyOf(query("")); }
}
