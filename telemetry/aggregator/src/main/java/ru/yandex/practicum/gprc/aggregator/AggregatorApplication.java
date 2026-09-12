package ru.yandex.practicum.gprc.aggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.yandex.practicum.gprc.aggregator.starter.AggregatorStarter;

@SpringBootApplication
public class AggregatorApplication {

    public static void main(String[] args) {
        var context = SpringApplication.run(AggregatorApplication.class, args);
        AggregatorStarter starter = context.getBean(AggregatorStarter.class);
        Runtime.getRuntime().addShutdownHook(new Thread(starter::shutdown));
        starter.start();
    }

}
