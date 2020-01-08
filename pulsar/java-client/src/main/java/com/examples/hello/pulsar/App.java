/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.examples.hello.pulsar;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.impl.schema.AvroSchema;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

public class App {
    public static void main(String[] args) throws PulsarClientException, InterruptedException {
      var client = createClient(getProp("PULSAR_HOST", "localhost"));
      var topic = getProp("PULSAR_TOPIC", "conditions");
      var isConsumer = Boolean.parseBoolean(getProp("PULSAR_PRODUCER", "false"));

      if (isConsumer)
        consume(client, topic);
      else
        produce(client, topic);
      client.close();
    }

    public static void consume(PulsarClient client, String topic) throws PulsarClientException {
      var subscriptionName = getProp("PULSAR_SUBSCRIPTION_NAME", "my-java-sub");
      var consumer = client.newConsumer(AvroSchema.of(Condition.class))
        .topic(topic)
        .subscriptionName(subscriptionName)
        .subscribe();

      while (true) {
        var msg = consumer.receive();
        try {
          var condition = msg.getValue();
          var ts = Instant.ofEpochMilli(msg.getPublishTime());
          var id = msg.getMessageId();
          var t = condition.getTemperature();
          var h = condition.getHumidity();
          System.out.printf("Received msg(time=%s id=%s), condition(T=%.2f H=%.2f)", ts, id, t, h);

          consumer.acknowledge(msg);
        } catch (Exception e) {
          consumer.negativeAcknowledge(msg);
        }
      }
    }

    public static void produce(PulsarClient client, String topic) throws PulsarClientException, InterruptedException {
      var producer = client.newProducer(AvroSchema.of(Condition.class))
        .topic(topic)
        .create();
      var condition = new Condition();
      var rnd = new Random();
      var sleepMs = Integer.parseInt(getProp("PULSAR_INTERVAL_MS", "0"));
      while (true) {
        condition.setTemperature(rnd.nextFloat() * 40);
        condition.setHumidity(rnd.nextFloat() * 100);
        producer.send(condition);
        if (sleepMs > 0)
          Thread.sleep(sleepMs);
      }
    }

    public static PulsarClient createClient(String pulsarHost) throws PulsarClientException {
      var serviceUrl = String.format("pulsar://%s:6650", pulsarHost);
      return PulsarClient.builder()
        .serviceUrl(serviceUrl)
        .build();
    }

    public static String getProp(String name, String defaultValue)
    {
      return Optional.ofNullable(System.getenv(name))
          .orElse(System.getProperty(name, defaultValue));
    }
}
