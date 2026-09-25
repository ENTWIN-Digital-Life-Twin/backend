package com.digitallifetwin.ai.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClientConfig {

    @Bean
    RestClient planningRestClient(RestClient.Builder builder, DownstreamProperties properties) {
        return builder.baseUrl(properties.resolvedPlanningServiceUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    @Bean
    RestClient wellnessRestClient(RestClient.Builder builder, DownstreamProperties properties) {
        return builder.baseUrl(properties.resolvedWellnessServiceUrl())
                .requestFactory(requestFactory(properties))
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory(DownstreamProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.resolvedConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.resolvedReadTimeoutMs()));
        return factory;
    }
}
