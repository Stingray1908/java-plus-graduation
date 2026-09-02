package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.practicum.dto.EndpointHit;
import ru.yandex.practicum.dto.ViewStats;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service("StatsClientDiscovery")
public class StatsClientDiscovery implements StatsClient {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final String serviceId = "stats-server";
    @Autowired
    private RetryTemplate retryTemplate;


    public StatsClientDiscovery(
            RestTemplate restTemplate,
            DiscoveryClient discoveryClient
    ) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    private ServiceInstance getStatsInstance() {
        return retryTemplate.execute(ctx -> {
            var instances = discoveryClient.getInstances(serviceId);
            if (instances.isEmpty()) {
                throw new IllegalStateException("Нет экземпляров stats-service");
            }
            return instances.get(0);
        });
    }

    private URI makeStatsUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getStatsInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    private URI getBaseUri() {
        ServiceInstance instance = retryTemplate.execute(ctx -> getStatsInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort());
    }

    @Override
    public void hit(EndpointHit hit) {
        URI uri = makeStatsUri("/hit");
        restTemplate.postForEntity(uri, hit, Void.class);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        URI baseUri = getBaseUri();
        URI statsUri = baseUri.resolve("/stats");

        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(statsUri)
                .queryParam("start", formatter.format(start))
                .queryParam("end", formatter.format(end))
                .queryParam("unique", unique != null && unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        URI finalUri = builder.build().encode().toUri();

        log.debug("Запрос статистики: {}", finalUri);
        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(finalUri, ViewStats[].class);

        ViewStats[] body = response.getBody();
        return body == null ? List.of() : List.of(body);
    }
}
