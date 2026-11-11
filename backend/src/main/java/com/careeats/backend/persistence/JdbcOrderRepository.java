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
        return readOrders("o.id=?", "o.id", id).stream().findFirst().orElse(null);
    }
    @Override public List<Order> findAll() {
        return readOrders("1=1", "o.id");
    }
    @Override public List<Order> findByCustomerId(Long customerId) {
        return readOrders("o.customer_id=?", "o.id", customerId);
    }
    @Override public List<Order> findByRestaurantId(Long restaurantId) {
        return readOrders("o.restaurant_id=?", "o.id", restaurantId);
    }
    @Override public List<Order> findAcceptedForRestaurant(Long restaurantId) {
        return readOrders("o.restaurant_id=? AND o.status='ACCEPTED'", "o.accepted_sequence", restaurantId);
    }

    private record OrderRow(long id, long customerId, long restaurantId, OrderStatus status, String reason) { }

    /** Three queries per result set, independent of the number of orders returned. */
    private List<Order> readOrders(String condition, String ordering, Object... args) {
        List<OrderRow> rows = jdbc.query("SELECT o.* FROM careeats.orders o WHERE " + condition + " ORDER BY " + ordering,
            (rs,n) -> new OrderRow(rs.getLong("id"), rs.getLong("customer_id"), rs.getLong("restaurant_id"),
                OrderStatus.valueOf(rs.getString("status")), rs.getString("rejection_reason")), args);
        if (rows.isEmpty()) return List.of();

        Map<Long,List<OrderLine>> lines = new HashMap<>();
        jdbc.query("SELECT l.* FROM careeats.order_lines l JOIN careeats.orders o ON o.id=l.order_id WHERE "
            + condition + " ORDER BY l.order_id,l.position", rs -> {
                lines.computeIfAbsent(rs.getLong("order_id"), ignored -> new ArrayList<>())
                    .add(new OrderLine(rs.getLong("item_id"),rs.getInt("quantity")));
            }, args);
        Map<Long,List<StateTransitionRecord>> histories = new HashMap<>();
        jdbc.query("SELECT t.* FROM careeats.order_transitions t JOIN careeats.orders o ON o.id=t.order_id WHERE "
            + condition + " ORDER BY t.order_id,t.position", rs -> {
                long id = rs.getLong("order_id");
                histories.computeIfAbsent(id, ignored -> new ArrayList<>()).add(new StateTransitionRecord(
                    rs.getString("event_id"), id,
                    rs.getString("previous_status")==null ? null : OrderStatus.valueOf(rs.getString("previous_status")),
                    OrderStatus.valueOf(rs.getString("new_status")),rs.getTimestamp("occurred_at").toLocalDateTime()));
            }, args);
        return rows.stream().map(row -> Order.restore(row.id(), row.customerId(), row.restaurantId(),
            lines.getOrDefault(row.id(), List.of()), row.status(), row.reason(),
            histories.getOrDefault(row.id(), List.of()))).toList();
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
                var item=jdbc.queryForMap("SELECT name,price FROM careeats.menu_items WHERE restaurant_id=? AND item_id=?",
                    order.getRestaurantId(),line.getMenuItemId());
                jdbc.update("INSERT INTO careeats.order_lines(order_id,restaurant_id,position,item_id,quantity,unit_price,item_name) VALUES (?,?,?,?,?,?,?)",
                    id,order.getRestaurantId(),position++,line.getMenuItemId(),line.getQuantity(),item.get("price"),item.get("name"));
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
