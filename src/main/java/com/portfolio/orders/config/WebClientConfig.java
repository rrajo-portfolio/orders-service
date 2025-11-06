package com.portfolio.orders.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean(name = "catalogWebClient")
    public WebClient catalogWebClient(@Value("${services.catalog.base-url}") String baseUrl) {
        return baseClient(baseUrl).build();
    }

    @Bean(name = "usersWebClient")
    public WebClient usersWebClient(@Value("${services.users.base-url}") String baseUrl) {
        return baseClient(baseUrl).build();
    }

    private WebClient.Builder baseClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
            .responseTimeout(Duration.ofSeconds(5))
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter(logRequest());
    }

    private ExchangeFilterFunction logRequest() {
        Logger logger = LoggerFactory.getLogger(WebClientConfig.class);
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            return Mono.fromRunnable(() ->
                logger.debug("Calling external service {} {}", clientRequest.method(), clientRequest.url()))
                .thenReturn(clientRequest);
        });
    }
}
