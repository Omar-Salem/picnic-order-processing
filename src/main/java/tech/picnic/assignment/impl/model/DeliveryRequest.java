package tech.picnic.assignment.impl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public record DeliveryRequest(@JsonProperty("delivery_id") String deliveryId,
                              @JsonProperty("delivery_time") String deliveryTime) {
    public Instant getDeliveryTime() {
        return Instant.parse(deliveryTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryRequest that = (DeliveryRequest) o;
        return Objects.equals(deliveryId, that.deliveryId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deliveryId);
    }
}
