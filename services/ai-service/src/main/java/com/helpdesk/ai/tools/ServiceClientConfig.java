package com.helpdesk.ai.tools;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * One RestClient per downstream service the agent tools call directly (not through
 * api-gateway - this is service-to-service traffic on the internal Docker/K8s network,
 * same as every other cross-service call in this platform).
 */
@Configuration
public class ServiceClientConfig {

    @Bean
    @Qualifier("ticketServiceClient")
    public RestClient ticketServiceClient(RestClient.Builder builder,
                                           @Value("${services.ticket-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean
    @Qualifier("kbServiceClient")
    public RestClient kbServiceClient(RestClient.Builder builder,
                                       @Value("${services.kb-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
