package tech.picnic.assignment.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.skyscreamer.jsonassert.*;
import reactor.core.publisher.Flux;
import tech.picnic.assignment.api.*;
import tech.picnic.assignment.impl.model.*;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static tech.picnic.assignment.impl.ObjectMapperFactory.createObjectMapper;

final class OrderInputStreamProcessorFactoryImplTest {

    private final InputStreamProcessor inputStreamProcessorMock = mock(InputStreamProcessor.class);
    private final ObjectMapper objectMapper = createObjectMapper();

    private static Stream<Arguments> provideOrderProcessingInput() {
        return Stream.of(
                Arguments.of(
                        100, Duration.ofSeconds(30), "happy-path-input.json-stream", "happy-path-output.json"));
    }

    @ParameterizedTest
    @MethodSource("provideOrderProcessingInput")
    void processing_orders_json_stream(
            int maxOrders, Duration maxTime, String inputResource, String expectedOutputResource)
            throws IOException, JSONException {
        try (InputStream source = getClass().getResourceAsStream(inputResource);
             ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            OrderStreamProcessorFactory factory = new OrderStreamProcessorFactoryImpl();
            OrderStreamProcessor processor = factory.createProcessor(maxOrders, maxTime);
            processor.process(source, sink);
            String expectedOutput = loadResource(expectedOutputResource);
            String actualOutput = sink.toString(StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expectedOutput, actualOutput, JSONCompareMode.STRICT);
        }
    }

    @Test
    void processing_orders_filters_by_order_status() throws IOException {
        //Arrange
        final Flux<OrderRequest> orderRequestFlux = Flux.just(
                new OrderRequest("1", OrderStatus.CREATED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("2", OrderStatus.CANCELLED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("3", OrderStatus.DELIVERED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN));
        final int maxOrders = 100;
        final Duration maxTime = Duration.ofSeconds(30);
        when(inputStreamProcessorMock.convertStream(any(), eq(maxOrders), eq(maxTime)))
                .thenReturn(orderRequestFlux);
        final OrderStreamProcessorImpl target = new OrderStreamProcessorImpl(maxOrders, maxTime, inputStreamProcessorMock);

        //Act
        final DeliveryResponse[] deliveryResponses;
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            target.process(null, sink);
            final String actualOutput = sink.toString(StandardCharsets.UTF_8);
            deliveryResponses = objectMapper.readValue(actualOutput, DeliveryResponse[].class);
        }

        //Assert
        assertEquals(1, deliveryResponses.length);
        assertEquals(2, deliveryResponses[0].orders().size());
    }

    @Test
    void orders_grouped_by_delivery_id() throws IOException {
        //Arrange
        final Flux<OrderRequest> orderRequestFlux = Flux.just(
                new OrderRequest("1", OrderStatus.CANCELLED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("2", OrderStatus.DELIVERED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("3", OrderStatus.CANCELLED, new DeliveryRequest("b", "2022-05-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("4", OrderStatus.DELIVERED, new DeliveryRequest("b", "2022-05-20T11:50:48Z"), BigDecimal.TEN));
        final int maxOrders = 100;
        final Duration maxTime = Duration.ofSeconds(30);
        when(inputStreamProcessorMock.convertStream(any(), eq(maxOrders), eq(maxTime)))
                .thenReturn(orderRequestFlux);
        final OrderStreamProcessorImpl target = new OrderStreamProcessorImpl(maxOrders, maxTime, inputStreamProcessorMock);

        //Act
        final DeliveryResponse[] deliveryResponses;
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            target.process(null, sink);
            final String actualOutput = sink.toString(StandardCharsets.UTF_8);
            deliveryResponses = objectMapper.readValue(actualOutput, DeliveryResponse[].class);
        }

        //Assert
        assertEquals(2, deliveryResponses.length);
        final DeliveryResponse firstDelivery = deliveryResponses[0];
        final DeliveryResponse secondDelivery = deliveryResponses[1];

        assertEquals("a", firstDelivery.deliveryId());
        assertEquals("2", firstDelivery.orders().get(0).orderId());
        assertEquals("1", firstDelivery.orders().get(1).orderId());

        assertEquals("b", secondDelivery.deliveryId());
        assertEquals("4", secondDelivery.orders().get(0).orderId());
        assertEquals("3", secondDelivery.orders().get(1).orderId());
    }

    @Test
    void deliveries_sorted_chronologically_ascending_by_their_delivery_time_breaking_ties_by_id() throws IOException {
        //Arrange
        final Flux<OrderRequest> orderRequestFlux = Flux.just(
                new OrderRequest("1", OrderStatus.CANCELLED, new DeliveryRequest("a", "2022-05-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("2", OrderStatus.CANCELLED, new DeliveryRequest("b", "2022-03-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("3", OrderStatus.DELIVERED, new DeliveryRequest("c", "2022-04-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("4", OrderStatus.DELIVERED, new DeliveryRequest("e", "2022-04-20T11:50:48Z"), BigDecimal.TEN),
                new OrderRequest("5", OrderStatus.DELIVERED, new DeliveryRequest("d", "2022-04-20T11:50:48Z"), BigDecimal.TEN));
        final int maxOrders = 100;
        final Duration maxTime = Duration.ofSeconds(30);
        when(inputStreamProcessorMock.convertStream(any(), eq(maxOrders), eq(maxTime)))
                .thenReturn(orderRequestFlux);
        final OrderStreamProcessorImpl target = new OrderStreamProcessorImpl(maxOrders, maxTime, inputStreamProcessorMock);

        //Act
        final DeliveryResponse[] deliveryResponses;
        try (ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
            target.process(null, sink);
            final String actualOutput = sink.toString(StandardCharsets.UTF_8);
            deliveryResponses = objectMapper.readValue(actualOutput, DeliveryResponse[].class);
        }

        //Assert
        assertEquals(5, deliveryResponses.length);
        assertEquals("b", deliveryResponses[0].deliveryId());
        assertEquals("c", deliveryResponses[1].deliveryId());
        assertEquals("d", deliveryResponses[2].deliveryId());
        assertEquals("e", deliveryResponses[3].deliveryId());
        assertEquals("a", deliveryResponses[4].deliveryId());
    }

    private String loadResource(String resource) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resource);
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    /**
     * Verifies that precisely one {@link OrderStreamProcessorFactory} can be service-loaded.
     */
    @Test
    void testServiceLoading() {
        Iterator<OrderStreamProcessorFactory> factories =
                ServiceLoader.load(OrderStreamProcessorFactory.class).iterator();
        assertTrue(factories.hasNext(), "No OrderStreamProcessorFactory is service-loaded");
        factories.next();
        assertFalse(factories.hasNext(), "More than one OrderStreamProcessorFactory is service-loaded");
    }
}
