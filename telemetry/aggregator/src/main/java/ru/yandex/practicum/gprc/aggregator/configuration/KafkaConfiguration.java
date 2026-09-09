package ru.yandex.practicum.gprc.aggregator.configuration;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.parcticum.telemetry.serialization.SensorsSnapshotSerializer;
import ru.yandex.parcticum.telemetry.serialization.SensorEventDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConfiguration {

    @Bean
    public KafkaConsumer<String, SpecificRecordBase> kafkaConsumer(KafkaConfigurationProperties config) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getConsumer().getGroupId());
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, config.getConsumer().getClientId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorEventDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, config.getConsumer().getEnableAutoCommit());

        return new KafkaConsumer<>(props);
    }

    @Bean
    public KafkaProducer<String, SpecificRecordBase> kafkaProducer(KafkaConfigurationProperties config) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SensorsSnapshotSerializer.class.getName());

        return new KafkaProducer<>(props);
    }
}
