package com.careeats.backend.dlv.infrastructure;

import com.careeats.backend.dlv.domain.Assignment;
import com.careeats.backend.dlv.domain.Courier;
import com.careeats.backend.dlv.domain.CourierState;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class DeliveryRepository {
    private final JdbcTemplate jdbc;

    public DeliveryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Courier findCourier(long courierId) {
        return jdbc.query(
                "SELECT * FROM careeats.couriers WHERE id = ?",
                (rs, row) -> readCourier(rs),
                courierId
        ).stream().findFirst().orElse(null);
    }

    public void createCourier(Courier courier) {
        jdbc.update("""
                INSERT INTO careeats.couriers
                    (id, latitude, longitude, state)
                VALUES (?, ?, ?, ?)
                """,
                courier.id(),
                courier.latitude(),
                courier.longitude(),
                courier.state().name());
    }

    public void updateCourierState(long courierId, CourierState state) {
        jdbc.update(
                "UPDATE careeats.couriers SET state = ? WHERE id = ?",
                state.name(),
                courierId);
    }

    public List<Courier> findAvailableCouriers() {
        return jdbc.query("""
                SELECT c.*
                FROM careeats.couriers c
                WHERE c.state = 'IDLE'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM careeats.delivery_assignments a
                      WHERE a.courier_id = c.id
                  )
                ORDER BY c.id
                """,
                (rs, row) -> readCourier(rs));
    }

    public List<Courier> findOnDutyCouriers() {
        return jdbc.query("""
                SELECT *
                FROM careeats.couriers
                WHERE state <> 'OFFLINE'
                ORDER BY id
                """,
                (rs, row) -> readCourier(rs));
    }

    public record WaitingOrder(
            long orderId,
            double latitude,
            double longitude
    ) {}

    public List<WaitingOrder> findWaitingOrders() {
        return jdbc.query("""
                SELECT o.id, r.latitude, r.longitude
                FROM careeats.orders o
                JOIN careeats.restaurants r
                  ON r.id = o.restaurant_id
                JOIN careeats.order_transitions t
                  ON t.order_id = o.id
                 AND t.new_status = 'READY'
                WHERE o.status = 'READY'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM careeats.delivery_assignments a
                      WHERE a.order_id = o.id
                  )
                ORDER BY t.occurred_at, o.id
                """,
                (rs, row) -> new WaitingOrder(
                        rs.getLong("id"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")));
    }

    public Assignment findAssignmentForCourier(long courierId) {
        return jdbc.query("""
                SELECT *
                FROM careeats.delivery_assignments
                WHERE courier_id = ?
                """,
                (rs, row) -> readAssignment(rs),
                courierId
        ).stream().findFirst().orElse(null);
    }

    public Assignment findAssignmentForOrder(long orderId) {
        return jdbc.query("""
                SELECT *
                FROM careeats.delivery_assignments
                WHERE order_id = ?
                """,
                (rs, row) -> readAssignment(rs),
                orderId
        ).stream().findFirst().orElse(null);
    }

    public List<Assignment> findStalledAssignments() {
        return jdbc.query("""
                SELECT *
                FROM careeats.delivery_assignments
                WHERE status = 'STALLED'
                ORDER BY order_id
                """,
                (rs, row) -> readAssignment(rs));
    }

    public void createOffer(long orderId, long courierId) {
        jdbc.update("""
                INSERT INTO careeats.delivery_assignments
                    (order_id, courier_id, status)
                VALUES (?, ?, 'OFFERED')
                """,
                orderId,
                courierId);
    }

    public void updateAssignmentStatus(
            long orderId,
            Assignment.Status status
    ) {
        jdbc.update("""
                UPDATE careeats.delivery_assignments
                SET status = ?
                WHERE order_id = ?
                """,
                status.name(),
                orderId);
    }

    public void removeAssignment(long orderId) {
        jdbc.update(
                "DELETE FROM careeats.delivery_assignments WHERE order_id = ?",
                orderId);
    }

    private Courier readCourier(ResultSet rs) throws SQLException {
        return new Courier(
                rs.getLong("id"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                CourierState.valueOf(rs.getString("state")));
    }

    private Assignment readAssignment(ResultSet rs) throws SQLException {
        return new Assignment(
                rs.getLong("order_id"),
                rs.getLong("courier_id"),
                Assignment.Status.valueOf(rs.getString("status")));
    }
}