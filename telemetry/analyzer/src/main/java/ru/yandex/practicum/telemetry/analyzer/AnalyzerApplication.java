package ru.yandex.practicum.telemetry.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.yandex.practicum.telemetry.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.telemetry.analyzer.processor.SnapshotProcessor;

@Slf4j
@EnableConfigurationProperties
@SpringBootApplication
public class AnalyzerApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(AnalyzerApplication.class, args);

        final HubEventProcessor hubEventProcessor =
                context.getBean(HubEventProcessor.class);

        SnapshotProcessor snapshotProcessor =
                context.getBean(SnapshotProcessor.class);

        Thread hubEventProcessorThread = new Thread(hubEventProcessor);
        hubEventProcessorThread.setName("HubEventProcessorThread");
        hubEventProcessorThread.start();

        snapshotProcessor.run();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            hubEventProcessor.shutdown();
            snapshotProcessor.shutdown();

            try {
                hubEventProcessorThread.join(5000);
                Thread.currentThread().join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            context.close();
        }));
    }

}
