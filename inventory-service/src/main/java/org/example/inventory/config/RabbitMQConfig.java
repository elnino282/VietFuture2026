package org.example.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "inventory-exchange";
    public static final String SEASON_EXCHANGE_NAME = "season-exchange";
    public static final String HARVEST_RECORDED_QUEUE = "inventory-harvest-recorded-queue";
    public static final String HARVEST_RECORDED_ROUTING_KEY = "season.event.harvest.recorded";

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue harvestRecordedQueue() {
        return new Queue(HARVEST_RECORDED_QUEUE, true);
    }

    @Bean
    public Binding harvestRecordedBinding(Queue harvestRecordedQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(harvestRecordedQueue).to(seasonExchange).with(HARVEST_RECORDED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange seasonExchange() {
        return new TopicExchange(SEASON_EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
