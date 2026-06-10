package com.petfinder.chat.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConsulRegistration {

    private static final Logger log = LoggerFactory.getLogger(ConsulRegistration.class);

    @Value("${consul.host:consul}")
    private String consulHost;

    @Value("${consul.port:8500}")
    private int consulPort;

    @Value("${service.name:chat-service}")
    private String serviceName;

    @Value("${service.id:chat-service}")
    private String serviceId;

    @Value("${service.host:chat-service}")
    private String serviceHost;

    @Value("${service.port:8443}")
    private int servicePort;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @PostConstruct
    public void register() {
        String body = String.format(
                "{\"ID\":\"%s\",\"Name\":\"%s\",\"Address\":\"%s\",\"Port\":%d,"
                        + "\"Tags\":[\"spring-boot\",\"java\",\"https\",\"mtls\"],"
                        + "\"Check\":{\"TCP\":\"%s:%d\",\"Interval\":\"10s\",\"Timeout\":\"5s\","
                        + "\"DeregisterCriticalServiceAfter\":\"1m\"}}",
                serviceId, serviceName, serviceHost, servicePort, serviceHost, servicePort);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + consulHost + ":" + consulPort + "/v1/agent/service/register"))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                log.warn("Consul register returned status={}", resp.statusCode());
            } else {
                log.info("Registered {} with Consul as {}", serviceId, serviceName);
            }
        } catch (Exception e) {
            log.warn("Failed to register with Consul: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void deregister() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + consulHost + ":" + consulPort
                            + "/v1/agent/service/deregister/" + serviceId))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
            log.info("Deregistered {} from Consul", serviceId);
        } catch (Exception e) {
            log.warn("Failed to deregister from Consul: {}", e.getMessage());
        }
    }
}
