package org.example.adminreporting.config;

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
    public static final String INVENTORY_EXCHANGE = "inventory-exchange";
    public static final String MARKETPLACE_EXCHANGE = "marketplace-exchange";

    public static final String ADMIN_REPORTING_EVENTS_QUEUE = "admin-reporting-service.events";

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
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE);
    }

    @Bean
    public TopicExchange marketplaceExchange() {
        return new TopicExchange(MARKETPLACE_EXCHANGE);
    }

    @Bean
    public Queue adminReportingEventsQueue() {
        return new Queue(ADMIN_REPORTING_EVENTS_QUEUE, true);
    }

    // Bindings for Farm events
    @Bean
    public Binding farmCreatedBinding(Queue adminReportingEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(farmExchange).with("farm.event.farm.created");
    }

    @Bean
    public Binding farmUpdatedBinding(Queue adminReportingEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(farmExchange).with("farm.event.farm.updated");
    }

    @Bean
    public Binding farmDeletedBinding(Queue adminReportingEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(farmExchange).with("farm.event.farm.deleted");
    }

    // Bindings for Plot events
    @Bean
    public Binding plotCreatedBinding(Queue adminReportingEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(farmExchange).with("farm.event.plot.created");
    }

    @Bean
    public Binding plotUpdatedBinding(Queue adminReportingEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(farmExchange).with("farm.event.plot.updated");
    }

    @Bean
    public Binding plotDeletedBinding(Queue adminReportingEventsQueue, TopicExchange farmExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(farmExchange).with("farm.event.plot.deleted");
    }

    // Bindings for Season events
    @Bean
    public Binding seasonCreatedBinding(Queue adminReportingEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(seasonExchange).with("season.event.season.created");
    }

    @Bean
    public Binding seasonUpdatedBinding(Queue adminReportingEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(seasonExchange).with("season.event.season.updated");
    }

    @Bean
    public Binding seasonStatusChangedBinding(Queue adminReportingEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(seasonExchange).with("season.event.season.status.changed");
    }

    @Bean
    public Binding seasonCompletedBinding(Queue adminReportingEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(seasonExchange).with("season.event.season.completed");
    }

    @Bean
    public Binding harvestRecordedBinding(Queue adminReportingEventsQueue, TopicExchange seasonExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(seasonExchange).with("season.event.harvest.recorded");
    }

    // Bindings for Expense events
    @Bean
    public Binding expenseCreatedBinding(Queue adminReportingEventsQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(financeExchange).with("finance.event.expense.created");
    }

    @Bean
    public Binding expenseUpdatedBinding(Queue adminReportingEventsQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(financeExchange).with("finance.event.expense.updated");
    }

    @Bean
    public Binding expenseDeletedBinding(Queue adminReportingEventsQueue, TopicExchange financeExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(financeExchange).with("finance.event.expense.deleted");
    }

    // Bindings for Incident events
    @Bean
    public Binding incidentCreatedBinding(Queue adminReportingEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(incidentExchange).with("incident.event.incident.created");
    }

    @Bean
    public Binding incidentUpdatedBinding(Queue adminReportingEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(incidentExchange).with("incident.event.incident.updated");
    }

    @Bean
    public Binding incidentResolvedBinding(Queue adminReportingEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(incidentExchange).with("incident.event.incident.resolved");
    }

    @Bean
    public Binding incidentCancelledBinding(Queue adminReportingEventsQueue, TopicExchange incidentExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(incidentExchange).with("incident.event.incident.cancelled");
    }

    // Bindings for Inventory events
    @Bean
    public Binding inventoryLotReceivedBinding(Queue adminReportingEventsQueue, TopicExchange inventoryExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(inventoryExchange).with("inventory.event.product_warehouse_lot_received");
    }

    // Bindings for Marketplace events
    @Bean
    public Binding orderCreatedBinding(Queue adminReportingEventsQueue, TopicExchange marketplaceExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(marketplaceExchange).with("order.created");
    }

    @Bean
    public Binding paymentSubmittedBinding(Queue adminReportingEventsQueue, TopicExchange marketplaceExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(marketplaceExchange).with("payment.submitted");
    }

    @Bean
    public Binding paymentVerifiedBinding(Queue adminReportingEventsQueue, TopicExchange marketplaceExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(marketplaceExchange).with("payment.verified");
    }

    @Bean
    public Binding orderCompletedBinding(Queue adminReportingEventsQueue, TopicExchange marketplaceExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(marketplaceExchange).with("order.completed");
    }

    @Bean
    public Binding orderCancelledBinding(Queue adminReportingEventsQueue, TopicExchange marketplaceExchange) {
        return BindingBuilder.bind(adminReportingEventsQueue).to(marketplaceExchange).with("order.cancelled");
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
