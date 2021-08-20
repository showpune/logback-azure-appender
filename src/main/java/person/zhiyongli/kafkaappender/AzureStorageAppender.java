package person.zhiyongli.kafkaappender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class RollingAzureBlobAppender extends AppenderBase<ILoggingEvent> {

    private CloudBlobContainer container;
    private String baseBlobName;
    private CloudAppendBlob blob;

    private Date date;

    private Encoder<ILoggingEvent> encoder;

    public CloudBlobContainer getContainer() {
        return container;
    }

    public void setContainer(CloudBlobContainer container) {
        this.container = container;
    }

    public String getBaseBlobName() {
        return baseBlobName;
    }

    public void setBaseBlobName(String baseBlobName) {
        this.baseBlobName = baseBlobName;
    }

    public CloudAppendBlob getBlob() {
        return blob;
    }

    public void setBlob(CloudAppendBlob blob) {
        this.blob = blob;
    }

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (differentDay(date, new Date())) {
            roll();
        }

        byte[] message;
        if(encoder == null) {
            message = event.getFormattedMessage().getBytes();
        }
        else {
            message = encoder.encode(event);
        }

        try {
            blob.appendFromByteArray(message, 0, message.length);
        } catch (StorageException ex) {
            throw new RuntimeException(ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void roll() {
        try {
            date = new Date();
            String stamp = new SimpleDateFormat("yyyy-MM-dd").format(date);
            String blobName = baseBlobName + "_" + stamp + ".log";
            blob = container.getAppendBlobReference(blobName);

            if (!blob.exists()) {
                blob.createOrReplace();

                HashMap<String, String> metadata = new HashMap<>();
                metadata.put("date", stamp);
                metadata.put("name", baseBlobName);

                blob.setMetadata(metadata);
                blob.uploadMetadata();
            }
        } catch (StorageException ex) {
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    private boolean differentDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR) ||
                cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR);
    }

}