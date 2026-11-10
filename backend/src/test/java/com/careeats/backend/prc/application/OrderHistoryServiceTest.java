package com.careeats.backend.prc.application;

import com.careeats.backend.prc.domain.*;
import com.careeats.backend.prc.infrastructure.*;
import com.careeats.backend.reg.domain.*;
import com.careeats.backend.ord.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderHistoryServiceTest {
    private final OrderHistoryRepository repository=mock(OrderHistoryRepository.class);
    private final LedgerRepository ledger=new LedgerRepository();

    private final HistoryVisibilityStrategies strategies =
        new HistoryVisibilityStrategies(List.of(
            new CustomerHistoryStrategy(),
            new RestaurantHistoryStrategy(),
            new OperatorHistoryStrategy(),
            new CourierHistoryStrategy()
        ));
        private final OrderHistoryService history = new OrderHistoryService(repository, ledger, Map.of(2L, 5L), strategies);

    private User user(long id,Role role) { return new User(id,role.name(),id+"@example.test","hash",role); }
    private OrderHistoryRecord row(OrderStatus status) {
        return new OrderHistoryRecord(10L,1L,5L,"Test Restaurant",status,null,LocalDateTime.of(2026,11,1,12,0),
            3L,Instant.parse("2026-11-01T13:00:00Z"),new BigDecimal("26.39"),Instant.parse("2026-11-01T13:00:01Z"),null,null);
    }
    @Test void authorizationAppliesToDetailsAndReceiptReads() {
        when(repository.findById(10L)).thenReturn(row(OrderStatus.DELIVERED));
        when(repository.findLines(10L)).thenReturn(List.of(new ReceiptLine(1L,"Meal",2,new BigDecimal("10.00"))));
        assertEquals(10L,history.detailsFor(user(1L,Role.CUSTOMER),10L).orderId());
        assertEquals(10L,history.detailsFor(user(2L,Role.RESTAURANT),10L).orderId());
        assertThrows(SecurityException.class,()->history.detailsFor(user(9L,Role.CUSTOMER),10L));
        assertThrows(SecurityException.class,()->history.detailsFor(user(9L,Role.RESTAURANT),10L));
        assertThrows(SecurityException.class,()->history.detailsFor(user(3L,Role.COURIER),10L));
        assertThrows(PrcNotFoundException.class,()->history.detailsFor(user(4L,Role.OPERATOR),999L));
        assertThrows(IllegalArgumentException.class,()->history.detailsFor(user(4L,Role.OPERATOR),0L));
        assertThrows(ReceiptUnavailableException.class,()->history.receiptFor(user(1L,Role.CUSTOMER),10L));
    }
    @Test void courierResponseTypeHasNoFinancialFields() {
        User courier=user(3L,Role.COURIER);
        when(repository.findVisibleTo(courier,null)).thenReturn(List.of(row(OrderStatus.DELIVERED)));
        var result=history.historyFor(courier);
        assertInstanceOf(OrderHistoryItem.Courier.class,result.get(0));
        assertEquals(10L,result.get(0).orderId());
    }
    @Test void restaurantScopeUsesAssignmentRatherThanUserId() {
        User staff=user(2L,Role.RESTAURANT);
        when(repository.findVisibleTo(staff,5L)).thenReturn(List.of(row(OrderStatus.DELIVERED)));
        assertEquals(1,history.historyFor(staff).size());
        assertEquals(5L,history.context(staff).restaurantId());
        verify(repository).findVisibleTo(staff,5L);
    }
}
