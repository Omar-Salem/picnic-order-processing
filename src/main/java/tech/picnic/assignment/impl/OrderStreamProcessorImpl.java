package tech.picnic.assignment.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.Exceptions;
import reactor.core.publisher.*;
import tech.picnic.assignment.api.*;
import tech.picnic.assignment.impl.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static tech.picnic.assignment.impl.ObjectMapperFactory.createObjectMapper;
import static tech.picnic.assignment.impl.model.OrderStatus.*;

public class OrderStreamProcessorImpl implements OrderStreamProcessor {
    private final int maxOrders;
    private final Duration maxTime;
    private final InputStreamProcessor inputStreamProcessor;
    private static final Set<OrderStatus> ELIGIBLE_ORDER_STATUSES = Set.of(DELIVERED, CANCELLED);
    private final ObjectMapper objectMapper = createObjectMapper();

    public OrderStreamProcessorImpl(int maxOrders, Duration maxTime, InputStreamProcessor inputStreamProcessor) {
        this.maxOrders = maxOrders;
        this.maxTime = maxTime;
        this.inputStreamProcessor = inputStreamProcessor;
    }

    @Override
    public void process(InputStream source, OutputStream sink) {
        final Flux<OrderRequest> orderRequestFlux = inputStreamProcessor.convertStream(source, maxOrders, maxTime);
        orderRequestFlux
                .filter(orderRequest -> ELIGIBLE_ORDER_STATUSES.contains(orderRequest.orderStatus()))
                .groupBy(OrderRequest::delivery)
                .flatMap(this::buildDeliveryResponse)
                .collectSortedList()
                .subscribe(deliveryResponses -> {
                    try {
                        var json = objectMapper.writeValueAsString(deliveryResponses);
                        sink.write(json.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw Exceptions.propagate(e);
                    }
                });
    }

    private Mono<DeliveryResponse> buildDeliveryResponse(GroupedFlux<DeliveryRequest, OrderRequest> group) {
        final DeliveryRequest delivery = group.key();
        return group
                .collectList()
                .map(orders -> {
                    final List<OrderResponse> orderResponses = orders
                            .stream()
                            .map(o -> new OrderResponse(o.orderId(), o.amount(), o.orderStatus()))
                            .toList();
                    return new DeliveryResponse(delivery.deliveryId(),
                            delivery.getDeliveryTime(),
                            orderResponses);
                });
    }
}
