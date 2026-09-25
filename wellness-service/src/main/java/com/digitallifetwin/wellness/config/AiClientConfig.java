package com.digitallifetwin.wellness.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    @Bean
    RestClient wellnessAiRestClient(RestClient.Builder builder, AiProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(properties.resolvedConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(properties.resolvedReadTimeoutMs()));
        return builder.baseUrl(properties.resolvedServiceUrl()).requestFactory(factory).build();
    }
}
