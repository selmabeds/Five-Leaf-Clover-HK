package com.careeats.backend.persistence;

import com.careeats.backend.ord.domain.*;
import com.careeats.backend.ord.infrastructure.OrderRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Transactional
public class JdbcOrderRepository extends OrderRepository {
    private final JdbcTemplate jdbc;
    public JdbcOrderRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public long nextId() { return jdbc.queryForObject("SELECT nextval('careeats.order_ids')",Long.class); }
    @Override public void lockForWrite() { DatabaseLock.acquire(jdbc); }
    @Override public boolean existsById(Long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM careeats.orders WHERE id=?",Long.class,id)>0;
    }
    @Override public Order findById(Long id) {
        return jdbc.query("SELECT * FROM careeats.orders WHERE id=?",(rs,n) -> {
            List<OrderLine> lines=jdbc.query("SELECT item_id,quantity FROM careeats.order_lines WHERE order_id=? ORDER BY position",
                (r,i) -> new OrderLine(r.getLong(1),r.getInt(2)),id);
            List<StateTransitionRecord> history=jdbc.query("SELECT * FROM careeats.order_transitions WHERE order_id=? ORDER BY position",
                (r,i) -> new StateTransitionRecord(r.getString("event_id"),id,
                    r.getString("previous_status")==null?null:OrderStatus.valueOf(r.getString("previous_status")),
                    OrderStatus.valueOf(r.getString("new_status")),r.getTimestamp("occurred_at").toLocalDateTime()),id);
            return Order.restore(id,rs.getLong("customer_id"),rs.getLong("restaurant_id"),lines,
                OrderStatus.valueOf(rs.getString("status")),rs.getString("rejection_reason"),history);
        },id).stream().findFirst().orElse(null);
    }
    @Override public List<Order> findAll() {
        return jdbc.queryForList("SELECT id FROM careeats.orders ORDER BY id",Long.class).stream().map(this::findById).toList();
    }
    @Override public List<Order> findAcceptedForRestaurant(Long restaurantId) {
        return jdbc.queryForList("SELECT id FROM careeats.orders WHERE restaurant_id=? AND status='ACCEPTED' ORDER BY accepted_sequence",
            Long.class,restaurantId).stream().map(this::findById).toList();
    }
    @Override public void save(Order order) {
        if(order==null) throw new IllegalArgumentException("Order cannot be null");
        lockForWrite();
        long id=order.getOrderId();
        if(!existsById(id)) {
            Long sequence=order.getStatus()==OrderStatus.ACCEPTED?nextAcceptance():null;
            jdbc.update("INSERT INTO careeats.orders(id,customer_id,restaurant_id,status,rejection_reason,accepted_sequence) VALUES (?,?,?,?,?,?)",
                id,order.getCustomerId(),order.getRestaurantId(),order.getStatus().name(),order.getRejectionReason(),sequence);
            int position=0;
            for(OrderLine line:order.getOrderLines()) {
                var price=jdbc.queryForObject("SELECT price FROM careeats.menu_items WHERE restaurant_id=? AND item_id=?",
                    java.math.BigDecimal.class,order.getRestaurantId(),line.getMenuItemId());
                jdbc.update("INSERT INTO careeats.order_lines(order_id,restaurant_id,position,item_id,quantity,unit_price) VALUES (?,?,?,?,?,?)",
                    id,order.getRestaurantId(),position++,line.getMenuItemId(),line.getQuantity(),price);
            }
        } else {
            Long sequence=jdbc.queryForObject("SELECT accepted_sequence FROM careeats.orders WHERE id=?",Long.class,id);
            if(sequence==null && order.getStatus()==OrderStatus.ACCEPTED) sequence=nextAcceptance();
            jdbc.update("UPDATE careeats.orders SET status=?,rejection_reason=?,accepted_sequence=? WHERE id=?",
                order.getStatus().name(),order.getRejectionReason(),sequence,id);
        }
        int position=0;
        for(StateTransitionRecord event:order.getTransitionHistory()) {
            if(jdbc.queryForObject("SELECT COUNT(*) FROM careeats.order_transitions WHERE event_id=?",Long.class,event.getEventId())==0)
                jdbc.update("INSERT INTO careeats.order_transitions(event_id,order_id,position,previous_status,new_status,occurred_at) VALUES (?,?,?,?,?,?)",
                    event.getEventId(),id,position,event.getPreviousStatus()==null?null:event.getPreviousStatus().name(),
                    event.getNewStatus().name(),event.getTimestamp());
            position++;
        }
    }
    private long nextAcceptance() { return jdbc.queryForObject("SELECT nextval('careeats.acceptance_ids')",Long.class); }
}
