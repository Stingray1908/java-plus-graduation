package ru.yandex.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableDiscoveryClient
//@EnableConfigurationProperties(KafkaProperties.class)
public class CollectorApp {
    public static void main(String[] args) {
        SpringApplication.run(CollectorApp.class, args);
    }
}

