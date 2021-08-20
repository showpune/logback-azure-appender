package person.zhiyongli.kafkaappender;


import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.status.ErrorStatus;
import com.azure.messaging.eventhubs.*;

import java.util.Arrays;
import java.util.List;


public class EventHubAppender<E> extends AppenderBase<E> {

    protected Layout<E> layout;
    private boolean logToLocal = false;
    private String connectionString;

    private String eventHubName;
    private EventHubProducerClient producer;


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
                    .connectionString(connectionString)
                    .buildProducerClient();
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
    protected void append(E event) {
        String msg = layout.doLayout(event);
        List<EventData> allEvents = Arrays.asList(new EventData(msg));
        // create a batch
//        EventDataBatch eventDataBatch = producer.createBatch();
//        if (!eventDataBatch.tryAdd(new EventData(msg))) {
//            throw new IllegalArgumentException("Event is too large for an empty batch. Max size: "
//                    + eventDataBatch.getMaxSizeInBytes());
//        }
//
//        // send the last batch of remaining events
//        if (eventDataBatch.getCount() > 0) {
//            producer.send(eventDataBatch);
//        }
        try {
            producer.send(allEvents);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public boolean getLogToLocal() {
        return logToLocal;
    }

    public void setLogToLocal(String logToLocal) {
        if (Boolean.valueOf(logToLocal)) {
            this.logToLocal = true;
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