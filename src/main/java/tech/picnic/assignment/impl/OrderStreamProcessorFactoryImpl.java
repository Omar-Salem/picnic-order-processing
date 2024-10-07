package tech.picnic.assignment.impl;

import com.google.auto.service.AutoService;
import tech.picnic.assignment.api.*;

import java.time.Duration;

@AutoService(OrderStreamProcessorFactory.class)
public final class OrderStreamProcessorFactoryImpl implements OrderStreamProcessorFactory {
    @Override
    public OrderStreamProcessor createProcessor(int maxOrders, Duration maxTime) {
        return new OrderStreamProcessorImpl(maxOrders, maxTime, new DefaultInputStreamProcessor());
    }
}
