package tech.picnic.assignment.impl.model;

import com.fasterxml.jackson.annotation.*;

import java.math.BigDecimal;
import java.util.Objects;

public record OrderRequest(@JsonProperty("order_id") String orderId,
                           @JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                           @JsonProperty("order_status") OrderStatus orderStatus,
                           DeliveryRequest delivery,
                           BigDecimal amount) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderRequest that = (OrderRequest) o;
        return Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(orderId);
    }
}
