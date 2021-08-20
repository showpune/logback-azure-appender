package person.zhiyongli.kafkaappender;


import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.status.ErrorStatus;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerAsyncClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;


public class EventHubAsyncAppender<E> extends AppenderBase<E> {

    private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(30);

    protected Layout<E> layout;

    private String connectionString;

    private String eventHubName;

    private EventHubProducerAsyncClient producer;


    public void start() {
        if (this.layout == null) {
            this.addStatus(new ErrorStatus("No layout set for the appender named \"" + this.name + "\".", this));
            return;
        }
        System.out.println("Starting EventHubAppender...");
        if (eventHubName == null) {
            System.err.println("EventHubAppender requires a eventHubName. Add this to the appender configuration.");
            return;
        } else {
            System.out.println("EventHubAppender will publish messages to the '" + eventHubName + "' topic.");
        }
        if (connectionString == null) {
            System.err.println("EventHubAppender requires a connectionString. Add this to the appender configuration.");
            return;
        } else {
            System.out.println("EventHubAppender use connection '" + connectionString + "'.");
        }
        try {
            producer = new EventHubClientBuilder()
                    .connectionString(connectionString, eventHubName)
                    .buildAsyncProducerClient();
        } catch (Exception exception) {
            System.err.println("EventHubAppender: Exception initializing Producer. " + exception + " : " + exception.getMessage());
            return;
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        System.out.println("Stopping EventHubAppender...");
        producer.close();
    }

    @Override
    protected void append(E eventMessage) {
        String msg = layout.doLayout(eventMessage);
        Flux<EventData> telemetryEvents = Flux.just(new EventData(msg));
        // create a batch
        final AtomicReference<EventDataBatch> currentBatch = new AtomicReference<>(
                producer.createBatch().block());

        final Mono<Void> sendOperation = telemetryEvents.flatMap(event -> {
                    final EventDataBatch batch = currentBatch.get();
                    if (batch.tryAdd(event)) {
                        return Mono.empty();
                    }

                    // The batch is full, so we create a new batch and send the batch. Mono.when completes when both operations
                    // have completed.
                    return Mono.when(
                            producer.send(batch),
                            producer.createBatch().map(newBatch -> {
                                currentBatch.set(newBatch);

                                // Add that event that we couldn't before.
                                if (!newBatch.tryAdd(event)) {
                                    throw Exceptions.propagate(new IllegalArgumentException(String.format(
                                            "Event is too large for an empty batch. Max size: %s. Event: %s",
                                            newBatch.getMaxSizeInBytes(), event.getBodyAsString())));
                                }

                                return newBatch;
                            }));
                }).then()
                .doFinally(signal -> {
                    final EventDataBatch batch = currentBatch.getAndSet(null);
                    if (batch != null) {
                        producer.send(batch).block(OPERATION_TIMEOUT);
                    }
                });

        // The sendOperation creation and assignment is not a blocking call. It does not get invoked until there is a
        // subscriber to that operation. For the purpose of this example, we block so the program does not end before
        // the send operation is complete. Any of the `.subscribe` overloads also work to start the Mono asynchronously.
        try {
            sendOperation.block(OPERATION_TIMEOUT);
        } finally {
        }
    }

    public void setLayout(Layout<E> layout) {
        this.layout = layout;
    }


    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public void setEventHubName(String eventHubName) {
        this.eventHubName = eventHubName;
    }

}