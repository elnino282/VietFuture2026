package org.example.sustainability.config;

import org.example.sustainability.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void sustainabilityEventsQueue_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        assertEquals("sustainability-service.events", queue.getName());
        assertNotNull(queue);
    }

    @Test
    void farmDeletedBinding_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        TopicExchange exchange = config.farmExchange();
        Binding binding = config.farmDeletedBinding(queue, exchange);
        assertNotNull(binding);
        assertEquals("farm.event.farm.deleted", binding.getRoutingKey());
    }

    @Test
    void plotDeletedBinding_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        TopicExchange exchange = config.farmExchange();
        Binding binding = config.plotDeletedBinding(queue, exchange);
        assertNotNull(binding);
        assertEquals("farm.event.plot.deleted", binding.getRoutingKey());
    }

    @Test
    void seasonUpdatedBinding_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        TopicExchange exchange = config.seasonExchange();
        Binding binding = config.seasonUpdatedBinding(queue, exchange);
        assertNotNull(binding);
        assertEquals("season.event.season.updated", binding.getRoutingKey());
    }

    @Test
    void seasonStatusChangedBinding_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        TopicExchange exchange = config.seasonExchange();
        Binding binding = config.seasonStatusChangedBinding(queue, exchange);
        assertNotNull(binding);
        assertEquals("season.event.season.status.changed", binding.getRoutingKey());
    }

    @Test
    void seasonCompletedBinding_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        TopicExchange exchange = config.seasonExchange();
        Binding binding = config.seasonCompletedBinding(queue, exchange);
        assertNotNull(binding);
        assertEquals("season.event.season.completed", binding.getRoutingKey());
    }

    @Test
    void cropDeletedBinding_ShouldExist() {
        Queue queue = config.sustainabilityEventsQueue();
        TopicExchange exchange = config.cropExchange();
        Binding binding = config.cropDeletedBinding(queue, exchange);
        assertNotNull(binding);
        assertEquals("crop.event.crop.deleted", binding.getRoutingKey());
    }
}
