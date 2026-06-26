package org.example.sustainability.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String FARM_EXCHANGE = "farm-exchange";
    public static final String SEASON_EXCHANGE = "season-exchange";
    public static final String FINANCE_EXCHANGE = "finance-exchange";
    public static final String INCIDENT_EXCHANGE = "incident-exchange";
    public static final String CROP_EXCHANGE = "crop-exchange";
    public static final String SUSTAINABILITY_EVENTS_QUEUE = "sustainability-service.events";

    @Bean
    public TopicExchange farmExchange() {
        return new TopicExchange(FARM_EXCHANGE);
    }

    @Bean
    public TopicExchange seasonExchange() {
        return new TopicExchange(SEASON_EXCHANGE);
    }

    @Bean
    public TopicExchange financeExchange() {
        return new TopicExchange(FINANCE_EXCHANGE);
    }

    @Bean
    public TopicExchange incidentExchange() {
        return new TopicExchange(INCIDENT_EXCHANGE);
    }

    @Bean
    public TopicExchange cropExchange() {
        return new TopicExchange(CROP_EXCHANGE);
    }

    @Bean
    public Queue sustainabilityEventsQueue() {
        return new Queue(SUSTAINABILITY_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding farmCreatedBinding(Queue sustainabilityEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(farmExchange).with("farm.event.farm.created");
    }

    @Bean
    public Binding farmUpdatedBinding(Queue sustainabilityEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(farmExchange).with("farm.event.farm.updated");
    }

    @Bean
    public Binding farmDeletedBinding(Queue sustainabilityEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(farmExchange).with("farm.event.farm.deleted");
    }

    @Bean
    public Binding plotCreatedBinding(Queue sustainabilityEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(farmExchange).with("farm.event.plot.created");
    }

    @Bean
    public Binding plotUpdatedBinding(Queue sustainabilityEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(farmExchange).with("farm.event.plot.updated");
    }

    @Bean
    public Binding plotDeletedBinding(Queue sustainabilityEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(farmExchange).with("farm.event.plot.deleted");
    }

    @Bean
    public Binding seasonCreatedBinding(Queue sustainabilityEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(seasonExchange).with("season.event.season.created");
    }

    @Bean
    public Binding seasonUpdatedBinding(Queue sustainabilityEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(seasonExchange).with("season.event.season.updated");
    }

    @Bean
    public Binding seasonStatusChangedBinding(Queue sustainabilityEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(seasonExchange).with("season.event.season.status.changed");
    }

    @Bean
    public Binding seasonCompletedBinding(Queue sustainabilityEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(seasonExchange).with("season.event.season.completed");
    }

    @Bean
    public Binding harvestRecordedBinding(Queue sustainabilityEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(seasonExchange).with("season.event.harvest.recorded");
    }

    @Bean
    public Binding expenseCreatedBinding(Queue sustainabilityEventsQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(financeExchange).with("finance.event.expense.created");
    }

    @Bean
    public Binding expenseUpdatedBinding(Queue sustainabilityEventsQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(financeExchange).with("finance.event.expense.updated");
    }

    @Bean
    public Binding expenseDeletedBinding(Queue sustainabilityEventsQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(financeExchange).with("finance.event.expense.deleted");
    }

    @Bean
    public Binding incidentCreatedBinding(Queue sustainabilityEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(incidentExchange).with("incident.event.incident.created");
    }

    @Bean
    public Binding incidentUpdatedBinding(Queue sustainabilityEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(incidentExchange).with("incident.event.incident.updated");
    }

    @Bean
    public Binding incidentResolvedBinding(Queue sustainabilityEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(incidentExchange).with("incident.event.incident.resolved");
    }

    @Bean
    public Binding incidentCancelledBinding(Queue sustainabilityEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(incidentExchange).with("incident.event.incident.cancelled");
    }

    @Bean
    public Binding cropCreatedBinding(Queue sustainabilityEventsQueue, TopicExchange cropExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(cropExchange).with("crop.event.crop.created");
    }

    @Bean
    public Binding cropUpdatedBinding(Queue sustainabilityEventsQueue, TopicExchange cropExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(cropExchange).with("crop.event.crop.updated");
    }

    @Bean
    public Binding cropDeletedBinding(Queue sustainabilityEventsQueue, TopicExchange cropExchange) {
        return BindingBuilder.bind(sustainabilityEventsQueue).to(cropExchange).with("crop.event.crop.deleted");
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
