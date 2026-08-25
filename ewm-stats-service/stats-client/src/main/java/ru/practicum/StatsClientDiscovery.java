package ru.practicum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHit;
import ru.practicum.dto.ViewStats;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClientDiscovery implements StatsClient {

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final String serviceId = "stats-service";
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

    @Override
    public void hit(EndpointHit hit) {
        URI uri = makeStatsUri("/hit");
        restTemplate.postForEntity(uri, hit, Void.class);
    }

    @Override
    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("http://placeholder") // не используется
                .path("/stats")
                .queryParam("start", formatter.format(start))
                .queryParam("end", formatter.format(end))
                .queryParam("unique", unique != null && unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        String rawQuery = builder.build().encode().toString().substring(1); // убрать ведущий "/"
        URI baseUri = makeStatsUri("");
        URI finalUri = URI.create(baseUri.toString() + "/" + rawQuery.replaceFirst("/", ""));

        ResponseEntity<ViewStats[]> response = restTemplate.getForEntity(finalUri, ViewStats[].class);
        return List.of(response.getBody());
    }
}
