package com.univerliga.identityprovisioning.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange crmExchange(AppProperties props) {
        return new DirectExchange(props.getProvisioning().getBroker().getExchange(), true, false);
    }

    @Bean
    Queue inboxQueue(AppProperties props) {
        return QueueBuilder.durable(props.getProvisioning().getBroker().getQueue())
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", props.getProvisioning().getBroker().getDlq())
            .build();
    }

    @Bean
    Queue dlqQueue(AppProperties props) {
        return QueueBuilder.durable(props.getProvisioning().getBroker().getDlq()).build();
    }

    @Bean
    Binding createdBinding(AppProperties props, DirectExchange crmExchange, Queue inboxQueue) {
        return BindingBuilder.bind(inboxQueue).to(crmExchange).with(props.getProvisioning().getBroker().getRoutingCreated());
    }

    @Bean
    Binding updatedBinding(AppProperties props, DirectExchange crmExchange, Queue inboxQueue) {
        return BindingBuilder.bind(inboxQueue).to(crmExchange).with(props.getProvisioning().getBroker().getRoutingUpdated());
    }

    @Bean
    Binding deactivatedBinding(AppProperties props, DirectExchange crmExchange, Queue inboxQueue) {
        return BindingBuilder.bind(inboxQueue).to(crmExchange).with(props.getProvisioning().getBroker().getRoutingDeactivated());
    }
}
