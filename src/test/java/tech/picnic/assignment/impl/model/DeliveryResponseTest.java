package tech.picnic.assignment.impl.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static tech.picnic.assignment.impl.model.DeliveryStatus.*;

class DeliveryResponseTest {


    @Test
    void delivery_status_is_delivered_iff_at_least_one_of_its_orders_has_status_delivered
            () {
        //Arrange
        final List<OrderResponse> orders = List.of(new OrderResponse("a", BigDecimal.ONE, OrderStatus.DELIVERED),
                new OrderResponse("a", BigDecimal.ONE, OrderStatus.CANCELLED));
        final DeliveryResponse target = new DeliveryResponse("1", Instant.now(), orders);
        //Assert
        assertEquals(DELIVERED, target.getDeliveryStatus());
    }

    @Test
    void delivery_status_is_cancelled_if_none_of_its_orders_has_status_delivered
            () {
        //Arrange
        final List<OrderResponse> orders = List.of(new OrderResponse("a", BigDecimal.ONE, OrderStatus.CREATED),
                new OrderResponse("a", BigDecimal.ONE, OrderStatus.CANCELLED));
        final DeliveryResponse target = new DeliveryResponse("1", Instant.now(), orders);

        //Assert
        assertEquals(CANCELLED, target.getDeliveryStatus());
    }

    @Test
    void total_amount_of_delivered_orders() {
        //Arrange
        final List<OrderResponse> orders = List.of(new OrderResponse("a", BigDecimal.TEN, OrderStatus.DELIVERED),
                new OrderResponse("a", BigDecimal.valueOf(9), OrderStatus.CANCELLED));
        final DeliveryResponse target = new DeliveryResponse("1", Instant.now(), orders);

        //Assert
        assertEquals(BigDecimal.TEN, target.getTotalAmount());
    }

    @Test
    void total_amount_null_if_no_orders_delivered() {
        //Arrange
        final List<OrderResponse> orders = List.of(new OrderResponse("a", BigDecimal.TEN, OrderStatus.CREATED),
                new OrderResponse("a", BigDecimal.valueOf(9), OrderStatus.CANCELLED));
        final DeliveryResponse target = new DeliveryResponse("1", Instant.now(), orders);

        //Assert
        assertNull(target.getTotalAmount());
    }

    @Test
    void orders_sorted_by_id_descending() {
        //Arrange
        final List<OrderResponse> orders = List.of(new OrderResponse("a", BigDecimal.TEN, OrderStatus.CREATED),
                new OrderResponse("c", BigDecimal.valueOf(9), OrderStatus.CANCELLED),
                new OrderResponse("b", BigDecimal.valueOf(9), OrderStatus.CANCELLED));
        final DeliveryResponse target = new DeliveryResponse("1", Instant.now(), orders);

        //Act
        final List<OrderResponse> actualOrders = target.getOrders();

        //Assert
        assertEquals("c", actualOrders.get(0).orderId());
        assertEquals("b", actualOrders.get(1).orderId());
        assertEquals("a", actualOrders.get(2).orderId());
    }
}